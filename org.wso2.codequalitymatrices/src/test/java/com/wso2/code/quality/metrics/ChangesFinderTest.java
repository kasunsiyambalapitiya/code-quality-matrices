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

import com.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * A class used to test identifyChangedFile method of the ChangeFinder class
 *
 * @since 1.0.0
 */
public class ChangesFinderTest {
    private static final Logger logger = Logger.getLogger(ChangesFinderTest.class);

    private ChangesFinder changesFinder = new ChangesFinder();

    @Test
    public void testIdentifyDeletedLines() {
        String patchString;
        Set<Integer> actualDeltedLinesForPatchString;
        patchString = readFile("patchStrings/patchString1.txt");
        actualDeltedLinesForPatchString = changesFinder.identifyDeletedLines(patchString);
        Set<Integer> expectedDeltedLinesForPatchString1 = new HashSet<>();
        expectedDeltedLinesForPatchString1.add(822);
        expectedDeltedLinesForPatchString1.add(823);
        expectedDeltedLinesForPatchString1.add(824);
        expectedDeltedLinesForPatchString1.add(907);
        expectedDeltedLinesForPatchString1.add(908);
        expectedDeltedLinesForPatchString1.add(909);
        expectedDeltedLinesForPatchString1.add(1081);
        expectedDeltedLinesForPatchString1.add(1082);
        expectedDeltedLinesForPatchString1.add(1083);
        expectedDeltedLinesForPatchString1.add(1087);
        expectedDeltedLinesForPatchString1.add(1094);
        expectedDeltedLinesForPatchString1.add(1143);
        expectedDeltedLinesForPatchString1.add(1144);
        expectedDeltedLinesForPatchString1.add(1145);
        expectedDeltedLinesForPatchString1.add(1146);
        expectedDeltedLinesForPatchString1.add(1147);
        expectedDeltedLinesForPatchString1.add(1148);
        expectedDeltedLinesForPatchString1.add(1149);
        expectedDeltedLinesForPatchString1.add(1150);
        expectedDeltedLinesForPatchString1.add(1151);
        expectedDeltedLinesForPatchString1.add(1152);
        expectedDeltedLinesForPatchString1.add(1153);
        expectedDeltedLinesForPatchString1.add(1165);

        assertThat(actualDeltedLinesForPatchString.size(), is(expectedDeltedLinesForPatchString1.size()));
        assertThat(actualDeltedLinesForPatchString, is(expectedDeltedLinesForPatchString1));

        patchString = readFile("patchStrings/patchString2.txt");
        actualDeltedLinesForPatchString = changesFinder.identifyDeletedLines(patchString);
        assertTrue(actualDeltedLinesForPatchString.isEmpty());
    }

    /**
     * This is used to get the contents of the txt file as a single String
     *
     * @param path location path to the relevant txt file
     * @return single String containing all the contents of the given file
     */
    private String readFile(String path) {
        String result = "";
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            result = IOUtils.toString(classLoader.getResourceAsStream(path));
        } catch (IOException e) {
            logger.error(e.getMessage(), e.getCause());
        }
        return result;
    }

    @Test
    public void testObtainRepoNamesForCommitHashes() throws CodeQualityMetricsException {
        Token token = new Token();
        List<String> commitHash = new ArrayList<>();
        commitHash.add("ad0debb15f1abac020b8ba69066ae4ebec782bdc");
        Set<String> actualAuthorCommits = changesFinder.obtainRepoNamesForCommitHashes(token.getGithubToken(),
                commitHash);
        Set<String> expectedAuthorCommits = new HashSet<>();
        expectedAuthorCommits.add("90fec04e4ac05281612de8d445c5767c26433b0d");
        assertThat(actualAuthorCommits.size(), is(expectedAuthorCommits.size()));
        assertThat(actualAuthorCommits, is(expectedAuthorCommits));
    }

    @Test
    public void testObtainRepoNamesForCommitHashesForAuthorNames() throws CodeQualityMetricsException {
        Token token = new Token();
        List<String> commitHash = new ArrayList<>();
        commitHash.add("bba8ce79cd3373445e21dd12deffae1a7b48dca9");
        commitHash.add("1c0e28ca181a08398efbc8ba8e984d8800e23c95");
        commitHash.add("a8ddc56575ede78c6a1882df20789bb2cc04022c");
        changesFinder.obtainRepoNamesForCommitHashes(token.getGithubToken(),
                commitHash);
        Set<String> expectedAuthorName = new HashSet<>();
        expectedAuthorName.add("ruchiraw");
        assertThat(changesFinder.authorNames.size(), is(expectedAuthorName.size()));
        assertThat(changesFinder.authorNames, is(expectedAuthorName));
    }

    @Test
    public void testObtainRepoNamesForCommitHashesForAuthorNames2() throws CodeQualityMetricsException {
        Token token = new Token();
        List<String> commitHash = new ArrayList<>();
        commitHash.add("2b1d973d089ebc3af3b9e7b893f48cf905758cf4");
        commitHash.add("eaa45529cbabc5f30a2ffaa4781821ad0a5223ab");
        changesFinder.obtainRepoNamesForCommitHashes(token.getGithubToken(),
                commitHash);
        Set<String> expectedAuthorName = new HashSet<>();
        expectedAuthorName.add("Chamila");
        expectedAuthorName.add("lalaji");
        expectedAuthorName.add("Amila De Silva");
        expectedAuthorName.add("Lakmali");
        assertThat(changesFinder.authorNames.size(), is(expectedAuthorName.size()));
        assertThat(changesFinder.authorNames, is(expectedAuthorName));
    }
}
