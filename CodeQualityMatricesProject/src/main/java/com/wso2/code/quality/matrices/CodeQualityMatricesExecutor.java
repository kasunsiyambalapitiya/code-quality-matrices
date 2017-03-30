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
 * This is used for creating executing the program.
 *
 * @since 1.0.0
 */
public class CodeQualityMatricesExecutor {
    private final String pmtToken;
    private final String patchId;
    private final String gitHubToken;
    private static final String COMMITS_INSIDE_GIVEN_PATCH = "patchInformation_svnRevisionpublic";
    private final static Logger logger = Logger.getLogger(CodeQualityMatricesExecutor.class);

    /**
     * This create an instance of CodeQualityMatricesExecutor class.
     *
     * @param pmtToken    PMT Access Token
     * @param patchId     Patch ID
     * @param gitHubToken Github access token
     */
    public CodeQualityMatricesExecutor(String pmtToken, String patchId, String gitHubToken) {
        this.pmtToken = pmtToken;
        this.patchId = patchId;
        this.gitHubToken = gitHubToken;

    }

    /**
     * This is the entry point to this application.
     */
    public void execute() {
        try {
            List<String> commitHashes = findCommitHashesInPatch();
            ChangesFinder changesFinder = new ChangesFinder();
            Set<String> authorCommits = changesFinder.obtainRepoNamesForCommitHashes(gitHubToken, commitHashes);
            System.out.println("Author Commits" + authorCommits);

            ReviewAnalyser reviewAnalyser = new ReviewAnalyser();
            reviewAnalyser.findReviewers(authorCommits, gitHubToken);
            reviewAnalyser.printReviewUsers();

            logger.debug("The application executed successfully");

        } catch (CodeQualityMatricesException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }

    /**
     * This is used to filter out the commit hashes that belongs to the given patch
     *
     * @return List of commithashes contained in the given patch
     */
    private List<String> findCommitHashesInPatch() throws CodeQualityMatricesException {
        PmtApiCaller pmtApiCaller = new PmtApiCaller();
        String jsonText;
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
