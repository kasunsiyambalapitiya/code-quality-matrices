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
import com.wso2.code.quality.matrices.model.IssueApiResponse;
import com.wso2.code.quality.matrices.model.ReviewApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to find the reviewers of the buggy lines of code.
 *
 * @since 1.0.0
 */

public class ReviewAnalyser {

    private final Set<String> approvedReviewers = new HashSet<>();      // to store the reviewed and approved users of the pull
    // requests
    private final Set<String> commentedReviewers = new HashSet<>();     // to store the reviewed and commented users of the pull
    // requests

    private static final Logger logger = Logger.getLogger(ReviewAnalyser.class);

    //constants for filtering github API responses
    private static final String GITHUB_REVIEW_APPROVED = "APPROVED";
    private static final String GITHUB_REVIEW_COMMENTED = "COMMENTED";
    private static final String GITHUB_REVIEW_API_CLOSED_STATE = "closed";
    private final GithubApiCaller githubApiCaller = new GithubApiCaller();
    private final Gson gson = new Gson();

    /**
     * This is used to identify the pull requests that introduce the given commit to the code base.
     *
     * @param authorCommits Commits which the relevant pull request no must be found
     * @param githubToken   Github access token for accessing github API
     */
    public void findReviewers(Set<String> authorCommits, String githubToken) {

        authorCommits.forEach(commitHash -> {
            String jsonText;

            try {
                jsonText = githubApiCaller.callSearchIssueApi(commitHash, githubToken);
                Map<String, Set<Integer>> prNoWithRepoName = savePrNumberAndRepoName(jsonText);
                logger.debug("Pull requests with their relevant repository names are successfully saved in a map.");
                saveReviewers(prNoWithRepoName, githubToken);
            } catch (CodeQualityMatricesException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });
    }

    /**
     * This is used to save the pull requests with their relevant repository names in a map.
     *
     * @param jsonText json reponse received from the github issue API
     * @return a map of pull requests againt their repository name
     * @throws CodeQualityMatricesException
     */
    private Map<String, Set<Integer>> savePrNumberAndRepoName(String jsonText) throws CodeQualityMatricesException {

        // map for storing the pull requests numbers against their Repository
        Map<String, Set<Integer>> prNoWithRepoName = new HashMap<>();
        try {
            IssueApiResponse issueApiResponse = gson.fromJson(jsonText, IssueApiResponse.class);

            issueApiResponse.getItems().parallelStream()
                    .filter(searchItem -> GITHUB_REVIEW_API_CLOSED_STATE.equals(searchItem.getState()))
                    .filter(searchItem -> StringUtils.contains(searchItem.getRepositoryUrl(), "/wso2/"))
                    .forEach(searchItem -> {
                        String repositoryName = StringUtils.substringAfter(searchItem.getRepositoryUrl(), "repos/");
                        int pullRequestNo = searchItem.getNumber();
                        prNoWithRepoName.putIfAbsent(repositoryName, new HashSet<>());
                        if (!prNoWithRepoName.get(repositoryName).contains(pullRequestNo)) {
                            prNoWithRepoName.get(repositoryName).add(pullRequestNo);
                        }

                    });
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMatricesException(e.getMessage(), e.getCause());
        }
        return prNoWithRepoName;
    }

    /**
     * This is used to save the names of the users who has approved and commented on the pull requests which
     * introduce bug lines of code to the code base. Approved users are saved in approvedReviewers list while
     * commented users are saved in commentedReviewers list
     *
     * @param prNoWithRepoName Map containg the pull requests which introduce bug lines to the code base against the
     *                         relevant reposiory
     * @param githubToken      Github access token for accessing github API
     */
    private void saveReviewers(Map<String, Set<Integer>> prNoWithRepoName, String githubToken) {

        for (Map.Entry entry : prNoWithRepoName.entrySet()) {
            String repositoryName = (String) entry.getKey();
            Set<Integer> prNumbers = (Set<Integer>) entry.getValue();
            prNumbers.parallelStream()
                    .forEach(prNumber -> {

                        try {
                            String jsonText = githubApiCaller.callReviewApi(repositoryName, prNumber, githubToken);
                            if (jsonText != null) {
                                List<ReviewApiResponse> reviews = gson.fromJson(jsonText, List.class);
                                // to filter Approved users
                                reviews.parallelStream()
                                        .filter(review -> GITHUB_REVIEW_APPROVED.equals(review.getState()))
                                        .forEach(review -> approvedReviewers.add(review.getReviewer().getName()));

                                logger.debug("Users who approved the pull requests which introduce bug lines to the code base are" +
                                        " successfully saved to approvedReviewers list");

                                reviews.parallelStream()
                                        .filter(review -> GITHUB_REVIEW_COMMENTED.equals(review.getState()))
                                        .forEach(review -> commentedReviewers.add(review.getReviewer().getName()));

                                logger.debug("Users who commented on the pull requests which introduce bug lines to the code base are" +
                                        " successfully saved to approvedReviewers list");
                            } else {
                                System.out.println("There are no records of reviews for pull request: " + prNumber + " on " +
                                        repositoryName + " repository");
                                logger.debug("There are no records of reviews for pull request: " + prNumber + " on " +
                                        repositoryName +
                                        " repository");
                            }
                        } catch (CodeQualityMatricesException e) {
                            logger.error(e.getMessage(), e.getCause());
                        }
                    });
        }
    }

    /**
     * Print the list of reviewers and commented users on the pull requests which introduce bugs to the code base
     */
    public void printReviewUsers() {
        System.out.println("Reviewed and approved users of the bug lines: " + approvedReviewers);
        System.out.println("Reviewed and commented users on bug lines: " + commentedReviewers);
    }
}
