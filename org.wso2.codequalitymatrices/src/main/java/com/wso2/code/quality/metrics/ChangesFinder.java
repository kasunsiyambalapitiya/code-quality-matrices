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
import com.wso2.code.quality.metrics.model.SingleCommitApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class is used for getting the blame information on relevant changed line ranges on relevant files from the
 * given patch.
 *
 * @since 1.0.0
 */

public class ChangesFinder {
    private static final Logger logger = Logger.getLogger(ChangesFinder.class);

    private Set<String> authorNames = new HashSet<>();    //authors of the bug lines fixed from the patch
    private Set<String> authorCommits = new HashSet<>();  //  author commits of the bug lines fixed from the patch
    private final JSONObject jsonStructure = new JSONObject();
    private final GithubApiCaller githubApiCaller = new GithubApiCaller();
    private final Gson gson = new Gson();

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
                    Map<String, String> fileNamesWithPatchString = new HashMap<>();
                    try {
                        fileNamesWithPatchString = sdkGitHubClient.getFilesChanged(repositoryName, commitHash);
                        Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers = new HashMap<>();
                        Map<String, String> fileNamesWithPreviousCommitHash = new HashMap<>();
                        if (fileNamesWithPatchString != null) {
                            /* looping from one file to file and saving deleted lines against the file name in another
                             map */
                            for (Map.Entry<String, String> entry : fileNamesWithPatchString.entrySet()) {
                                String fileName = entry.getKey();
                                String patchString = entry.getValue();
                                Set<Integer> deletedLines = identifyDeletedLines(patchString);
                                String previousCommitHash;
                                //for omitting files without having deleted lines
                                if (deletedLines.size() > 0) {
                                    fileNamesWithDeletedLineNumbers.put(fileName, deletedLines);
                                    previousCommitHash = checkForOctopussMerge(repositoryName, fileName, commitHash,
                                            gitHubToken);
                                    fileNamesWithPreviousCommitHash.put(fileName, previousCommitHash);
                                }
                            }
                        }
                        getBlameDetails(repositoryName, fileNamesWithPreviousCommitHash,
                                fileNamesWithDeletedLineNumbers, gitHubToken);
                    } catch (CodeQualityMetricsException e) {
                        logger.error(e.getMessage(), e.getCause());
                    }
                });
        if (logger.isDebugEnabled()) {
            logger.debug("\n Author names :" + authorNames);
            logger.debug("\n Author commits :" + authorCommits);
        }
    }

    /**
     * This check whether the commit in the given patch is an octopuss commit or not, if it is octopuss commit an
     * exception as the current scope of the program doesnot support octopuss commits
     *
     * @param repositoryName   current selected Repository
     * @param filePath         current selected file name
     * @param latestCommitHash current selected recent commit contained in the given patch
     * @param githubToken      github access token for accessing github REST API
     * @return the commit hash returned from findPreviousCommitOfFile method
     * @throws CodeQualityMetricsException notifying that the current scope of the program does not support octopuss
     *                                     merges
     */
    public String checkForOctopussMerge(String repositoryName, String filePath, String latestCommitHash, String
            githubToken) throws CodeQualityMetricsException {
        String previousCommit = null;
        try {
            // need check whether the given commit has more than 2 parent commits
            String singleCommitJsonText = githubApiCaller.callSingleCommitApi(repositoryName, latestCommitHash,
                    githubToken);
            if (singleCommitJsonText != null) {
                SingleCommitApiResponse singleCommitApiResponse = gson.fromJson(singleCommitJsonText,
                        SingleCommitApiResponse.class);
                if (singleCommitApiResponse.getParentCommits().size() == 1) {
                    /*if the singleCommitApiResponse.getParentCommits().size() == 1 it is fast forward merge, so no need
                 to change the latestCommitHash*/
                    if (logger.isDebugEnabled()) {
                        logger.debug("The commit " + latestCommitHash + " in relevant patch is not a octopuss merge " +
                                "commit");
                    }
                } else if (singleCommitApiResponse.getParentCommits().size() == 2) {
                    latestCommitHash = singleCommitApiResponse.getParentCommits().get(1).getCommitHash();
                    if (logger.isDebugEnabled()) {
                        logger.debug("The commit " + latestCommitHash + " in relevant patch is not a octopuss merge " +
                                "commit");
                    }
                } else if (singleCommitApiResponse.getParentCommits().size() > 2) {
                    throw new CodeQualityMetricsException("The commit " + latestCommitHash + " in relevant patch is " +
                            "an octopuss merge commit, octopuss merge commits are avoided in the current scope of " +
                            "the program");
                }
                previousCommit = findPreviousCommitOfFile(repositoryName, filePath, latestCommitHash, githubToken);
            }
        } catch (CodeQualityMetricsException e) {
            logger.error(e.getMessage(), e.getCause());
        }
        return previousCommit;
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
    public String findPreviousCommitOfFile(String repositoryName, String filePath, String latestCommitHash,
                                           String githubToken) {
        String previousCommit = null;
        Map<String, String> commitWithDate = new HashMap<>();
        try {
            String commitHistoryJsonText = githubApiCaller.callCommitHistoryApi(repositoryName, filePath, githubToken);
            if (commitHistoryJsonText != null) {
                Type listType = new ListType().getType();
                List<CommitHistoryApiResponse> commitHistoryApiResponses = gson.fromJson(commitHistoryJsonText,
                        listType);
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
        fileNamesWithPreviousCommitHash.forEach((fileName, previousCommitHash) -> {
            graphqlBean.setGraphqlInputWithoutHistory(owner, repositoryName, previousCommitHash, fileName);
            jsonStructure.put("query", graphqlBean.getGraphqlInputWithoutHistory());
            String jsonText = null;
            try {
                // calling the graphql API for getting blame information for the current file.
                jsonText = githubApiCaller.callGraphqlApi(jsonStructure, githubToken);
                findAuthorCommits(jsonText, fileNamesWithDeletedLineNumbers, fileName);
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });
    }

    /**
     * This is used to save the authors and relevant author commits of the buggy lines of code which are been fixed by
     * the given patch, to relevant lists.
     *
     * @param jsonText                        String representation of the json response received from github graphql
     *                                        API
     * @param fileNamesWithDeletedLineNumbers map containing changed files with their deleted line numbers
     * @param selectedFileName                selected file for finding the authors and author commits of its buggy
     *                                        lines
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
