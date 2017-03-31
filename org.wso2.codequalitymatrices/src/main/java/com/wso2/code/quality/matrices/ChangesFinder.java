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

package com.wso2.code.quality.matrices;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wso2.code.quality.matrices.model.GraphQlResponse;
import com.wso2.code.quality.matrices.model.Graphql;
import com.wso2.code.quality.matrices.model.SearchApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    private final List<List<String>> changedLineRanges = new ArrayList<>();  // for saving the line no that are changed
    private final JSONObject jsonStructure = new JSONObject();
    private Map<String, Set<String>> parentCommitHashes;
    private Set<String> authorNames = new HashSet<>();    //authors of the bug lines fixed from the patch
    private Set<String> authorCommits = new HashSet<>();  //  author commits of the bug lines fixed from the patch

    private final GithubApiCaller githubApiCaller = new GithubApiCaller();
    private final Gson gson = new Gson();

    /**
     * This is used for obtaining the repositories that contain the relevant commits belongs to the given patch
     *
     * @param gitHubToken  github token for accessing github API
     * @param commitHashes List of commits that belongs to the given patch
     * @return author commits of the bug lines which are fixed from the given patch
     */

    public Set<String> obtainRepoNamesForCommitHashes(String gitHubToken, List<String> commitHashes) {

        commitHashes.forEach(commitHash -> {
            try {
                String jsonText = githubApiCaller.callSearchCommitApi(commitHash, gitHubToken);
                saveRepoNames(jsonText, commitHash, gitHubToken);
            } catch (CodeQualityMatricesException e) {
                logger.debug(e.getMessage(), e.getCause());
            }
        });
        return authorCommits;
    }

    /**
     * Saving the  relevant repository names that contain the given commit hash in a List.
     *
     * @param jsonText    String representation of the json response
     * @param commitHash  commit hash to be searched for containing WSO2 repositories
     * @param gitHubToken Github access token for accessing github API
     */
    private void saveRepoNames(String jsonText, String commitHash, String gitHubToken) throws
            CodeQualityMatricesException {
        List<String> repoLocation = new ArrayList<>();
        SearchApiResponse searchCommitPojo;
        try {
            searchCommitPojo = gson.fromJson(jsonText, SearchApiResponse.class);
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMatricesException(e.getMessage(), e.getCause());
        }
        searchCommitPojo.getItems()
                .forEach(recordItem -> repoLocation.add(recordItem.getRepository().getFull_name()));

        logger.debug("Repositories having the given commit are successfully saved in an List");
        SdkGitHubClient sdkGitHubClient = new SdkGitHubClient(gitHubToken);
        repoLocation.stream()
                .filter(repositoryName -> StringUtils.contains(repositoryName, "wso2/"))
                .forEach(repositoryName -> {
                    //clearing all data in the current fileNames and changedLineRanges arraylists for each Repository
                    //authorNames.clear();
                    fileNames.clear();
                    changedLineRanges.clear();
                    patchString.clear();
                    Map<String, List<String>> fileNamesWithPatcheString = null;
                    try {
                        fileNamesWithPatcheString = sdkGitHubClient.getFilesChanged(repositoryName, commitHash);
                    } catch (CodeQualityMatricesException e) {
                        logger.debug(e.getMessage(), e.getCause());
                    }
                    if (fileNamesWithPatcheString != null) {
                        fileNames = fileNamesWithPatcheString.get("fileNames");
                        patchString = fileNamesWithPatcheString.get("patchString");
                    }
                    saveRelaventEditLineNumbers(fileNames, patchString);
                    try {
                        findFileChanges(repositoryName, commitHash, gitHubToken);
                    } catch (CodeQualityMatricesException e) {
                        logger.debug(e.getMessage(), e.getCause());
                    }
                });
        if (logger.isDebugEnabled()) {
            logger.debug("\n Author names :" + authorNames);
            logger.debug("\n Author commits :" + authorCommits);
        }
    }

    /**
     * This method is used to save the line ranges being modified in a given file to a list and add that list to the
     * root list of.
     *
     * @param fileNames   Arraylist of files names that are being affected by the relevant commit
     * @param patchString Array list having the patch string value for each of the file being changed
     */

    private void saveRelaventEditLineNumbers(List<String> fileNames, List<String> patchString) {
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
    private void findFileChanges(String repoLocation, String commitHash, String gitHubToken)
            throws CodeQualityMatricesException {

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
            } catch (CodeQualityMatricesException e) {
                logger.debug(e.getMessage(), e.getCause());
            }
            //reading the above saved output for the current selected file name
            try {
                readBlameForSelectedFile(jsonText, lineRangesForSelectedFile);
                if (logger.isDebugEnabled()) {
                    logger.debug("Parent commits are saved for the " + fileName + " for all the modified line ranges");
                }
            } catch (CodeQualityMatricesException e) {
                logger.debug(e.getMessage(), e.getCause());
            }
            findAuthorCommits(owner, repositoryName, fileName, lineRangesForSelectedFile, gitHubToken);
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
            throws CodeQualityMatricesException {

        GraphQlResponse graphQlResponse;
        try {
            graphQlResponse = gson.fromJson(jsonText, GraphQlResponse.class);
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMatricesException(e.getMessage(), e.getCause());
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
                    finalGraphQlResponse.getData().getRepository().getObject().getBlame().getRanges().stream()
                            .filter(graphqlRange -> (graphqlRange.getStartingLine() <= finalStartingLineNo &&
                                    graphqlRange.getEndingLine() >= finalStartingLineNo))
                            .forEach(graphqlRange -> {
                                int age = graphqlRange.getAge();
                                String url = graphqlRange.getCommit().getHistory().getEdges().get(1).getNode().getUrl();
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
    private void findAuthorCommits(String owner, String repositoryName, String fileName,
                                   List<String> lineRangesForSelectedFile, String gitHubToken) {
        for (Map.Entry entry : parentCommitHashes.entrySet()) {
            String oldRange = (String) entry.getKey();
            Set<String> commitHashes = (Set<String>) entry.getValue();
            Graphql graphqlBean = new Graphql();
            commitHashes.parallelStream()
                    .forEach(commitHash -> {
                        graphqlBean.setGraphqlInputWithoutHistory(owner, repositoryName, commitHash, fileName);
                        jsonStructure.put("query", graphqlBean.getGraphqlInputWithoutHistory());
                        String jsonText = null;
                        try {
                            jsonText = githubApiCaller.callGraphqlApi(jsonStructure, gitHubToken);
                        } catch (CodeQualityMatricesException e) {
                            logger.debug(e.getMessage(), e.getCause());
                        }
                        saveAuthorCommits(jsonText, oldRange, lineRangesForSelectedFile);
                    });
        }
        if (logger.isDebugEnabled()) {
            logger.debug("author commits and authors of bug lines of code on " + fileName + " file which are been " +
                    "fixed by the given patch are successfully saved to lists");
        }
    }

    /**
     * This is used to save the authors and author commits of the buggy lines of code which are been fixed by the
     * given patch, to relevant lists.
     *
     * @param jsonText                  String representation of the json response
     * @param oldRange                  Range to select the correct range for collecting author commits
     * @param lineRangesForSelectedFile arraylist containing the changed line ranges of the current selected file
     */
    private void saveAuthorCommits(String jsonText, String oldRange, List<String> lineRangesForSelectedFile) {

        GraphQlResponse graphQlResponse = gson.fromJson(jsonText, GraphQlResponse.class);
        lineRangesForSelectedFile.forEach(lineRange -> {
            int startingLineNo;
            int endLineNo;
            String oldFileRange = StringUtils.substringBefore(lineRange, "/");
            // need to skip the newly created files from taking the blame as they contain no previous commits
            if (!oldFileRange.equals("0,0") && oldRange.equals(oldFileRange)) {
                // need to consider the correct line range in the old file for finding authors and author commits
                startingLineNo = Integer.parseInt(StringUtils.substringBefore(oldFileRange, ","));
                endLineNo = Integer.parseInt(StringUtils.substringAfter(oldFileRange, ","));
                while (endLineNo >= startingLineNo) {
                    int finalStartingLineNo = startingLineNo;       // to make a effective final variable
                    graphQlResponse.getData().getRepository().getObject().getBlame().getRanges().stream()
                            .filter(graphqlRange -> (graphqlRange.getStartingLine() <= finalStartingLineNo &&
                                    graphqlRange.getEndingLine() >= finalStartingLineNo))
                            .forEach(graphqlRange -> {
                                String authorName = graphqlRange.getCommit().getAuthor().getName();
                                String authorcommit = StringUtils.substringAfter(graphqlRange.getCommit().getUrl(),
                                        "commit/");
                                authorNames.add(authorName); // authors are added to the Set
                                authorCommits.add(authorcommit); // author commits are added to the set
                            });
                    startingLineNo++;   // to check for other line numbers
                }
            }
        });
    }
}
