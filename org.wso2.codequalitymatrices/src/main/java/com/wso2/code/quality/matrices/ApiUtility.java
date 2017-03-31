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

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * This is a utility class for calling APIs.
 *
 * @since 1.0.0
 */
public final class ApiUtility {
    private static final Logger logger = Logger.getLogger(ApiUtility.class);

    // to prevent instantiation
    private ApiUtility() {
    }

    /**
     * This is used for calling the REST APIs.
     *
     * @param httpGet Instance of the relevant httpGet
     * @return String representation of the json response
     * @throws CodeQualityMatricesException
     */
    public static String callApi(HttpGet httpGet) throws CodeQualityMatricesException {
        BufferedReader bufferedReader = null;
        CloseableHttpClient httpClient;
        CloseableHttpResponse httpResponse = null;
        httpClient = HttpClients.createDefault();
        String jsonText;
        try {
            httpResponse = httpClient.execute(httpGet);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == 200) {
                //success
                bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(),
                        "UTF-8"));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                // creating a JSON object from the response
                jsonText = stringBuilder.toString();
            } else {
                throw new CodeQualityMatricesException("Error occurred while calling the API, the response code is " +
                        responseCode);
            }
        } catch (ClientProtocolException e) {
            throw new CodeQualityMatricesException("ClientProtocolException when calling the REST API", e);
        } catch (IOException e) {
            throw new CodeQualityMatricesException("IOException occurred when calling the REST API", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    logger.debug("IOException occurred when closing the BufferedReader", e);
                }
            }
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    logger.debug("IOException occurred when closing the HttpResponse", e);
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.debug("IOException occurred when closing the HttpClient", e);
                }
            }
        }
        return jsonText;
    }

    /**
     * This is used for calling the github graphql API.
     *
     * @param httpPost relevant instance of the httpost
     * @return String representation of the json response
     * @throws CodeQualityMatricesException
     */
    public static String callGraphQlApi(HttpPost httpPost) throws CodeQualityMatricesException {
        BufferedReader bufferedReader = null;
        CloseableHttpClient httpClient;
        CloseableHttpResponse httpResponse = null;
        httpClient = HttpClients.createDefault();
        String jsonText;
        try {
            httpResponse = httpClient.execute(httpPost);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == 200) {
                bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(),
                        "UTF-8"));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                jsonText = stringBuilder.toString();
                logger.debug("The response received from the Github GraphQL converted to a JSON text successfully");
            } else {
                throw new CodeQualityMatricesException("Error occurred while calling the API, the response code is " +
                        responseCode);
            }
        } catch (UnsupportedEncodingException e) {
            throw new CodeQualityMatricesException("Encoding error occured before calling the github graphQL API", e);
        } catch (ClientProtocolException e) {
            throw new CodeQualityMatricesException("Client protocol exception occurred when calling the github" +
                    " graphQL API", e);
        } catch (IOException e) {
            throw new CodeQualityMatricesException("A problem or the connection was aborted while executing the" +
                    " httpPost", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    logger.debug("IOException occurred when closing the buffered reader", e);
                }
            }
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    logger.debug("IOException occurred when closing the HttpResponse", e);
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.debug("IOException occurred when closing the HttpClient", e);
                }
            }
        }
        return jsonText;
    }
}
