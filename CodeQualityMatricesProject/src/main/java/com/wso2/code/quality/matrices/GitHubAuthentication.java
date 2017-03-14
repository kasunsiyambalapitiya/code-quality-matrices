package com.wso2.code.quality.matrices;/*
*  Copyright (c) ${date}, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/


import org.apache.commons.lang3.StringUtils;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */

public class GitHubAuthentication {
    protected GitHubClient gitHubClient = null;
    protected CommitService commitService = null;
    protected RepositoryService repositoryService = null;
    protected ArrayList<String> fileNames = new ArrayList<String>();
    protected ArrayList<String> patchString = new ArrayList<String>();


    GitHubAuthentication(String githubToken) {
        gitHubClient = new GitHubClient();
        gitHubClient.setOAuth2Token(githubToken);
        commitService = new CommitService(gitHubClient);
        repositoryService = new RepositoryService(gitHubClient);


    }

    public Map<String, ArrayList<String>> gettingFilesChanged(String repositoryName, String commitHash) {
        Map<String, ArrayList<String>> mapWithFileNamesAndPatches = new HashMap<>();

        try {
//            Repository repository = repositoryService.getRepository("wso2", "carbon-apimgt");
//            String id = repository.generateId();

            IRepositoryIdProvider iRepositoryIdProvider = () -> repositoryName;
            RepositoryCommit repositoryCommit = commitService.getCommit(iRepositoryIdProvider, commitHash);
            List<CommitFile> filesChanged = repositoryCommit.getFiles();

            Iterator listIterator = filesChanged.iterator();
            while (listIterator.hasNext()) {
                CommitFile commitFile = (CommitFile) listIterator.next();
                fileNames.add(commitFile.getFilename());
                patchString.add(commitFile.getPatch());

            }
//            System.out.println(fileNames);
            mapWithFileNamesAndPatches.put("fileNames", fileNames);
            mapWithFileNamesAndPatches.put("patchString", patchString);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapWithFileNamesAndPatches;

    }

}
