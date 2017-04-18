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

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

/**
 * A class used to test saveRepoNames,savePrNumberAndRepoName and saveReviewers methods of the GithubResponses class
 *
 * @since 1.0.0
 */
public class GithubResponsesTest {
    private static String githubToken;
    private static GithubApiCaller githubApiCaller;

    @BeforeClass
    public static void setupTheEnvironment() {
        githubToken = new Token().getGithubToken();
        githubApiCaller = new GithubApiCaller();
    }

    @Test
    public void testSaveRepoNames() throws CodeQualityMetricsException {
        Map<String, List<String>> commitHashWithRepoNames = new HashMap<>();
        commitHashWithRepoNames.put("eaa45529cbabc5f30a2ffaa4781821ad0a5223ab",
                Collections.singletonList("wso2/carbon-apimgt"));
        commitHashWithRepoNames.put("2b1d973d089ebc3af3b9e7b893f48cf905758cf4",
                Collections.singletonList("wso2/carbon-apimgt"));
        commitHashWithRepoNames.put("e3c3457149b109178d510aac965d5a85cc465aa0",
                Collections.singletonList("wso2/wso2-axis2-transports"));
        for (Map.Entry<String, List<String>> entry : commitHashWithRepoNames.entrySet()) {
            String commitHash = entry.getKey();
            String jsonText = githubApiCaller.callSearchCommitApi(commitHash, githubToken);
            ChangesFinder changesFinder = new ChangesFinder();
            List<String> repoLocations = changesFinder.saveRepoNames(jsonText);
            assertEquals("List of RepoLocations obtained must be same", entry.getValue(), repoLocations);
        }
    }

    @Test
    public void testSavePrNumberAndRepoName() throws CodeQualityMetricsException {
        String jsonText = githubApiCaller.callSearchIssueApi
                ("0015c02145c8ec6d3bba433f2fb5e850e1d25846", githubToken);
        ReviewAnalyser reviewAnalyser = new ReviewAnalyser();
        Map<String, Set<Integer>> actualPrNoWithRepoName = reviewAnalyser.savePrNumberAndRepoName(jsonText);
        Map<String, Set<Integer>> expectedPrNoWithRepoName = new HashMap<>();
        Set<Integer> expectedPrSet = new HashSet<>();
        expectedPrSet.add(656);
        expectedPrSet.add(657);
        expectedPrNoWithRepoName.put("wso2/carbon-apimgt", expectedPrSet);
        assertThat(actualPrNoWithRepoName, is(expectedPrNoWithRepoName));

    }

    @Test
    public void testSaveReviewers() {
        ReviewAnalyser reviewAnalyser = new ReviewAnalyser();
        Map<String, Set<Integer>> prNoWithRepoName1 = new HashMap<>();
        Set<Integer> prNumberSet = new HashSet<>();
        prNumberSet.add(656);
        prNumberSet.add(657);
        prNoWithRepoName1.put("wso2/carbon-apimgt", prNumberSet);
        List<String> expectedApprovedReviewers1 = Collections.emptyList();
        List<String> expectedCommentedReviewers1 = Collections.emptyList();
        reviewAnalyser.saveReviewers(prNoWithRepoName1, githubToken);
        assertThat("List of approved users ", reviewAnalyser.approvedReviewers,
                containsInAnyOrder(expectedApprovedReviewers1.toArray()));
        assertThat("List of commented users ", reviewAnalyser.commentedReviewers,
                containsInAnyOrder(expectedCommentedReviewers1.toArray()));

        reviewAnalyser.approvedReviewers.clear();
        reviewAnalyser.commentedReviewers.clear();
        Map<String, Set<Integer>> prNoWithRepoName2 = new HashMap<>();
        Set<Integer> prNumberSet2 = new HashSet<>();
        prNumberSet2.add(885);
        prNoWithRepoName2.put("wso2/product-is", prNumberSet2);
        List<String> expectedApprovedReviewers2 = Collections.singletonList("darshanasbg");
        List<String> expectedCommentedReviewers2 = Collections.singletonList("isharak");
        reviewAnalyser.saveReviewers(prNoWithRepoName2, githubToken);
        assertThat("List of approved users ", reviewAnalyser.approvedReviewers,
                containsInAnyOrder(expectedApprovedReviewers2.toArray()));
        assertThat("List of commented users ", reviewAnalyser.commentedReviewers,
                containsInAnyOrder(expectedCommentedReviewers2.toArray()));
    }
}
