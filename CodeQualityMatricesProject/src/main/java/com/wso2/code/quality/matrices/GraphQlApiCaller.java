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

public class GraphQlApiCaller {

    protected static final Logger GraphQlApiCallerLogger = Logger.getLogger(GraphQlApiCaller.class);

    /**
     * Calling the github graphQL API
     *
     * @param queryObject the JSONObject required for querying
     * @param gitHubToken github token for accessing github GraphQL API
     * @return Depending on the content return a JSONObject or a JSONArray
     * @throws IOException
     */
    public Object callingGraphQl(JSONObject queryObject, String gitHubToken) throws IOException {

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://api.github.com/graphql");
        httpPost.addHeader("Authorization", "Bearer " + gitHubToken);
        httpPost.addHeader("Accept", "application/json");
        Object returnedObject = null;

        try {
            StringEntity entity = new StringEntity(queryObject.toString());
            httpPost.setEntity(entity);
            response = client.execute(httpPost);

        } catch (UnsupportedEncodingException e) {
            GraphQlApiCallerLogger.error("Encoding error occured before calling the github graphQL API", e);
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            GraphQlApiCallerLogger.error("Client protocol exception occurred when calling the github graphQL API", e);

            e.printStackTrace();
        } catch (IOException e) {
            GraphQlApiCallerLogger.error("IO Exception occured when calling the github graphQL API", e);

            e.printStackTrace();
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {

                stringBuilder.append(line);
            }

            String jsonText = stringBuilder.toString();
            Object json = new JSONTokener(jsonText).nextValue();     // gives an object http://stackoverflow.com/questions/14685777/how-to-check-if-response-from-server-is-jsonaobject-or-jsonarray

            if (json instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) json;
                returnedObject = jsonObject;
            } else if (json instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) json;
                returnedObject = jsonArray;
            }

            //            System.out.println(stringBuilder.toString());
        } catch (Exception e) {
            GraphQlApiCallerLogger.error("Exception occured when reading the response received from github graphQL API", e);
            e.printStackTrace();
        } finally {

            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return returnedObject;
    }
}
