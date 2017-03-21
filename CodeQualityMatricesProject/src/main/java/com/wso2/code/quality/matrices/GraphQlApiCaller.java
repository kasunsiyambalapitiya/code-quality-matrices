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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * This class is used for calling the GithubGraphQL API which is currently in early access mode.
 *
 * @since 1.0.0
 */

public class GraphQlApiCaller {

    protected static final Logger logger = Logger.getLogger(GraphQlApiCaller.class);

    /**
     * Calls the github graphQL API and returns the relevant JSON response received
     *
     * @param queryObject the JSONObject required for querying
     * @param gitHubToken github token for accessing github GraphQL API
     * @return Depending on the content return a JSONObject or a JSONArray
     * @throws IOException
     */
    public Object callGraphQlApi(JSONObject queryObject, String gitHubToken) throws CodeQualityMatricesException {

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        BufferedReader bufferedReader = null;

        Object returnedObject = null;

        try {
            client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://api.github.com/graphql");
            httpPost.addHeader("Authorization", "Bearer " + gitHubToken);
            httpPost.addHeader("Accept", "application/json");
            StringEntity entity = new StringEntity(queryObject.toString());
            httpPost.setEntity(entity);
            response = client.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();

            switch (responseCode) {
                case 200:

                    bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    String jsonText = stringBuilder.toString();
                    logger.info("The response received from the Github GraphQL converted to a JSON text successfully");

                    Object json = new JSONTokener(jsonText).nextValue();     // gives an object http://stackoverflow.com/questions/14685777/how-to-check-if-response-from-server-is-jsonaobject-or-jsonarray

                    if (json instanceof JSONObject) {
                        JSONObject jsonObject = (JSONObject) json;
                        returnedObject = jsonObject;
                        logger.info("JSONObject was returned successfully after calling the GraphQL API");
                    } else if (json instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray) json;
                        returnedObject = jsonArray;
                        logger.info("JSONArray was returned successfully after calling the GraphQL API");
                    }
                    break;

                case 401:
                    // to handle Response code 401: Unauthorized
                    throw new CodeQualityMatricesException("Response code 401 : Git hub access token is invalid");
                default:
                    returnedObject = null;
            }
        } catch (UnsupportedEncodingException e) {
            throw new CodeQualityMatricesException("Encoding error occured before calling the github graphQL API", e);
        } catch (ClientProtocolException e) {
            throw new CodeQualityMatricesException("Client protocol exception occurred when calling the github graphQL API", e);
        } catch (IOException e) {
            throw new CodeQualityMatricesException("IO Exception occured when calling the github graphQL API", e);
        } catch (Exception e) {
            throw new CodeQualityMatricesException("Exception occurred when reading the response received from github graphQL API", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new CodeQualityMatricesException("IOException occurred when closing the buffered reader", e);
                }
            }
        }
        return returnedObject;
    }
}
