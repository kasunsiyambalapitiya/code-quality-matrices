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
import org.json.JSONObject;

/**
 * This class is used for obtaining the commit hashes that belongs to the given patch
 */

public class Pmt {
    private String[] patchInformation_svnRevisionpublic;
    private final Logger logger = Logger.getLogger(Pmt.class.getName());


    /**
     * getting the commit IDs from the received json response
     *
     * @param jsonArray jsonarray containing the output received from WSO2 PMT for the given patch
     * @return a copy of the array containing the commit hashes that belongs to the given patch
     */
    public String[] getThePublicGitCommitId(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String tempName = (String) jsonObject.get("name");
            if (tempName.equals("patchInformation_svnRevisionpublic")) {
                JSONArray tempCommitsJSONArray = (JSONArray) jsonObject.get("value");
                //initializing the patchInformation_svnRevisionpublic array
                patchInformation_svnRevisionpublic = new String[tempCommitsJSONArray.length()];
                for (int j = 0; j < tempCommitsJSONArray.length(); j++) {
                    patchInformation_svnRevisionpublic[j] = ((String) tempCommitsJSONArray.get(j)).trim();     // for ommiting the white spaces at the begingin and end of the commits
                }

                logger.info(" The commits hashes obtained from WSO2 PMT are successfully saved to an array");

                System.out.println("The commit Ids are");
                //            for printing all the commits ID associated with a patch
                for (String tmp : patchInformation_svnRevisionpublic) {
                    System.out.println(tmp);
                }
                System.out.println();
                break;
            }
        }
        //to prevent from internaal representation by returning referecnce to mutable object
        String clonedPatchInformation_svnRevisionpublic[] = patchInformation_svnRevisionpublic.clone();
        return clonedPatchInformation_svnRevisionpublic;
    }
}
