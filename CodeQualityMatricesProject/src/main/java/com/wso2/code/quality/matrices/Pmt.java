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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class is used for obtaining the commit hashes that belongs to the given patch
 *
 * @since 1.0.0
 */

public class Pmt {
    private String[] patchInformation_svnRevisionpublic;
    private final Logger logger = Logger.getLogger(Pmt.class.getName());

    private static final String COMMITS_IN_PATCH_IDENTIFIER = "patchInformation_svnRevisionpublic";  //key used to identify the commits in a patch from JSON response received from PMT
    private static final String KEY_STRING = "name";
    private static final String VALUE_STRING = "value";

    /**
     * getting the commit IDs from the received json response
     *
     * @param jsonArray jsonarray containing the output received from WSO2 PMT for the given patch
     * @return a copy of the array containing the commit hashes that belongs to the given patch
     */
    public String[] getPublicGitCommitHashes(JSONArray jsonArray) {

        List<String> listOfCommits = getCommitHashesToList(jsonArray);
        logger.info(" The commits hashes obtained from WSO2 PMT are successfully saved to an Array list");
        patchInformation_svnRevisionpublic = listOfCommits.toArray(new String[listOfCommits.size()]);
        logger.info(" The commits hashes obtained from WSO2 PMT are successfully saved to an array");
        System.out.println("The commit Ids are");
        //            for printing all the commits ID associated with a patch
        IntStream.range(0, patchInformation_svnRevisionpublic.length).mapToObj(i -> patchInformation_svnRevisionpublic[i]).forEach(System.out::println);
        System.out.println();

        //to prevent from internaal representation by returning referecnce to mutable object
        String clonedPatchInformation_svnRevisionpublic[] = patchInformation_svnRevisionpublic.clone();
        return clonedPatchInformation_svnRevisionpublic;
    }

    /**
     * This method returns the commit hashes belongs to the given patch in as a list of Strings
     *
     * @param array jsonarray containing the output received from WSO2 PMT for the given patch
     * @return a List containing the commit hashes that belongs to the given patch
     */

    public List<String> getCommitHashesToList(JSONArray array) {
        return arrayToStream(array)
                .map(JSONObject.class::cast)
                .filter(o -> o.get(KEY_STRING).equals(COMMITS_IN_PATCH_IDENTIFIER))
                .findFirst()
                .map(o -> (JSONArray) o.get(VALUE_STRING))
                .map(Pmt::arrayToStream)
                .map(commits ->
                        commits.map(Object::toString)
                                .map(String::trim)
                                .collect(Collectors.toList())
                )
                .orElseGet(Collections::emptyList);
    }

    /**
     * This method is used to obtain a sequential stream
     * @param array JSON array that a sequential stream should be obtained from
     * @return a sequential stream created from the supplied JSON Array
     */
    public static Stream<Object> arrayToStream(JSONArray array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

}
