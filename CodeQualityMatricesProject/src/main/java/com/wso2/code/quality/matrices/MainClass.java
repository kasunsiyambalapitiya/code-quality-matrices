/*
 *  Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package com.wso2.code.quality.matrices;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import java.util.Set;
import java.util.logging.Logger;


/**
 * This is the class having the main method of this application
 * PMT Access token, patch id and github access token
 * should be passed as command line arguments when running the application
 */
public class MainClass {
    private final static Logger logger = Logger.getLogger(MainClass.class.getName());

    public static void main(String[] args) {
        logger.info(" Main method got executed");

//        String pmtToken = args[0];
//        String patchId = args[1];
//
//        String pmtUrl = "http://umt.private.wso2.com:9765/codequalitymatricesapi/1.0.0//properties?path=/_system/governance/patchs/" + patchId;
//
//        RestApiCaller restApiCaller = new RestApiCaller();
//        JSONArray jsonArray = (JSONArray) restApiCaller.callingTheAPI(pmtUrl, pmtToken, false, false);

        logger.info("JSON response is received successfully from WSO2 PMT for the given patch " + args[1]);

//        Pmt pmt = new Pmt();
//        String[] commitsInTheGivenPatch = pmt.getThePublicGitCommitId(jsonArray);
        logger.info("Commits received from WSO2 PMT are saved in an array successfully");

        logger.info("Commits received from WSO2 PMT are saved in an array successfully");

//        String gitHubToken = args[2];
//
//        BlameCommit blameCommit = new BlameCommit();
//        Set<String> commitHashObtainedForPRReview = blameCommit.obtainingRepoNamesForCommitHashes(gitHubToken, commitsInTheGivenPatch, restApiCaller);
        logger.info("Author commits that introduce bug lines of code to the repository are saved in commitHashObtainedForPRReview SET successfully");

        logger.info("Author commits that introduce bug lines of code to the repository are saved in commitHashObtainedForPRReview SET successfully");

//        Reviewers reviewers = new Reviewers();
//        reviewers.findingReviewers(commitHashObtainedForPRReview, gitHubToken);

        GitHubAuthentication gitHubAuthentication= new GitHubAuthentication();
        gitHubAuthentication.gettingGithubClient();
    }
}
