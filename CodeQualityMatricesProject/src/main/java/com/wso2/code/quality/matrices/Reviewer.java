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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to find the revierwers of the buggy lines of code
 */

public class Reviewer {

    String searchPullReqeustAPIUrl;
    String pullRequestReviewAPIUrl;
    Set<String> approvedReviewers = new HashSet<String>();      // to store the reviewed and approved users of the pull requests
    Set<String> commentedReviewers = new HashSet<String>();     // to store the reviewed and commented users of the pull requests

    private static final Logger logger = Logger.getLogger(Reviewer.class);

    //constants for accessing github API
    private static final String GITHUB_REVIEW_API_STATE_KEY = "state";
    private static final String GITHUB_REVIEW_API_APPROVED_KEY = "APPROVED";
    private static final String GITHUB_REVIEW_API_COMMENTED_KEY = "COMMENTED";
    private static final String GITHUB_REVIEW_API_LOGIN_KEY = "login";
    private static final String GITHUB_REVIEW_API_USER_KEY = "user";
    private static final String GITHUB_REVIEW_API_ITEMS_KEY = "items";
    private static final String GITHUB_REVIEW_API_REPOSITORY_URL_KEY = "repository_url";
    private static final String GITHUB_REVIEW_API_NUMBER_KEY = "number";
    private static final String GITHUB_REVIEW_API_CLOSED_STATE_KEY = "closed";


    public String getSearchPullReqeustAPI() {
        return searchPullReqeustAPIUrl;
    }

    /**
     * Sets the URL for Github Search API
     *
     * @param commitHashToBeSearched commit hash to be searched used for finding the Pull requests
     */

    public void setSearchPullReqeustAPI(String commitHashToBeSearched) {
        this.searchPullReqeustAPIUrl = "https://api.github.com/search/issues?q=" + commitHashToBeSearched;
    }

    public String getPullRequestReviewAPIUrl() {
        return pullRequestReviewAPIUrl;
    }

    public void setPullRequestReviewAPIUrl(String repoLocation, int pullRequestNumber) {
        this.pullRequestReviewAPIUrl = "https://api.github.com/repos/" + repoLocation + "/pulls/" + pullRequestNumber + "/reviews";
    }

    // map for storing the pull requests numbers against their Repository
    Map<String, Set<Integer>> mapContainingPRNoAgainstRepoName = new HashMap<String, Set<Integer>>();

    /**
     * for finding the reviewers of each commit and storing them in a Set
     *
     * @param commitHashObtainedForPRReview commit hash Set for finding the pull requests
     * @param githubToken                   github token for accessing github REST API
     */
    public void findReviewers(Set<String> commitHashObtainedForPRReview, String githubToken, RestApiCaller restApiCaller) {

        commitHashObtainedForPRReview.stream()
                .forEach(commitHashForFindingReviewers -> {
                    setSearchPullReqeustAPI(commitHashForFindingReviewers);
                    // calling the github search API
                    JSONObject rootJsonObject = null;
                    try {
                        rootJsonObject = (JSONObject) restApiCaller.callApi(getSearchPullReqeustAPI(), githubToken, false, true);
                    } catch (CodeQualityMatricesException e) {
                        logger.error(e.getMessage(), e.getCause());
                        System.exit(1);
                    }
                    // reading thus saved json file
                    if (rootJsonObject != null) {
                        savePrNumberAndRepoName(rootJsonObject);
                    }
                });

        logger.info("PR numbers which introduce bug lines of code with their relevant Repository are saved successfully to mapContainingPRNoAgainstRepoName map");
        saveReviewersToList(githubToken, restApiCaller);
        logger.info("List of approved reviwers and comment users of the PRs which introduce bug lines to Repository are saved in commentedReviewers and approvedReviewers list ");
        // printing the list of reviewers of pull requests
        printReviewUsers();
        logger.info("Names of approved reviewers and commented reviewers are printed successfully");
    }

    /**
     * reads the search API output and save the pull request number with the repo name in a map
     *
     * @param rootJsonObject JSONObject received from github search API
     */
    public void savePrNumberAndRepoName(JSONObject rootJsonObject) {
        JSONArray itemsJsonArray = (JSONArray) rootJsonObject.get(GITHUB_REVIEW_API_ITEMS_KEY);

        Pmt.arrayToStream(itemsJsonArray)
                .map(JSONObject.class::cast)
                .filter(o -> o.get(GITHUB_REVIEW_API_STATE_KEY).equals(GITHUB_REVIEW_API_CLOSED_STATE_KEY))
                .forEach(prJsonObject -> {
                    String repositoryUrl = (String) prJsonObject.get(GITHUB_REVIEW_API_REPOSITORY_URL_KEY);
                    String repositoryLocation = StringUtils.substringAfter(repositoryUrl, "https://api.github.com/repos/");
                    if (repositoryLocation.contains("wso2/")) {
                        // to filter out only the repositories belongs to wso2
                        int pullRequetNumber = (int) prJsonObject.get(GITHUB_REVIEW_API_NUMBER_KEY);
                        mapContainingPRNoAgainstRepoName.putIfAbsent(repositoryLocation, new HashSet<Integer>()); // put the repo name key only if it does not exists in the map
                        mapContainingPRNoAgainstRepoName.get(repositoryLocation).add(pullRequetNumber);  // since SET is there we do not need to check for availability of the key in the map
                    }
                });
    }

    /**
     * Calling the github review API for a selected pull request on its relevant product
     *
     * @param githubToken github token for accessing github REST API
     */
    public void saveReviewersToList(String githubToken, RestApiCaller restApiCaller) {

        for (Map.Entry m : mapContainingPRNoAgainstRepoName.entrySet()) {
            String productLocation = (String) m.getKey();
            Set<Integer> prNumbers = (Set<Integer>) m.getValue();
            prNumbers.stream()
                    .forEach(prNumber -> {
                        setPullRequestReviewAPIUrl(productLocation, prNumber);
                        JSONArray reviewJsonArray = null;
                        try {
                            reviewJsonArray = (JSONArray) restApiCaller.callApi(getPullRequestReviewAPIUrl(), githubToken, false, true);
                        } catch (CodeQualityMatricesException e) {
                            logger.error(e.getMessage(), e.getCause());
                            System.exit(1);
                        }
                        // for reading the output JSON from above and adding the reviewers to the Set
                        if (reviewJsonArray != null) {
                            readTheReviewOutJSON(reviewJsonArray, productLocation, prNumber);
                        }

                    });
        }
    }

    /**
     * Reading the output received from the review API and saving the relevant reviewers and commented users to relevant Sets
     *
     * @param reviewJsonArray JSON response from the github Review API
     * @param productLocation Product Location for printing the error message when there are no reviewers and a commented users
     * @param prNumber        relevant PR number for finding the reviewers and commenters
     */
    public void readTheReviewOutJSON(JSONArray reviewJsonArray, String productLocation, int prNumber) {

        if (reviewJsonArray.length() != 0) {
            Pmt.arrayToStream(reviewJsonArray)
                    .map(JSONObject.class::cast)
                    .forEach(reviewJsonObject->{
                        addRelevantUsersToList(reviewJsonObject);
                    });

//            for (Object object : reviewJsonArray) {
//                if (object instanceof JSONObject) {
//                    JSONObject reviewJsonObject = (JSONObject) object;
//
//
//                }
//            }
        } else {
            System.out.println("There are no records of reviews for pull request: " + prNumber + " on " + productLocation + " Repository");
            logger.info("There are no records of reviews for pull request: " + prNumber + " on " + productLocation + " Repository");
        }
    }

    /**
     * This method is used for saving the relevant reviewers and commented users to relevant Sets
     *
     * @param reviewJsonObject jsonObject received from readTheReviewOutJSON method
     */

    public void addRelevantUsersToList(JSONObject reviewJsonObject) {
        if ((reviewJsonObject.get(GITHUB_REVIEW_API_STATE_KEY)).equals(GITHUB_REVIEW_API_APPROVED_KEY)) {

            JSONObject userJsonObject = (JSONObject) reviewJsonObject.get(GITHUB_REVIEW_API_USER_KEY);
            String approvedReviwer = (String) userJsonObject.get(GITHUB_REVIEW_API_LOGIN_KEY);
            approvedReviewers.add(approvedReviwer);         // adding the approved user to the Set

        } else if ((reviewJsonObject.get(GITHUB_REVIEW_API_STATE_KEY)).equals(GITHUB_REVIEW_API_COMMENTED_KEY)) {
            JSONObject userJsonObject = (JSONObject) reviewJsonObject.get(GITHUB_REVIEW_API_USER_KEY);
            String commentedReviwer = (String) userJsonObject.get(GITHUB_REVIEW_API_LOGIN_KEY);
            commentedReviewers.add(commentedReviwer);        // adding the commented user to the Set
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
