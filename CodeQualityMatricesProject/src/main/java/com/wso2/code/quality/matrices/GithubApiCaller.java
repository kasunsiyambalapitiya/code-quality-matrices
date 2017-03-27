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

import org.apache.http.client.methods.HttpGet;

/**
 * This is used for all github communications
 */
public class GithubApiCaller {
    private HttpGet httpGet;

    /**
     * @param commitHash
     * @param accessToken
     * @throws CodeQualityMatricesException
     */
    public String callSearchCommitApi(String commitHash, String accessToken) throws CodeQualityMatricesException {
        String url = "https://api.github.com/search/commits?q=hash%3A" + commitHash;
        try {
            httpGet = new HttpGet(url);
            httpGet.addHeader("Authorization", "Bearer " + accessToken);
            //as the accept header is needed for accessing commit search API which is still in preview mode
            httpGet.addHeader("Accept", "application/vnd.github.cloak-preview");
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMatricesException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        }
        return ApiUtility.callApi(httpGet);
    }

    /**
     * @param repoLocation
     * @param pullRequestNumber
     * @param accessToken
     * @throws CodeQualityMatricesException
     */
    public String callReviewApi(String repoLocation, String pullRequestNumber, String accessToken) throws
            CodeQualityMatricesException {
        String url = "https://api.github.com/repos/" + repoLocation + "/pulls/" + pullRequestNumber + "/reviews";
        try {
            httpGet = new HttpGet(url);
            httpGet.addHeader("Accept", "application/vnd.github.black-cat-preview+json");
            httpGet.addHeader("Authorization", "Bearer " + accessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMatricesException("The url provided for accessing the Github Review Commit API is " +
                    "invalid ", e);
        }
        return ApiUtility.callApi(httpGet);
    }

    /**
     * @param commitHashToBeSearched
     * @param accessToken
     * @throws CodeQualityMatricesException
     */
    public String callSearchIssueApi(String commitHashToBeSearched, String accessToken) throws
            CodeQualityMatricesException {
        String url = "https://api.github.com/search/issues?q=" + commitHashToBeSearched;
        try {
            httpGet = new HttpGet(url);
            httpGet.addHeader("Accept", "application/vnd.github.mercy-preview+json");
            httpGet.addHeader("Authorization", "Bearer " + accessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMatricesException("The url provided for accessing the Github Search Issue API is " +
                    "invalid ", e);
        }
        return ApiUtility.callApi(httpGet);
    }
}

