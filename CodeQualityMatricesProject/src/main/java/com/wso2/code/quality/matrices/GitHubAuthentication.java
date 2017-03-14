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


import com.sun.org.apache.xpath.internal.SourceTree;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class GitHubAuthentication {
    public void gettingGithubClient(){
        GitHubClient client= new GitHubClient();
        client.setCredentials("kasunsiyambalapitiya","resident0507");

        CommitService commitService= new CommitService(client);
//        commitService.getCommit("product-is","fdafewafd");

        RepositoryService repositoryService= new RepositoryService(client);
        try {
            Repository repository=repositoryService.getRepository("wso2","wso2-axis2-transports");

            String id=repository.generateId();

            IRepositoryIdProvider iRepositoryIdProvider = () -> id;
            RepositoryCommit repositoryCommit=commitService.getCommit(iRepositoryIdProvider,"e3c3457149b109178d510aac965d5a85cc465aa0");
            List<CommitFile> filesChanged=repositoryCommit.getFiles();

            Iterator listIterator= filesChanged.iterator();
            while(listIterator.hasNext()){
                CommitFile commitFile= (CommitFile) listIterator.next();
                System.out.println(commitFile.getFilename());
                String patch=commitFile.getPatch();
                System.out.println(patch);


            }

//            filesChanged.stream().forEach();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
