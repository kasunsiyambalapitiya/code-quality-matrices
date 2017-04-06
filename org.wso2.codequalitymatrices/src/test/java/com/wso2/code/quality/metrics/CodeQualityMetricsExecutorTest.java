/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package com.wso2.code.quality.metrics;


import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CodeQualityMetricsExecutorTest {
    private static final Logger logger = Logger.getLogger(CodeQualityMetricsExecutorTest.class);

    private String pmtToken = "tQU5vxzrGeBpLMQuwOsJW_fyYLYa";
    private String githubToken = "4e8e69986aefdbae2f5fe59d892cd3badf771191";
    private final GithubApiCaller githubApiCaller = new GithubApiCaller();

    private Set authorCommits;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testUsingTempFolder() throws IOException {
        File createdFile = folder.newFile("myfilefile.txt");
        assertTrue(createdFile.canRead());
    }

    @Test
    public void testFindCommitHashesInPatch() throws CodeQualityMetricsException {
        Map<String, List<String>> patchesAndCommits = new HashMap<>();
        patchesAndCommits.put("WSO2-CARBON-PATCH-4.4.0-0680", Arrays.asList("eaa45529cbabc5f30a2ffaa4781821ad0a5223ab"
                , "2b1d973d089ebc3af3b9e7b893f48cf905758cf4"));
        patchesAndCommits.put("WSO2-CARBON-PATCH-4.4.0-0682", Arrays.asList("e3c3457149b109178d510aac965d5a85cc465aa0")
        );
        patchesAndCommits.put("WSO2-CARBON-PATCH-4.4.0-0692", Arrays.asList
                ("67a60e081c8e0fe01d087f60cd9b629bcea172ae"));
        for (Map.Entry<String, List<String>> map : patchesAndCommits.entrySet()) {
            CodeQualityMetricsExecutor codeQualityMetricsExecutor = new CodeQualityMetricsExecutor(pmtToken,
                    map.getKey(), githubToken);
            List commitHashes = codeQualityMetricsExecutor.findCommitHashesInPatch();
            assertEquals("Must match with the relevant commit list", map.getValue(), commitHashes);
        }
    }
}
