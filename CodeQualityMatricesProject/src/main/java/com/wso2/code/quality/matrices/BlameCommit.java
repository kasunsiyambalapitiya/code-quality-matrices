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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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

/**
 * This class is used for getting the blame information on relevant lines changeed from the given patch
 */

public class BlameCommit extends RestApiCaller {

    private String urlForObtainingCommits, urlForGetingFilesChanged;
    protected ArrayList<String> fileNames = new ArrayList<String>();
    protected List<ArrayList<String>> lineRangesChanged = new ArrayList<ArrayList<String>>();      // for saving the line no that are changed
    JSONObject graphqlApiJsonObject = new JSONObject();
    Set<String> commitHashesOfTheParent;
    Set<String> authorNames = new HashSet<String>();    //as the authors are for all the commits that exists in the relevant patch
    protected Set<String> commitHashObtainedForPRReview = new HashSet<String>();  //  relevant commits in old file that need to find the PR Reviewers
    private String repoLocation[];

    private static final Logger BlameCommitLogger = Logger.getLogger(BlameCommit.class.getName());


    public String getUrlForSearchingCommits() {
        return urlForObtainingCommits;
    }

    public void setUrlForSearchingCommits(String commitHash) {
        this.urlForObtainingCommits = "https://api.github.com/search/commits?q=hash%3A" + commitHash;
    }

    public String getUrlForGetingFilesChanged() {
        return urlForGetingFilesChanged;
    }

    public void setUrlForGetingFilesChanged(String repoName, String commitHash) {
        this.urlForGetingFilesChanged = "http://api.github.com/repos/" + repoName + "/commits/" + commitHash;
    }

    /**
     * This method is used for obtaining the repositories that contain the relevant commits belongs to the given patch
     *
     * @param gitHubToken            Github token
     * @param commitsInTheGivenPatch Commits that belongs to the given patch
     * @param restApiCaller          Instance of the RestApiCaller class for accessing the REST APIs
     * @return a Set <String> containing the commit hashes that needs to be checked for reviewers
     */
    public Set obtainingRepoNamesForCommitHashes(String gitHubToken, String[] commitsInTheGivenPatch, RestApiCaller restApiCaller) {

        for (String commitHash : commitsInTheGivenPatch) {
            setUrlForSearchingCommits(commitHash);
            //calling the API calling method
            JSONObject jsonObject = (JSONObject) restApiCaller.callingTheAPI(getUrlForSearchingCommits(), gitHubToken, true, false);
            saveRepoNamesInAnArray(jsonObject, commitHash, gitHubToken);
        }
        return commitHashObtainedForPRReview;
    }

    /**
     * saving the  Repo Names in the array and calling to Get files content
     *
     * @param rootJsonObject JSON object containing the repositories which are having the current selected commit from the given patch
     * @param commitHash     the current selected commit hash
     * @param gitHubToken    github token for accessing the github REST API
     */

    public void saveRepoNamesInAnArray(JSONObject rootJsonObject, String commitHash, String gitHubToken) {
        JSONArray jsonArrayOfItems = (JSONArray) rootJsonObject.get("items");
        // setting the size of the repoLocationArray
        repoLocation = new String[jsonArrayOfItems.length()];
        for (int i = 0; i < jsonArrayOfItems.length(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArrayOfItems.get(i);
            JSONObject repositoryJsonObject = (JSONObject) jsonObject.get("repository");
            //adding the repo name to the array
            repoLocation[i] = (String) repositoryJsonObject.get("full_name");
        }
        BlameCommitLogger.info("Repo names having the given commit are successfully saved in an array");

        //        for running through the repoName Array
        for (int i = 0; i < repoLocation.length; i++) {
            if (StringUtils.contains(repoLocation[i], "wso2/")) {
                //clearing all the data in the current fileNames and lineRangesChanged arraylists for each repository
                fileNames.clear();
                lineRangesChanged.clear();
                //authorNames.clear();
                callingToGetFilesChanged(repoLocation[i], commitHash, gitHubToken);

                iteratingOver(repoLocation[i], commitHash, gitHubToken);
            }
        }
        // for printing the author names and commit hashes for a certain commit.
        System.out.println(authorNames);
        System.out.println(commitHashObtainedForPRReview);
    }

    /**
     * calling github single commit API to get files changed from the current selected commit in the current selected repository
     *
     * @param repoLocation current selected repository
     * @param commitHash   current selected commit hash
     * @param gitHubToken  github token for accessing github REST API
     */
    public void callingToGetFilesChanged(String repoLocation, String commitHash, String gitHubToken) {
        //        setting the URL for calling github single commit API
        setUrlForGetingFilesChanged(repoLocation, commitHash);
        JSONObject rootJsonObject = null;
        //saving the commit details for the commit hash on the relevant repository
        rootJsonObject = (JSONObject) callingTheAPI(getUrlForGetingFilesChanged(), gitHubToken, false, false);
        //calling savingRelaventFileNamesAndEditLineNumbers method to read the above saved json output
        savingRelaventFileNamesAndEditLineNumbers(rootJsonObject);
        BlameCommitLogger.info("Relevant file names and their line ranges which being affected by the given commit are saved successfully");
    }

    /**
     * saving relevant file names and their changed line ranges from the current selected commit in the current selected repository
     *
     * @param rootJsonObject JSONObject received from the calling the single commit API in github
     */
    public void savingRelaventFileNamesAndEditLineNumbers(JSONObject rootJsonObject) {
        JSONArray fileJsonArray = (JSONArray) rootJsonObject.get("files");
        // to save one file at a time
        for (int i = 0; i < fileJsonArray.length(); i++) {
            JSONObject tempJsonObject = (JSONObject) fileJsonArray.get(i);
            String fileName = (String) tempJsonObject.get("filename");
            //saving the file name in the filename arraylist
            fileNames.add(fileName);

            //filtering only the line ranges that are modified and saving to a string array
            String patch = (String) tempJsonObject.get("patch");
            String lineChanges[] = StringUtils.substringsBetween(patch, "@@ ", " @@");

            //filtering the lines that existed in the previous file, that exists in the new file and saving them in to the same array
            for (int j = 0; j < lineChanges.length; j++) {

                //@@ -22,7 +22,7 @@
                String tempString = lineChanges[j];
                String lineRangeInTheOldFileBeingModified = StringUtils.substringBetween(tempString, "-", " +");      // for taking the authors and commit hashes of the previous lines
                String lineRangeInTheNewFileResultedFromModification = StringUtils.substringAfter(tempString, "+");  // for taking the parent commit

                int intialLineNoInOldFile = Integer.parseInt(StringUtils.substringBefore(lineRangeInTheOldFileBeingModified, ","));
                int tempEndLineNoInOldFile = Integer.parseInt(StringUtils.substringAfter(lineRangeInTheOldFileBeingModified, ","));
                int endLineNoOfOldFile;
                if (intialLineNoInOldFile != 0) {
                    // to filterout the newly created files
                    endLineNoOfOldFile = intialLineNoInOldFile + (tempEndLineNoInOldFile - 1);
                } else {
                    endLineNoOfOldFile = tempEndLineNoInOldFile;
                }
                int intialLineNoInNewFile = Integer.parseInt(StringUtils.substringBefore(lineRangeInTheNewFileResultedFromModification, ","));
                int tempEndLineNoInNewFile = Integer.parseInt(StringUtils.substringAfter(lineRangeInTheNewFileResultedFromModification, ","));
                int endLineNoOfNewFile = intialLineNoInNewFile + (tempEndLineNoInNewFile - 1);

                // storing the line ranges that are being modified in the same array by replacing values
                lineChanges[j] = intialLineNoInOldFile + "," + endLineNoOfOldFile + "/" + intialLineNoInNewFile + "," + endLineNoOfNewFile;
            }
            ArrayList<String> tempArrayList = new ArrayList<>(Arrays.asList(lineChanges));
            //adding to the array list which keep track of the line ranges which are being changed to the main arrayList
            lineRangesChanged.add(tempArrayList);
        }
        System.out.println("done saving file names and their relevant modification line ranges");
        System.out.println(fileNames);
        System.out.println(lineRangesChanged + "\n");
    }

    /**
     * This method will iterate over the saved filenames and their relevant changed line ranges and calls the github graphQL API
     * for getting blame details for each of the files
     *
     * @param repoLocation current selected repository
     * @param commitHash   current selected repository
     * @param gitHubToken  github token for accessing github GraphQL API
     */
    public void iteratingOver(String repoLocation, String commitHash, String gitHubToken) {

        // filtering the owner and the repository name from the repoLocation
        String owner = StringUtils.substringBefore(repoLocation, "/");
        String repositoryName = StringUtils.substringAfter(repoLocation, "/");
        //        iterating over the fileNames arraylist for the given commit
        Iterator iteratorForFileNames = fileNames.iterator();

        while (iteratorForFileNames.hasNext()) {
            String fileName = (String) iteratorForFileNames.next();
            int index = fileNames.indexOf(fileName);
            // the relevant arraylist of changed lines for that file
            ArrayList<String> arrayListOfRelevantChangedLines = lineRangesChanged.get(index);
            commitHashesOfTheParent = new HashSet<>();   // for storing the parent commit hashes for all the line ranges of the relevant file
            graphqlApiJsonObject.put("query", "{repository(owner:\"" + owner + "\",name:\"" + repositoryName + "\"){object(expression:\"" + commitHash + "\"){ ... on Commit{blame(path:\"" + fileName + "\"){ranges{startingLine endingLine age commit{history(first: 2) { edges { node {  message url } } } author { name email } } } } } } } }");

            JSONObject rootJsonObject = null;
            try {
                //            calling the graphql API for getting blame information for the current file and saving it in a location.
                rootJsonObject = (JSONObject) callingGraphQl(graphqlApiJsonObject, gitHubToken);
            } catch (IOException e) {
                BlameCommitLogger.error("IO exception occurred when calling the github graphQL API ", e);
                e.printStackTrace();
            }
            //            reading the above saved output for the current selected file name
            readingTheBlameReceivedForAFile(rootJsonObject, arrayListOfRelevantChangedLines, false);

            // parent commit hashes are stored in the arraylist for the given file

            iteratingOverForFindingAuthors(owner, repositoryName, fileName, arrayListOfRelevantChangedLines, gitHubToken);
            BlameCommitLogger.info("Authors of the bug lines of code which are being fixed from the given patch are saved successfully to authorNames SET");
        }
    }

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
            BlameCommitLogger.error("Encoding error occured before calling the github graphQL API", e);
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            BlameCommitLogger.error("Client protocol exception occurred when calling the github graphQL API", e);

            e.printStackTrace();
        } catch (IOException e) {
            BlameCommitLogger.error("IO Exception occured when calling the github graphQL API", e);

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
            BlameCommitLogger.error("Exception occured when reading the response received from github graphQL API", e);
            e.printStackTrace();
        } finally {

            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        return returnedObject;
    }

    /**
     * Reading the blame received for a current selected file name and insert the parent commits of the changed lines,
     * relevant authors and the relevant commits hashes to look for the reviewers of those line ranges
     *
     * @param rootJsonObject                  JSONObject containing blame information for current selected file
     * @param arrayListOfRelevantChangedLines arraylist containing the changed line ranges of the current selected file
     * @param gettingPr                       should be true if running this method for finding the authors of buggy lines which are being fixed from  the patch
     */
    public void readingTheBlameReceivedForAFile(JSONObject rootJsonObject, ArrayList<String> arrayListOfRelevantChangedLines, boolean gettingPr) {

        //running a iterator for fileName arrayList to get the location of the above saved file
        JSONObject dataJSONObject = (JSONObject) rootJsonObject.get("data");
        JSONObject repositoryJSONObect = (JSONObject) dataJSONObject.get("repository");
        JSONObject objectJSONObject = (JSONObject) repositoryJSONObect.get("object");
        JSONObject blameJSONObject = (JSONObject) objectJSONObject.get("blame");
        JSONArray rangeJSONArray = (JSONArray) blameJSONObject.get("ranges");

        //getting the starting line no of the range of lines that are modified from the patch
        Iterator arrayListOfRelevantChangedLinesIterator = arrayListOfRelevantChangedLines.iterator();     // iterator for the array list inside the root arraylist

        while (arrayListOfRelevantChangedLinesIterator.hasNext()) {

            int startingLineNo;
            int endLineNo;

            String lineRanges = (String) arrayListOfRelevantChangedLinesIterator.next();

            String oldFileRange = StringUtils.substringBefore(lineRanges, "/");
            String newFileRange = StringUtils.substringAfter(lineRanges, "/");

            // need to skip the newly created files from taking the blame
            if (oldFileRange.equals("0,0")) {

                continue;

            }
            //non newly created files
            else {

                if (gettingPr) {
                    // need to consider the line range in the old file for finding authors and reviewers

                    startingLineNo = Integer.parseInt(StringUtils.substringBefore(oldFileRange, ","));
                    endLineNo = Integer.parseInt(StringUtils.substringAfter(oldFileRange, ","));

                } else {
                    // need to consider the line range in the new file resulted from applying the commit for finding parent commits

                    startingLineNo = Integer.parseInt(StringUtils.substringBefore(newFileRange, ","));
                    endLineNo = Integer.parseInt(StringUtils.substringAfter(newFileRange, ","));
                }

                // as it is required to create a new Map for finding the recent commit for each line range
                Map<Integer, ArrayList<Integer>> mapForStoringAgeAndIndex = new HashMap<Integer, ArrayList<Integer>>();

                //checking line by line by iterating the startinLineNo
                while (endLineNo >= startingLineNo) {
                    //running through the rangeJSONArray
                    for (int i = 0; i < rangeJSONArray.length(); i++) {
                        JSONObject rangeJSONObject = (JSONObject) rangeJSONArray.get(i);
                        int tempStartingLineNo = (int) rangeJSONObject.get("startingLine");
                        int tempEndingLineNo = (int) rangeJSONObject.get("endingLine");

                        //checking whether the line belongs to that line range group
                        if ((tempStartingLineNo <= startingLineNo) && (tempEndingLineNo >= startingLineNo)) {
                            // so the relevant startingLineNo belongs in this line range in other words in this JSONObject
                            if (!gettingPr) {
                                int age = (int) rangeJSONObject.get("age");
                                // storing the age field with relevant index of the JSONObject
                                mapForStoringAgeAndIndex.putIfAbsent(age, new ArrayList<Integer>());
                                if (!mapForStoringAgeAndIndex.get(age).contains(i)) {
                                    mapForStoringAgeAndIndex.get(age).add(i);   // adding if the index is not present in the array list for the relevant age
                                }

                            } else {
                                //for saving the author names of commiters
                                JSONObject commitJSONObject = (JSONObject) rangeJSONObject.get("commit");

                                JSONObject authorJSONObject = (JSONObject) commitJSONObject.get("author");
                                String nameOfTheAuthor = (String) authorJSONObject.get("name");
                                authorNames.add(nameOfTheAuthor);       // authors are added to the Set

                                String urlOfCommit = (String) commitJSONObject.get("url");
                                String commitHashForPRReview = StringUtils.substringAfter(urlOfCommit, "commit/");
                                commitHashObtainedForPRReview.add(commitHashForPRReview);
                            }
                            break;
                        } else {
                            continue;
                        }
                    }
                    startingLineNo++;   // to check for other line numbers
                }

                //for the above line range getting the lastest commit which modified the lines
                if (!gettingPr) {
                    //converting the map into a treeMap to get it ordered
                    TreeMap<Integer, ArrayList<Integer>> treeMap = new TreeMap<>(mapForStoringAgeAndIndex);
                    int minimumKeyOfMapForStoringAgeAndIndex = treeMap.firstKey(); // getting the minimum key
                    //                     getting the relevant JSONObject indexes which consists of the recent commit with in the relevant line range
                    ArrayList<Integer> indexesOfJsonObjectForRecentCommit = mapForStoringAgeAndIndex.get(minimumKeyOfMapForStoringAgeAndIndex);
                    Iterator indexesOfJsonObjectForRecentCommitIterator = indexesOfJsonObjectForRecentCommit.iterator();

                    while (indexesOfJsonObjectForRecentCommitIterator.hasNext()) {
                        int index = (int) indexesOfJsonObjectForRecentCommitIterator.next();
                        // this is the range where the code gets actually modified
                        JSONObject rangeJSONObject = (JSONObject) rangeJSONArray.get(index);
                        JSONObject commitJSONObject = (JSONObject) rangeJSONObject.get("commit");
                        JSONObject historyJSONObject = (JSONObject) commitJSONObject.get("history");
                        JSONArray edgesJSONArray = (JSONArray) historyJSONObject.get("edges");

                        //getting the second json object from the array as it contain the commit of the parent which modified the above line range
                        JSONObject edgeJSONObject = (JSONObject) edgesJSONArray.get(1);
                        JSONObject nodeJSONObject = (JSONObject) edgeJSONObject.get("node");
                        String urlOfTheParentCommit = (String) nodeJSONObject.get("url");       // this contain the URL of the parent commit
                        String commitHash = (String) StringUtils.substringAfter(urlOfTheParentCommit, "commit/");
                        commitHashesOfTheParent.add(commitHash);
                    }
                    BlameCommitLogger.info("Parent Commits hashes of the lines which are being fixed by the patch are saved to commitHashesOfTheParent SET successfully ");
                }
            }
        }
    }

    /**
     * Finding the authors of the commits
     *
     * @param owner                           owner of the repository
     * @param repositoryName                  repository name
     * @param fileName                        name of the file which is required to get blame details
     * @param arrayListOfRelevantChangedLines arraylist containing the changed line ranges of the current selected file
     * @param gitHubToken                     github token for accessing github GraphQL API
     */
    public void iteratingOverForFindingAuthors(String owner, String repositoryName, String fileName, ArrayList<String> arrayListOfRelevantChangedLines, String gitHubToken) {

        // calling the graphql api to get the blame details of the current file for the parent commits (That is found by filtering in the graqhql output)
        Iterator commitHashOfTheParentIterator = commitHashesOfTheParent.iterator();
        while (commitHashOfTheParentIterator.hasNext()) {
            String parentCommitHashForCallingGraphQl = (String) commitHashOfTheParentIterator.next();
            graphqlApiJsonObject.put("query", "{repository(owner:\"" + owner + "\",name:\"" + repositoryName + "\"){object(expression:\"" + parentCommitHashForCallingGraphQl + "\"){ ... on Commit{blame(path:\"" + fileName + "\"){ranges{startingLine endingLine age commit{ url author { name email } } } } } } } }");
            JSONObject rootJsonObject = null;
            try {
                rootJsonObject = (JSONObject) callingGraphQl(graphqlApiJsonObject, gitHubToken);
                readingTheBlameReceivedForAFile(rootJsonObject, arrayListOfRelevantChangedLines, true);
            } catch (IOException e) {
                BlameCommitLogger.error("IO Exception occured when calling the github graphQL API for finding the authors of the bug lines which are being fixed by the given patch", e);
                e.printStackTrace();
            }
        }
    }
}


