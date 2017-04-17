/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wso2.code.quality.metrics;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.wso2.code.quality.metrics.model.CommitHistoryApiResponse;
import com.wso2.code.quality.metrics.model.GraphQlResponse;
import com.wso2.code.quality.metrics.model.Graphql;
import com.wso2.code.quality.metrics.model.SearchApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * This class is used for getting the blame information on relevant changed line ranges on relevant files from the
 * given patch.
 *
 * @since 1.0.0
 */

public class ChangesFinder {
    private static final Logger logger = Logger.getLogger(ChangesFinder.class);

    private List<String> fileNames = new ArrayList<>();
    private List<String> patchString = new ArrayList<>();
    private Map<String, Set<String>> parentCommitHashes;
    private Set<String> authorNames = new HashSet<>();    //authors of the bug lines fixed from the patch
    private Set<String> authorCommits = new HashSet<>();  //  author commits of the bug lines fixed from the patch
    private final JSONObject jsonStructure = new JSONObject();
    private final GithubApiCaller githubApiCaller = new GithubApiCaller();
    private final Gson gson = new Gson();
    public final List<List<String>> changedLineRanges = new ArrayList<>();  // for saving the line no that are changed

    /**
     * This class is used to prevent SIC_INNER_SHOULD_BE_STATIC_ANON error that comes when building with WSO2 parent
     * pom. As suggested by the above error an static inner class is used
     */
    private static class ListType extends TypeToken<List<CommitHistoryApiResponse>> {
    }

    /**
     * This is used for obtaining the repositories that contain the relevant commits belongs to the given patch
     *
     * @param gitHubToken  github token for accessing github API
     * @param commitHashes List of commits that belongs to the given patch
     * @return author commits of the bug lines which are fixed from the given patch
     */
    protected Set<String> obtainRepoNamesForCommitHashes(String gitHubToken, List<String> commitHashes) {
        commitHashes.forEach(commitHash -> {
            try {
                String jsonText = githubApiCaller.callSearchCommitApi(commitHash, gitHubToken);
                List<String> repoLocations = saveRepoNames(jsonText);
                identifyChangedFile(repoLocations, commitHash, gitHubToken);
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });
        return authorCommits;
    }

    /**
     * This save the  relevant repository names that contain the given commit hash in a List.
     *
     * @param jsonText String representation of the json response
     * @return A list of repository locations having the given commit hash
     * @throws CodeQualityMetricsException
     */
    protected List<String> saveRepoNames(String jsonText) throws CodeQualityMetricsException {
        List<String> repoLocation = new ArrayList<>();
        SearchApiResponse searchCommitPojo;
        try {
            searchCommitPojo = gson.fromJson(jsonText, SearchApiResponse.class);
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMetricsException(e.getMessage(), e.getCause());
        }
        searchCommitPojo.getContainingRepositories()
                .forEach(recordItem -> repoLocation.add(recordItem.getRepository().getRepositoryLocation()));
        logger.debug("Repositories having the given commit are successfully saved in an List");
        return repoLocation;
    }

    /**
     * This identifies the file changed and their relevant line ranges changed in selected repository from the given
     * commit hash
     *
     * @param repoLocation List of repository locations having the given commit hash
     * @param commitHash   commit hash to be searched for containing WSO2 repositories
     * @param gitHubToken  Github access token for accessing github API
     */
    private void identifyChangedFile(List<String> repoLocation, String commitHash, String gitHubToken) {
        SdkGitHubClient sdkGitHubClient = new SdkGitHubClient(gitHubToken);
        repoLocation.stream()
                .filter(repositoryName -> StringUtils.contains(repositoryName, "wso2/"))
                .forEach(repositoryName -> {
                    //authorNames.clear();
                    Map<String, String> fileNamesWithPatchString = new HashMap<>();
                    try {
                        fileNamesWithPatchString = sdkGitHubClient.getFilesChanged(repositoryName, commitHash);
                    } catch (CodeQualityMetricsException e) {
                        logger.error(e.getMessage(), e.getCause());
                    }

                    Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers = new HashMap<>();
                    Map<String, String> fileNamesWithPreviousCommitHash = new HashMap<>();
                    if (fileNamesWithPatchString != null) {
                        // looping from one file to file and saving deleted lines against the file name in another map
                        for (Map.Entry<String, String> entry : fileNamesWithPatchString.entrySet()) {
                            String fileName = entry.getKey();
                            String patchString = entry.getValue();
                            Set<Integer> deletedLines = identifyDeletedLines(patchString);
                            String previousCommitHash;
                            //for omitting files without having deleted lines
                            if (deletedLines.size() > 0) {
                                fileNamesWithDeletedLineNumbers.put(fileName, deletedLines);
                                previousCommitHash = findPreviousCommitOfFile(repositoryName, fileName, commitHash,
                                        gitHubToken);
                                fileNamesWithPreviousCommitHash.put(fileName, previousCommitHash);
                            }
                        }
                    }
//                    saveRelaventEditLineNumbers(fileNames, patchString);
                    getBlameDetails(repositoryName, fileNamesWithPreviousCommitHash,
                            fileNamesWithDeletedLineNumbers, gitHubToken);

//                    findFileChanges(repositoryName, commitHash, gitHubToken);

                });
        if (logger.isDebugEnabled()) {
            logger.debug("\n Author names :" + authorNames);
            logger.debug("\n Author commits :" + authorCommits);
        }
    }

    /**
     * This is used to get the previous commit hash of the selected file before the current selected commit which
     * contained in the given patch
     *
     * @param repositoryName   current selected Repository
     * @param filePath         current selected file name
     * @param latestCommitHash current selected recent commit contained in the given patch
     * @param githubToken      github access token for accessing github REST API
     * @return commits hash prior to the current selected commit hash contained in the given patch of the current
     * selected file
     */
    public String findPreviousCommitOfFile(String repositoryName, String filePath, String latestCommitHash, String githubToken) {
        String previousCommit = null;
        Map<String, String> commitWithDate = new HashMap<>();
        try {
            String jsonText = githubApiCaller.callCommitHistoryApi(repositoryName, filePath, githubToken);
            if (jsonText != null) {
                Type listType = new ListType().getType();
                List<CommitHistoryApiResponse> commitHistoryApiResponses = gson.fromJson(jsonText, listType);
                commitHistoryApiResponses.forEach(commitHistoryApiResponse -> {
                    String commitHash = commitHistoryApiResponse.getCommitHash();
                    String date = commitHistoryApiResponse.getCommit().getAuthor().getDate();
                    commitWithDate.put(commitHash, date);
                });
                String latestCommitDate = commitWithDate.get(latestCommitHash);
                String previousCommitDate = getPreviousCommitDate(commitWithDate, latestCommitDate);
                // looping for finding the commit Hash introduced in previous commit date
                for (Map.Entry entry : commitWithDate.entrySet()) {
                    if (entry.getValue().equals(previousCommitDate)) {
                        previousCommit = (String) entry.getKey();
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("There are no commit history records for the file " + filePath + " on repository " +
                            "" + repositoryName);
                }
            }
        } catch (CodeQualityMetricsException e) {
            logger.error(e.getMessage(), e.getCause());
        }
        return previousCommit;
    }

    /**
     * This is used to get the previous commit date of the selected file before the current commit
     *
     * @param commitWithDate   map contating all the commits with their respective date of the current selected
     *                         file
     * @param latestCommitDate latest commit date of the current selected file
     * @return previous commit date of the current selected file
     */
    public String getPreviousCommitDate(Map<String, String> commitWithDate, String latestCommitDate) {
        //creating a tempory list for sorting the date in ascending order
        List<String> sortedCommitDates = new ArrayList<>(commitWithDate.values());
        Collections.sort(sortedCommitDates);
        int indexOfLatestcommit = sortedCommitDates.indexOf(latestCommitDate);
        String previousCommitDateOfFile = sortedCommitDates.get(--indexOfLatestcommit);
        return previousCommitDateOfFile;
    }

    /**
     * This method is used to obtain the blame details of files for their previous commits
     *
     * @param repoLocation                    current selected Repository
     * @param fileNamesWithPreviousCommitHash map containing changed files with their prior commit hashes
     * @param fileNamesWithDeletedLineNumbers map containing changed files with their deleted line numbers
     * @param githubToken                     github access token for accessing github REST API
     */
    public void getBlameDetails(String repoLocation, Map<String, String> fileNamesWithPreviousCommitHash,
                                Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers, String githubToken) {

        // filtering the owner and the repository name from the repoLocation
        String owner = StringUtils.substringBefore(repoLocation, "/");
        String repositoryName = StringUtils.substringAfter(repoLocation, "/");
        Graphql graphqlBean = new Graphql();
//        for (Map.Entry entry : fileNamesWithPreviousCommitHash.entrySet()) {
//            String fileName =(String) entry.getKey();
//            String commitHash= (String)entry.getValue();
//            graphqlBean.setGraphqlInputWithoutHistory(owner, repositoryName);
//
//        }
        fileNamesWithPreviousCommitHash.forEach((fileName, previousCommitHash) -> {
            graphqlBean.setGraphqlInputWithoutHistory(owner, repositoryName, previousCommitHash, fileName);
            jsonStructure.put("query", graphqlBean.getGraphqlInputWithoutHistory());
            String jsonText = null;
            try {
                // calling the graphql API for getting blame information for the current file.
                jsonText = githubApiCaller.callGraphqlApi(jsonStructure, githubToken);
                findAuthorCommits(jsonText, fileNamesWithDeletedLineNumbers, fileName);
                System.out.println();
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });


    }

    /**
     * This is used to save the authors and relevant author commits of the buggy lines of code which are been fixed by
     * the given patch, to relevant lists.
     *
     * @param jsonText                        String representation of the json response received from github graphql API
     * @param fileNamesWithDeletedLineNumbers map containing changed files with their deleted line numbers
     * @param selectedFileName                selected file for finding the authors and author commits of its buggy lines
     */
    public void findAuthorCommits(String jsonText, Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers, String
            selectedFileName) {

        GraphQlResponse graphQlResponse = gson.fromJson(jsonText, GraphQlResponse.class);
        Set<Integer> deletedLines = fileNamesWithDeletedLineNumbers.get(selectedFileName);
        deletedLines.forEach(deletedLineNumber ->
                graphQlResponse.getResponse().getRepository().getFile().getBlame().getRanges()
                        .stream()
                        .filter(graphqlRange -> (graphqlRange.getStartingLine() <= deletedLineNumber &&
                                graphqlRange.getEndingLine() >= deletedLineNumber))
                        .forEach(graphqlRange -> {
                            String authorName = graphqlRange.getCommit().getAuthor().getName();
                            String authorcommit = StringUtils.substringAfter(graphqlRange.getCommit().getUrl(),
                                    "commit/");
                            authorNames.add(authorName); // authors are added to the Set
                            authorCommits.add(authorcommit); // author commits are added to the set
                        }));
    }

    /**
     * This method is used to save the line ranges being modified in a given file to a list and add that list to the
     * root list of.
     *
     * @param fileNames   Arraylist of files names that are being affected by the relevant commit
     * @param patchString Array list having the patch string value for each of the file being changed
     */

    protected void saveRelaventEditLineNumbers(List<String> fileNames, List<String> patchString) {
        /*filtering only the line ranges that are modified and saving to a string array cannot use parallel streams
          here as the order of the line changes must be preserved
         */
        patchString.stream()
                .map(patch -> StringUtils.substringsBetween(patch, "@@ ", " @@"))
                .forEach(lineChanges -> {
                    /*filtering the lines ranges that existed in the previous file, that exists in the new file and
                    saving them in to a list
                     */
                    IntStream.range(0, lineChanges.length)
                            .forEach(index -> {
                                //@@ -22,7 +22,7 @@ => -22,7 +22,7 => 22,28/22,28
                                String tempString = lineChanges[index];
                                // for taking the authors and commit hashes of the previous lines
                                String lineRangeInTheOldFileBeingModified = StringUtils.substringBetween(tempString,
                                        "-", " +");
                                // for taking the parent commit
                                String lineRangeInTheNewFileResultedFromModification = StringUtils.substringAfter
                                        (tempString, "+");
                                int intialLineNoInOldFile = Integer.parseInt(StringUtils.substringBefore
                                        (lineRangeInTheOldFileBeingModified, ","));
                                int tempEndLineNoInOldFile = Integer.parseInt(StringUtils.substringAfter
                                        (lineRangeInTheOldFileBeingModified, ","));
                                int endLineNoOfOldFile;
                                if (intialLineNoInOldFile != 0) {
                                    // to filterout the newly created files
                                    endLineNoOfOldFile = intialLineNoInOldFile + (tempEndLineNoInOldFile - 1);
                                } else {
                                    endLineNoOfOldFile = tempEndLineNoInOldFile;
                                }
                                int intialLineNoInNewFile = Integer.parseInt(StringUtils.substringBefore
                                        (lineRangeInTheNewFileResultedFromModification, ","));
                                int tempEndLineNoInNewFile = Integer.parseInt(StringUtils.substringAfter
                                        (lineRangeInTheNewFileResultedFromModification, ","));
                                int endLineNoOfNewFile = intialLineNoInNewFile + (tempEndLineNoInNewFile - 1);
                                // storing the line ranges that are being modified in the same array by replacing values
                                lineChanges[index] = intialLineNoInOldFile + "," + endLineNoOfOldFile + "/" +
                                        intialLineNoInNewFile + "," + endLineNoOfNewFile;
                            });
                    List<String> changedRange = new ArrayList<>(Arrays.asList(lineChanges));
                    //adding to the array list which keep track of the line ranges being changed
                    changedLineRanges.add(changedRange);
                });
        logger.debug("done saving file names and their relevant modification line ranges");
        if (logger.isDebugEnabled()) {
            logger.debug("\n File names : " + fileNames);
            logger.debug("\n Changed ranges :" + changedLineRanges + "\n");
        }
    }

    /**
     * This will iterate over the saved filenames and their relevant changed line ranges and calls the github graphQL
     * API for getting blame details for each of the files.
     *
     * @param repoLocation current selected Repository
     * @param commitHash   current selected Repository
     * @param gitHubToken  github token for accessing github GraphQL API
     */
    private void findFileChanges(String repoLocation, String commitHash, String gitHubToken) {
        // filtering the owner and the Repository name from the repoLocation
        String owner = StringUtils.substringBefore(repoLocation, "/");
        String repositoryName = StringUtils.substringAfter(repoLocation, "/");
        /*  iterating over the fileNames arraylist for the given commit cannot use parallel streams here as the order
         of the file names is important in the process
          */
        fileNames.forEach(fileName -> {
            int index = fileNames.indexOf(fileName);
            // the relevant arraylist of changed lines for that file
            List<String> lineRangesForSelectedFile = changedLineRanges.get(index);
            // for storing the parent commit hashes for all the changed line ranges of the relevant file
            parentCommitHashes = new HashMap<>();
            Graphql graphqlBean = new Graphql();
            graphqlBean.setGraphqlInputWithHistory(owner, repositoryName, commitHash, fileName);
            jsonStructure.put("query", graphqlBean.getGraphqlInputWithHistory());
            String jsonText = null;
            try {
                // calling the graphql API for getting blame information for the current file.
                jsonText = githubApiCaller.callGraphqlApi(jsonStructure, gitHubToken);
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
            //reading the above saved output for the current selected file name
            try {
                readBlameForSelectedFile(jsonText, lineRangesForSelectedFile);
                if (logger.isDebugEnabled()) {
                    logger.debug("Parent commits are saved for the " + fileName + " for all the modified line ranges");
                }
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
            findAuthorCommitsDel(owner, repositoryName, fileName, lineRangesForSelectedFile, gitHubToken);
            logger.debug("Authors and author commits of bug lines of code which are being fixed from the given " +
                    "patch are saved successfully to authorNames and authorCommits Sets");
        });
    }

    /**
     * This reads the blame received for the current selected file and insert parent commits of the changed lines
     * to a list.
     *
     * @param jsonText                    JSON response of blame of the selected file
     * @param changedRangesOfSelectedFile arraylist containing the changed line ranges of the current selected file
     */
    private void readBlameForSelectedFile(String jsonText, List<String> changedRangesOfSelectedFile)
            throws CodeQualityMetricsException {
        GraphQlResponse graphQlResponse;
        try {
            graphQlResponse = gson.fromJson(jsonText, GraphQlResponse.class);
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMetricsException(e.getMessage(), e.getCause());
        }
        GraphQlResponse finalGraphQlResponse = graphQlResponse;  // to make a effective final variable
        changedRangesOfSelectedFile.forEach(lineRange -> {
            int startingLineNo;
            int endLineNo;
            String oldFileRange = StringUtils.substringBefore(lineRange, "/");
            String newFileRange = StringUtils.substringAfter(lineRange, "/");
            // need to skip the newly created files from taking the blame as they contain no previous commits
            if (!oldFileRange.equals("0,0")) {
                /* need to consider the line range in the new file resulted from applying the commit, for finding
                parent commits
                 */
                startingLineNo = Integer.parseInt(StringUtils.substringBefore(newFileRange, ","));
                endLineNo = Integer.parseInt(StringUtils.substringAfter(newFileRange, ","));
                //for storing age with the relevant parent commit hash
                Map<Integer, Set<String>> ageWithParentCommit = new HashMap<>();
                while (endLineNo >= startingLineNo) {
                    int finalStartingLineNo = startingLineNo;       // to make a effective final variable
                    finalGraphQlResponse.getResponse().getRepository().getFile().getBlame().getRanges().stream()
                            .filter(graphqlRange -> (graphqlRange.getStartingLine() <= finalStartingLineNo &&
                                    graphqlRange.getEndingLine() >= finalStartingLineNo))
                            .forEach(graphqlRange -> {
                                int age = graphqlRange.getAge();
                                String url = graphqlRange.getCommit().getHistory().getCommits().get(1).getDetails()
                                        .getUrl();
                                /* get(1) is used directly as there are only 2 elements in the List<Edge> and last
                                resembles the parent commit
                                  */
                                String parentCommit = StringUtils.substringAfter(url, "commit/");
                                ageWithParentCommit.putIfAbsent(age, new HashSet<>());
                                if (!ageWithParentCommit.get(age).contains(parentCommit)) {
                                    ageWithParentCommit.get(age).add(parentCommit);
                                }
                            });
                    startingLineNo++;   // to check for other line numbers
                }
                TreeMap<Integer, Set<String>> sortedAgeWithParentCommit = new TreeMap<>(ageWithParentCommit);
                Set<String> parentCommit = sortedAgeWithParentCommit.get(sortedAgeWithParentCommit.firstKey());
                parentCommitHashes.put(oldFileRange, parentCommit);
            }
        });
    }

    /**
     * This is used to find the author and author commits of the buggy lines of code which are been fixed by the
     * given patch.
     *
     * @param owner                     owner of the selected epository
     * @param repositoryName            selected repository name
     * @param fileName                  name of the file which is required to get blame details
     * @param lineRangesForSelectedFile arraylist containing the changed line ranges of the current selected file
     * @param gitHubToken               github token for accessing github GraphQL API
     */
    private void findAuthorCommitsDel(String owner, String repositoryName, String fileName,
                                      List<String> lineRangesForSelectedFile, String gitHubToken) {
        for (Map.Entry<String, Set<String>> entry : parentCommitHashes.entrySet()) {
            String oldRange = entry.getKey();
            Set<String> commitHashes = entry.getValue();
            Graphql graphqlBean = new Graphql();
            commitHashes.parallelStream()
                    .forEach(commitHash -> {
                        graphqlBean.setGraphqlInputWithoutHistory(owner, repositoryName, commitHash, fileName);
                        jsonStructure.put("query", graphqlBean.getGraphqlInputWithoutHistory());
                        String jsonText = null;
                        try {
                            jsonText = githubApiCaller.callGraphqlApi(jsonStructure, gitHubToken);
                        } catch (CodeQualityMetricsException e) {
                            logger.error(e.getMessage(), e.getCause());
                        }
//                        saveAuthorCommits(jsonText, oldRange, lineRangesForSelectedFile);
                    });
        }
        if (logger.isDebugEnabled()) {
            logger.debug("author commits and authors of bug lines of code on " + fileName + " file which are been " +
                    "fixed by the given patch are successfully saved to lists");
        }
    }

//    /**
//     * This is used to save the authors and author commits of the buggy lines of code which are been fixed by the
//     * given patch, to relevant lists.
//     *
//     * @param jsonText                  String representation of the json response
//     * @param oldRange                  Range to select the correct range for collecting author commits
//     * @param lineRangesForSelectedFile arraylist containing the changed line ranges of the current selected file
//     */
//    private void saveAuthorCommits(String jsonText, String oldRange, List<String> lineRangesForSelectedFile) {
//        GraphQlResponse graphQlResponse = gson.fromJson(jsonText, GraphQlResponse.class);
//        lineRangesForSelectedFile.forEach(lineRange -> {
//            int startingLineNo;
//            int endLineNo;
//            String oldFileRange = StringUtils.substringBefore(lineRange, "/");
//            // need to skip the newly created files from taking the blame as they contain no previous commits
//            if (!oldFileRange.equals("0,0") && oldRange.equals(oldFileRange)) {
//                // need to consider the correct line range in the old file for finding authors and author commits
//                startingLineNo = Integer.parseInt(StringUtils.substringBefore(oldFileRange, ","));
//                endLineNo = Integer.parseInt(StringUtils.substringAfter(oldFileRange, ","));
//                while (endLineNo >= startingLineNo) {
//                    int finalStartingLineNo = startingLineNo;       // to make a effective final variable
//
//                    graphQlResponse.getResponse().getRepository().getFile().getBlame().getRanges().stream()
//
//                            .filter(graphqlRange -> (graphqlRange.getStartingLine() <= finalStartingLineNo &&
//                                    graphqlRange.getEndingLine() >= finalStartingLineNo))
//                            .forEach(graphqlRange -> {
//                                String authorName = graphqlRange.getCommit().getAuthor().getName();
//                                String authorcommit = StringUtils.substringAfter(graphqlRange.getCommit().getUrl(),
//                                        "commit/");
//                                authorNames.add(authorName); // authors are added to the Set
//                                authorCommits.add(authorcommit); // author commits are added to the set
//                            });
//                    startingLineNo++;   // to check for other line numbers
//                }
//            }
//        });
//    }

    /**
     * This method is used to identify the deleted lines from the current selected commit in given patch
     *
     * @param patchString patch string of the selected file received from github SDK
     * @return a Set of deleted lines in the above mentioned file
     */
    private Set<Integer> identifyDeletedLines(String patchString) {
        Set<Integer> linesDeletedInSelectedFile = new HashSet<>();
        Pattern pattern = Pattern.compile("@@ -");
        String patches[] = pattern.split(patchString);
        for (String patchRange : patches) {
            Scanner scanner = new Scanner(patchRange);
            int lineNumber = 0;
            //for finding the starting line number for the modified range.
            if (scanner.hasNext()) {
                String patchLine = scanner.nextLine();
                String words[] = patchLine.split(",");
                lineNumber = Integer.parseInt(words[0]);
            }
            //for finding the deleted lines in the string
            while (scanner.hasNext()) {
                String patchLine = scanner.nextLine();
                String words[];
                if (patchLine.trim().length() != 0) {
                    words = patchLine.split("\\s");
                } else {
                    words = patchLine.trim().split("\\s");
                }
                String sign = words[0];
                if ("-".equals(sign) || "".equals(sign)) {
                    if ("-".equals(sign)) {
                        linesDeletedInSelectedFile.add(lineNumber);
                    }
                    lineNumber++;
                }
            }
        }
        return linesDeletedInSelectedFile;
    }
}
