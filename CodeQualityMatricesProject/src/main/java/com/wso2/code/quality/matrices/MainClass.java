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

import org.apache.log4j.Logger;

/**
 * This is the class having the main method of this application
 * PMT Access token, patch id and github access token
 * should be passed in order as command line arguments when running the application
 */
public class MainClass {
    private final static Logger logger = Logger.getLogger(MainClass.class);
    public static final String COMMITS_INSIDE_GIVEN_PATCH = "patchInformation_svnRevisionpublic";

    public static void main(String[] args) {
        if (args.length == 3) {

            String pmtToken = args[0];
            String patchId = args[1];
            String gitHubToken = args[2];

            CodeQualityMatrices codeQualityMatrices= new CodeQualityMatrices(pmtToken,patchId,gitHubToken);
            codeQualityMatrices.execute();

//            PmtApiCaller pmtApiCaller = new PmtApiCaller();
//            String jsonText = null;
//            List <String> commitHashes= new ArrayList<>();
//
//            try {
//                jsonText = pmtApiCaller.callApi(pmtToken, patchId);
//            } catch (CodeQualityMatricesException e) {
//                logger.error("Error occurred while calling PMT API", e);
//            }
//
//            Gson gson = new Gson();
//
//            if (jsonText != null) {
//                List pmtResponse = gson.fromJson(jsonText, List.class);
//                for(Object pmtEntry : pmtResponse){
//                    if(pmtEntry instanceof Map){
//                        Map<String,List<String>> entryMap= (Map<String,List<String>>)pmtEntry;
//                        if(COMMITS_INSIDE_GIVEN_PATCH.equals(entryMap.get("name"))){
//                            commitHashes=entryMap.get("values");
//
//
//                        }
//
//                    }
//                }
//            }

//            String pmtUrl = "fda";
//            RestApiCaller restApiCaller = new RestApiCaller();
//            JSONArray jsonArray = null;
//            try {
//                jsonArray = (JSONArray) restApiCaller.callApi(pmtUrl, pmtToken, false, false);
//            } catch (CodeQualityMatricesException e) {
//                logger.error(e.getMessage(), e.getCause());
//                System.exit(1);
//            }
//            logger.info("JSON response is received successfully from WSO2 PMT for the given patch " + args[1]);
//
//            Pmt pmt = new Pmt();
//            String[] commitsInTheGivenPatch = null;
//            if (jsonArray != null) {
//                commitsInTheGivenPatch = pmt.getPublicGitCommitHashes(jsonArray);
//            }
//            logger.info("Commits received from WSO2 PMT are saved in an array successfully");
//
//
//            ChangesFinder changesFinder = new ChangesFinder();
//            Set<String> commitHashObtainedForPRReview = null;
//            if (commitsInTheGivenPatch != null) {
//                commitHashObtainedForPRReview = changesFinder.obtainRepoNamesForCommitHashes(gitHubToken, commitsInTheGivenPatch, restApiCaller);
//            }
//            logger.info("Author commits that introduce bug lines of code to the Repository are saved in commitHashObtainedForPRReview SET successfully");
//
//            Reviewer reviewer = new Reviewer();
//            if (commitHashObtainedForPRReview != null) {
//                reviewer.findReviewers(commitHashObtainedForPRReview, gitHubToken, restApiCaller);
//            }
//        } else {
//            logger.error("at least one of the command line arguments are null.");
//            System.exit(4);
//        }
    }}
}
