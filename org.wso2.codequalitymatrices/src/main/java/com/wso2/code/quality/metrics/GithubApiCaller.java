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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import static com.wso2.code.quality.metrics.model.Constants.ACCEPT;
import static com.wso2.code.quality.metrics.model.Constants.AUTHORIZATION;
import static com.wso2.code.quality.metrics.model.Constants.BEARER;

/**
 * This is used for all github communications.
 *
 * @since 1.0.0
 */
public class GithubApiCaller {
    private HttpGet httpGet;
    private final Properties defaultProperties = new Properties();
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final InputStream inputStream = classLoader.getResourceAsStream("url.properties");


    /**
     * This is used for calling the github search REST API.
     *
     * @param commitHash        commit hash to be searched
     * @param githubAccessToken Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public String callSearchCommitApi(String commitHash, String githubAccessToken) throws CodeQualityMetricsException {
        try {
            defaultProperties.load(inputStream);
            String url = defaultProperties.getProperty("searchCommitApiUrl") + commitHash;
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
            //as the accept header is needed for accessing commit search API which is still in preview mode
            httpGet.addHeader(ACCEPT, defaultProperties.getProperty("searchCommitApiHeader"));
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.callApi(httpGet);
    }

    /**
     * This is used to call github commit REST API
     *
     * @param repoLocation
     * @param filePath
     * @param githubAccessToken
     * @return
     * @throws CodeQualityMetricsException
     */
    public String callCommitHistoryApi(String repoLocation, String filePath, String githubAccessToken)
            throws CodeQualityMetricsException {
        try {
            defaultProperties.load(inputStream);
            String tempUrl = defaultProperties.getProperty("commitHistoryApiUrl");
            String url = tempUrl.replaceFirst("REPO_LOCATION", repoLocation).replaceFirst("FILE_NAME", filePath);
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.callApi(httpGet);
    }

    /**
     * This is used to call github single commit REST API
     * @param repoLocation
     * @param commitHash
     * @param githubAccessToken
     * @return
     */
    public String callSingleCommitApi(String repoLocation, String commitHash, String githubAccessToken)
            throws CodeQualityMetricsException {
        try {
            defaultProperties.load(inputStream);
            String tempUrl = defaultProperties.getProperty("singleCommitApiUrl");
            String url = tempUrl.replaceFirst("REPO_LOCATION", repoLocation).replaceFirst("COMMIT_HASH", commitHash);
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.callApi(httpGet);

    }

    /**
     * This is used to call the github review API.
     *
     * @param repoLocation      repository name
     * @param pullRequestNumber pull request number to be queried for
     * @param githubAccessToken Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public String callReviewApi(String repoLocation, int pullRequestNumber, String githubAccessToken) throws
            CodeQualityMetricsException {
        try {
            defaultProperties.load(inputStream);
            String tempUrl = defaultProperties.getProperty("reviewApiUrl");
            String url = tempUrl.replaceFirst("REPO_LOCATION", repoLocation).replaceFirst("PULL_REQUEST_NUMBER",
                    String.valueOf(pullRequestNumber));
            httpGet = new HttpGet(url);
            httpGet.addHeader(ACCEPT, defaultProperties.getProperty("reviewApiUrlHeader"));
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Review Commit API is " +
                    "invalid ", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.callApi(httpGet);
    }

    /**
     * This is used to call the github Issue Search API.
     *
     * @param commitHashToBeSearched commit hash to be searched for issues
     * @param githubAccessToken      Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public String callSearchIssueApi(String commitHashToBeSearched, String githubAccessToken) throws
            CodeQualityMetricsException {
        try {
            defaultProperties.load(inputStream);
            String url = defaultProperties.getProperty("searchIssueApiUrl") + commitHashToBeSearched;
            httpGet = new HttpGet(url);
            httpGet.addHeader(ACCEPT, defaultProperties.getProperty("searchIssueApiUrlHeader"));
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Issue API is " +
                    "invalid ", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.callApi(httpGet);
    }

    /**
     * This is used for calling the github graphql API.
     *
     * @param graphqlJsonStructure JSON input structure for calling the graphql API
     * @param githubToken          Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public String callGraphqlApi(JSONObject graphqlJsonStructure, String githubToken) throws
            CodeQualityMetricsException {
        HttpPost httpPost;
        try {
            defaultProperties.load(inputStream);
            String url = defaultProperties.getProperty("githubGraphqlUrl");
            httpPost = new HttpPost(url);
            httpPost.addHeader(AUTHORIZATION, BEARER + githubToken);
            httpPost.addHeader(ACCEPT, defaultProperties.getProperty("githubGraphqlUrlHeader"));
            StringEntity entity = new StringEntity(graphqlJsonStructure.toString());
            httpPost.setEntity(entity);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Graphql API is " +
                    "invalid", e);
        } catch (UnsupportedEncodingException e) {
            throw new CodeQualityMetricsException("An error occurred when creating the String entity from Json " +
                    "Structure", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.callGraphQlApi(httpPost);
    }
}
