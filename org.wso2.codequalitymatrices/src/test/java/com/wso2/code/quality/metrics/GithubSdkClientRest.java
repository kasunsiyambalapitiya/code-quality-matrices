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

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class GithubSdkClientRest {
    private static Map<String, List<String>> fileNamesAndPatches;

    @BeforeClass
    public static void setupTheEnvironment() throws CodeQualityMetricsException {
        String githubToken = "4e8e69986aefdbae2f5fe59d892cd3badf771191";
        SdkGitHubClient sdkGitHubClient = new SdkGitHubClient(githubToken);
        fileNamesAndPatches = sdkGitHubClient.getFilesChanged
                ("wso2/carbon-apimgt", "eaa45529cbabc5f30a2ffaa4781821ad0a5223ab");
    }

    @Test
    public void testGetFilesChanged() {
        List<String> fileChanged = new ArrayList<>();
        fileChanged.add("features/apimgt/org.wso2.carbon.apimgt.publisher.feature/src/main" +
                "/resources/publisher/site/themes/wso2/templates/sso/logout/template.jag");
        fileChanged.add("features/apimgt/org.wso2.carbon.apimgt.store" +
                ".feature/src/main/resources/store/site/themes/wso2" +
                "/templates/sso/logout/template.jag");
        fileChanged.add("components/sso-hostobject/org.wso2.carbon.hostobjects" +
                ".sso/src/main/java/org/wso2/carbon/hostobjects/" +
                "sso/internal/SessionInfo.java");
        fileChanged.add("components/sso-hostobject/org.wso2.carbon.hostobjects" +
                ".sso/src/main/java/org/wso2/carbon/hostobjects/" +
                "sso/SAMLSSORelyingPartyObject.java");
        List<String> fileNames = fileNamesAndPatches.get("fileNames");
        assertThat(fileNames, not(IsEmptyCollection.empty()));
        assertThat(fileNames, hasSize(fileChanged.size()));
        assertThat(fileNames, containsInAnyOrder(fileChanged.toArray()));
    }

    @Test
    public void testSaveRelaventEditLineNumbers() throws CodeQualityMetricsException {
        //testing  saveRelaventEditLineNumbers method
        List<List<String>> modifiedRanges = new ArrayList<>();
        List<String> listRange1 = Arrays.asList("45,50/45,53");
        List<String> listRange2 = Arrays.asList("44,49/44,52");
        List<String> listRange3 = Arrays.asList("819,827/819,829", "901,912/903,916", "1078,1098/1082,1107",
                "1140,1156/1149,1155", "1162,1168/1161,1166", "1276,1281/1274,1287", "1331,1336/1337,1367");
        List<String> listRange4 = Arrays.asList("16,26/16,29", "57,60/60,71");
        modifiedRanges.add(listRange1);
        modifiedRanges.add(listRange2);
        modifiedRanges.add(listRange3);
        modifiedRanges.add(listRange4);
        List<String> fileNames = fileNamesAndPatches.get("fileNames");
        List<String> patchString = fileNamesAndPatches.get("patchString");
        ChangesFinder changesFinder = new ChangesFinder();
        changesFinder.saveRelaventEditLineNumbers(fileNames, patchString);
        assertThat(changesFinder.changedLineRanges, not(IsEmptyCollection.empty()));
        assertThat(changesFinder.changedLineRanges, hasSize(modifiedRanges.size()));
        assertThat("Modified line ranges received from the method must be equal to the manually " +
                "modifiedRanges list", changesFinder.changedLineRanges, containsInAnyOrder(modifiedRanges.toArray()));
    }
}
