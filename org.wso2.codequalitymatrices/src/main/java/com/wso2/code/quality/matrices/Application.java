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

/**
 * This is having the main method of this application
 * PMT Access token, patch id and github access token
 * should be passed in order as command line arguments when running the application.
 */
public class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        if (args.length == 3) {
            String pmtToken = args[0];
            String patchId = args[1];
            String gitHubToken = args[2];
            CodeQualityMatricesExecutor codeQualityMatricesExecutor = new CodeQualityMatricesExecutor(pmtToken, patchId,
                    gitHubToken);
            codeQualityMatricesExecutor.execute();
        } else {
            logger.debug("Command line arguments were not given correctly to start the execution");
            logger.debug("Please enter PMT Access token, patch id and github access token in order as command " +
                    "line arguments to initiate the program");
        }
    }
}
