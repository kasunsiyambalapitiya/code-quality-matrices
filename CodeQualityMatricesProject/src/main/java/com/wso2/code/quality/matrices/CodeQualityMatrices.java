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

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CodeQualityMatrices {
    private String pmtToken;
    private String patchId;
    private String gitHubToken;
    private static final String COMMITS_INSIDE_GIVEN_PATCH = "patchInformation_svnRevisionpublic";
    private final static Logger logger = Logger.getLogger(CodeQualityMatrices.class);

    /**
     * @param pmtToken
     * @param patchId
     * @param gitHubToken
     */
    public CodeQualityMatrices(String pmtToken, String patchId, String gitHubToken) {
        this.pmtToken = pmtToken;
        this.patchId = patchId;
        this.gitHubToken = gitHubToken;

    }

    /**
     *
     */
    public void execute() {
        try {
            ChangesFinder changesFinder = new ChangesFinder();
            List<String> commitHashes = findCommitHashesInPatch();
            Set<String> authorCommits = changesFinder.obtainRepoNamesForCommitHashes(gitHubToken, commitHashes);
            System.out.println("Author Commits"+authorCommits);


        } catch (CodeQualityMatricesException e) {
            logger.error(e.getMessage(), e);

        }


    }

    /**
     *
     */
    public List<String> findCommitHashesInPatch() throws CodeQualityMatricesException {
        PmtApiCaller pmtApiCaller = new PmtApiCaller();
        String jsonText = null;
        List<String> commitHashes = new ArrayList<>();

        try {
            jsonText = pmtApiCaller.callApi(pmtToken, patchId);
        } catch (CodeQualityMatricesException e) {
            throw new CodeQualityMatricesException("Error occurred while calling PMT API", e);
        }

        Gson gson = new Gson();

        if (jsonText != null) {
            List pmtResponse = gson.fromJson(jsonText, List.class);
            for (Object pmtEntry : pmtResponse) {
                if (pmtEntry instanceof Map) {
                    Map<String, List<String>> entryMap = (Map<String, List<String>>) pmtEntry;
                    if (COMMITS_INSIDE_GIVEN_PATCH.equals(entryMap.get("name"))) {
                        commitHashes = entryMap.get("value");
                    }
                }
            }
            System.out.println("The commit hashes are: ");
            System.out.println(commitHashes);
        } else {
            throw new CodeQualityMatricesException("The returned jsonText from PMT API is null");
        }

        return commitHashes;
    }
}
