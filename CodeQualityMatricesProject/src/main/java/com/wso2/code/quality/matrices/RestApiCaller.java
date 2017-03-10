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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * This class is used to call the REST API of both WSO2 PMT and github.com
 */

public class RestApiCaller {
    static Logger restApiCallerLogger= Logger.getLogger(RestApiCaller.class.getName());
    /**
     * calling the relevant API and saving the output to a file
     *
     * @param URL                 url of the REST API to be called
     * @param token               either the WSO2 PMT access token or giihub.com access token
     * @param requireCommitHeader should be true for accessing the github commit search API and false otherwise
     * @param requireReviewHeader should be true for accessing the github review API or false otherwise
     */

    public Object callingTheAPI(String URL, String token, boolean requireCommitHeader, boolean requireReviewHeader) {

        BufferedReader bufferedReader = null;
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse httpResponse = null;
        Object returnedObject = null;

        try {
            httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(URL);

            if (token != null) {

                httpGet.addHeader("Authorization", "Bearer " + token);        // passing the token for the API call
            }

            //as the accept header is needed for the review API since it is still in preview mode   
            if (requireReviewHeader) {
                httpGet.addHeader("Accept", "application/vnd.github.black-cat-preview+json");
            }

            //as the accept header is needed for accessing commit search API which is still in preview mode
            if (requireCommitHeader) {
                httpGet.addHeader("Accept", "application/vnd.github.cloak-preview");
            }

            httpResponse = httpclient.execute(httpGet);
            int responseCode = httpResponse.getStatusLine().getStatusCode();     // to get the response code

            switch (responseCode) {
                case 200:
                    //success
                    bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    // creating a JSON object from the response
                    String JSONText = stringBuilder.toString();
                    Object json = new JSONTokener(JSONText).nextValue();    // gives an object http://stackoverflow.com/questions/14685777/how-to-check-if-response-from-server-is-jsonaobject-or-jsonarray

                    if (json instanceof JSONObject) {
                        JSONObject jsonObject = (JSONObject) json;
                        returnedObject = jsonObject;
                    } else if (json instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray) json;
                        returnedObject = jsonArray;
                    }
                    restApiCallerLogger.info("JSON response is passed after calling the given REST API");
                    break;
                case 401:
                    // to handle Response code 401: Unauthorized
                    System.err.print("Response code 401 : Git hub access token is invalid");
                    try {

                        Thread.sleep(100);
                        runningTheAppAgain();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case 403:
                    // to handle invalid credentials
                    System.err.println("Response Code:403 Invalid Credentials, insert a correct token");
                    try {

                        Thread.sleep(100);
                        runningTheAppAgain();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    break;
                case 404:
                    // to handle invalid patch
                    System.err.println("Reponse Code 404: Patch not found, enter a valid patch");
                    try {
                        Thread.sleep(100);
                        runningTheAppAgain();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    break;
                default:
                    returnedObject = null;
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return returnedObject;
    }

    /**
     * this method calls the main method again when incorrect inputs are supplied
     */
    public void runningTheAppAgain() {
        try {
            MainClass.main(null);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}



