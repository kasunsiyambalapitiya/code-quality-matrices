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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This is a utility class for calling APIs
 */
public final class ApiUtility {
    // to prevent instantiation
    private ApiUtility() {

    }

    private static Logger logger = Logger.getLogger(ApiUtility.class);

    public static String callApi(HttpGet httpGet) throws CodeQualityMatricesException {
        BufferedReader bufferedReader = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        httpClient = HttpClients.createDefault();
        String jsonText = null;

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
                throw new CodeQualityMatricesException("Error occurred while calling the API, the reponse code is " +
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
                    logger.error("IOException occurred when closing the BufferedReader", e);
                }
            }
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    logger.error("IOException occurred when closing the HttpResponse", e);
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("IOException occurred when closing the HttpClient", e);
                }
            }
        }
        return jsonText;
    }
}
