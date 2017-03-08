/*
 *  Copyright (c) Feb 20, 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package com.wso2.code.quality.matrices;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

public class Reviewers extends BlameCommit {

    String searchPullReqeustAPIUrl;
    String locationOfSavingSearchApiOutputs;
    String pullRequestReviewAPIUrl;

    Set<String> approvedReviewers = new HashSet<String>();      // to store the reviewed and approved users of the pull requests
    Set<String> commentedReviewers = new HashSet<String>();     // to store the reviewed and commented users of the pull requests

    public String getSearchPullReqeustAPI() {
        return searchPullReqeustAPIUrl;
    }


    public void setSearchPullReqeustAPI(String commitHashToBeSearched) {


        this.searchPullReqeustAPIUrl = "https://api.github.com/search/issues?q=" + commitHashToBeSearched;
    }

    public String getLocationOfSavingSearchApiOutputs() {
        return locationOfSavingSearchApiOutputs;
    }


    public void setLocationOfSavingSearchApiOutputs(String commitHashToBeSearched) {

        this.locationOfSavingSearchApiOutputs = "/searchApiOutputs/" + commitHashToBeSearched + ".json";
    }

    public String getPullRequestReviewAPIUrl() {
        return pullRequestReviewAPIUrl;
    }


    public void setPullRequestReviewAPIUrl(String repoLocation, int pullRequestNumber) {
        this.pullRequestReviewAPIUrl = "https://api.github.com/repos/" + repoLocation + "/pulls/" + pullRequestNumber + "/reviews";
    }


    // map for storing the pull requests numbers against their repository
    Map<String, Set<Integer>> mapContainingPRNoAgainstRepoName = new HashMap<String, Set<Integer>>();


    /**
     * for finding the reviewers of each commit and storing them in array list
     */
    public void findingReviewers(Set<String> commitHashObtainedForPRReview, String githubToken) {

        Iterator commitHashObtainedForPRReviewIterator = commitHashObtainedForPRReview.iterator();

        while (commitHashObtainedForPRReviewIterator.hasNext()) {

            String commitHashForFindingReviewers = (String) commitHashObtainedForPRReviewIterator.next();
            setSearchPullReqeustAPI(commitHashForFindingReviewers);
            setLocationOfSavingSearchApiOutputs(commitHashForFindingReviewers);

            // calling the github search API
            try {
                JSONObject rootJsonObject = (JSONObject) callingTheAPI(getSearchPullReqeustAPI(), getLocationOfSavingSearchApiOutputs(), githubToken, false, true);
                // reading thus saved json file
                savingPrNumberAndRepoName(rootJsonObject);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }
        System.out.println("Done Mapping" + mapContainingPRNoAgainstRepoName);
        savingReviewersToList(githubToken);

        // printing the list of reviewers of pull requests
        printReviewUsers();



    }


    /**
     * reading the search API output and saving the PR number with the repo name in a map
     *
     * @param rootJsonObject
     */

    public void savingPrNumberAndRepoName(JSONObject rootJsonObject) {


        JSONArray itemsJsonArray = (JSONArray) rootJsonObject.get("items");

        for (int i = 0; i < itemsJsonArray.length(); i++) {
            JSONObject prJsonObject = (JSONObject) itemsJsonArray.get(i);
            // filtering only the closed repositories
            if (((String) prJsonObject.get("state")).equals("closed")) {

                String repositoryUrl = (String) prJsonObject.get("repository_url");
                String repositoryLocation = StringUtils.substringAfter(repositoryUrl, "https://api.github.com/repos/");
                if (repositoryLocation.contains("wso2/")) {
                    // to filter out only the repositories belongs to wso2

                    int pullRequetNumber = (int) prJsonObject.get("number");

                    mapContainingPRNoAgainstRepoName.putIfAbsent(repositoryLocation, new HashSet<Integer>()); // put the repo name key only if it does not exists in the map

                    mapContainingPRNoAgainstRepoName.get(repositoryLocation).add(pullRequetNumber);  // since SET is there we do not need to check for availability of the key in the map

                }

            }

        }


    }

    /**
     * Saving the reviewers of the pull requests to a list
     */
    public void savingReviewersToList(String githubToken) {

        for (Map.Entry m : mapContainingPRNoAgainstRepoName.entrySet()) {

            String productLocation = (String) m.getKey();

            @SuppressWarnings("unchecked")
            Set<Long> prNumbers = (Set<Long>) m.getValue();

            Iterator prNumberIterator = prNumbers.iterator();
            while (prNumberIterator.hasNext()) {
                int prNumber = (int) prNumberIterator.next();

                String locationForSavingOutputFile = "/ReviewApiOutputs/" + productLocation + "/ReviewFor" + prNumber + ".json";
                setPullRequestReviewAPIUrl(productLocation, prNumber);

                try {

                    JSONArray rootJsonArray = (JSONArray) callingTheAPI(getPullRequestReviewAPIUrl(), locationForSavingOutputFile, githubToken, false, true);
                    // for reading the output JSON from above and adding the reviewers to the Set
                    readingTheReviewOutJSON(rootJsonArray, productLocation, prNumber);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }


        }



    }

    /**
     * Reading the output received from the review API
     *
     * @param reviewJsonArray
     * @param productLocation
     * @param prNumber
     */
    public void readingTheReviewOutJSON(JSONArray reviewJsonArray, String productLocation, int prNumber) {



            if (reviewJsonArray.length() != 0) {

                for (int i = 0; i < reviewJsonArray.length(); i++) {
                    JSONObject reviewJsonObject = (JSONObject) reviewJsonArray.get(i);
                    if ((reviewJsonObject.get("state")).equals("APPROVED")) {

                        JSONObject userJsonObject = (JSONObject) reviewJsonObject.get("user");
                        String approvedReviwer = (String) userJsonObject.get("login");
                        approvedReviewers.add(approvedReviwer);         // adding the approved user to the Set

                    } else if ((reviewJsonObject.get("state")).equals("COMMENTED")) {
                        JSONObject userJsonObject = (JSONObject) reviewJsonObject.get("user");
                        String commentedReviwer = (String) userJsonObject.get("login");
                        commentedReviewers.add(commentedReviwer);        // adding the commented user to the Set

                    }


                }
            } else {
                System.out.println("There are no records of reviews for pull request: " + prNumber + " on " + productLocation + " repository");
            }





    }

    /**
     * Print the list of reviewers
     */
    public void printReviewUsers() {
        System.out.println("Reviewed and approved users of the bug lines: " + approvedReviewers);
        System.out.println("Reviewed and commented users on bug lines: " + commentedReviewers);

    }


}
