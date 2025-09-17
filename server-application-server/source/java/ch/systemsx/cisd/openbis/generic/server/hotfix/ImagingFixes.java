/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.systemsx.cisd.openbis.generic.server.hotfix;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.create.PersonalAccessTokenCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.delete.PersonalAccessTokenDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.id.PersonalAccessTokenPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.create.PersonCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.fetchoptions.PersonFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.IPersonId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.create.RoleAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.id.RoleAssignmentTechId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class ImagingFixes
{
    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, ImagingFixes.class);

    private static final int TIMEOUT_COUNT = 60 * 60; //60 minutes
    private static final String PYTHON_PATH = Paths.get("..", "..", "..", "imaging", "1", "dss", "services", "imaging", "Python-3.10.16-linux-x64", "bin", "python3.10").toString();

    private static final String PYTHON_PATH_PROPERTY = "imaging.imaging-fixes-python3-path";
    private static final String URL_SYS_PROPERTY = "OPENBIS_FQDN";
    private static final String PERSON_ID = "admin";

    public static void registerExamples(String pathToDir, String pluginName) {

        Runnable registerOnSeparateThread = new Runnable()
        {
            @Override
            public void run()
            {
                int count = 0;
                // Wait until DSS starts up
                while(CommonServiceProvider.getDataStoreServerApi() == null) {
                    try {
                        operationLog.info("DSS not yet available! Waiting.");
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if(count == TIMEOUT_COUNT) {
                        operationLog.info("DSS was not available for 1 hour! Shutting down thread.");
                        break;
                    }
                    count++;
                }
                switch (pluginName) {
                    case "imaging-nanonis":
                        registerNanonisExamples(pathToDir);
                        break;
                    case "imaging-test":
                        registerTestExamples(pathToDir);
                    default:
                        break;
                }

            }
        };
        new Thread(registerOnSeparateThread).start();
    }


    private static void registerNanonisExamples(String pathToDir)
    {
        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        String token = api.loginAsSystem();

        //workaround to get url of openbis server
        String downloadUrl = CommonServiceProvider.tryToGetProperty("download-url");
        String openbisUrl = String.format("%s/openbis", downloadUrl);

        operationLog.info("Configured url: " + openbisUrl);

        String pythonPathProperty = CommonServiceProvider.tryToGetProperty(PYTHON_PATH_PROPERTY);
        String pythonPath = null;
        if(pythonPathProperty != null && !pythonPathProperty.trim().isEmpty()) {
            pythonPath = Paths.get(pathToDir, pythonPathProperty).toAbsolutePath().toString();
        } else {
            pythonPath = Paths.get(pathToDir, PYTHON_PATH).toAbsolutePath().toString();
        }
        operationLog.info("Configured python path: " + pythonPath);

        String examplePath =
                Paths.get(pathToDir, "..", "nanonis_example").toAbsolutePath().toString();
        String scriptPath =
                Paths.get(examplePath, "nanonis_importer.py").toAbsolutePath().toString();
        String dataPath = Paths.get(examplePath, "data").toAbsolutePath().toString();

        PersonalAccessTokenPermId patToken = null;
        try
        {
            patToken = getOrCreatePersonPAT(token, "import_imaging_nanonis_examples");
            runImporter(pythonPath, scriptPath, openbisUrl, dataPath, patToken.getPermId());
        } finally
        {
            if (patToken != null)
            {
                api.deletePersonalAccessTokens(token, Arrays.asList(patToken),
                        new PersonalAccessTokenDeletionOptions().setReason("imaging_nanonis_import_cleanup"));
            }
        }
    }

    private static PersonalAccessTokenPermId getOrCreatePersonPAT(String sessionToken, String sessionName) {
        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        IPersonId personId = new PersonPermId(PERSON_ID);
        PersonalAccessTokenPermId patToken = null;

        Map<IPersonId, Person> personMap = api.getPersons(sessionToken, Arrays.asList(personId), new PersonFetchOptions());
        if(!personMap.containsKey(personId)) {
            PersonCreation pc = new PersonCreation();
            pc.setUserId(PERSON_ID);
            PersonPermId importer = api.createPersons(sessionToken, Arrays.asList(pc)).get(0);

            RoleAssignmentCreation roleC = new RoleAssignmentCreation();
            roleC.setRole(Role.ADMIN);
            roleC.setUserId(new PersonPermId(PERSON_ID));
            RoleAssignmentTechId raId =
                    api.createRoleAssignments(sessionToken, Arrays.asList(roleC)).get(0);
        }

        PersonalAccessTokenCreation patc = new PersonalAccessTokenCreation();
        patc.setOwnerId(personId);
        patc.setSessionName(sessionName);
        patc.setValidFromDate(new Date(System.currentTimeMillis() - 10 * 60 * 1000L));
        patc.setValidToDate(new Date(System.currentTimeMillis() + 10 * 60 * 1000L));
        patToken =
                api.createPersonalAccessTokens(sessionToken, Arrays.asList(patc)).get(0);
        return patToken;

    }

    private static void registerTestExamples(String pathToDir) {
        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        String token = api.loginAsSystem();

        //workaround to get url of openbis server
        String downloadUrl = CommonServiceProvider.tryToGetProperty("download-url");
        String openbisUrl = String.format("%s/openbis", downloadUrl);

        String pythonPathProperty = CommonServiceProvider.tryToGetProperty(PYTHON_PATH_PROPERTY);
        String pythonPath = null;
        if(pythonPathProperty != null && !pythonPathProperty.trim().isEmpty()) {
            pythonPath = Paths.get(pathToDir, pythonPathProperty).toAbsolutePath().toString();
        } else {
            pythonPath = Paths.get(pathToDir, PYTHON_PATH).toAbsolutePath().toString();
        }
        operationLog.info("Configured python path: " + pythonPath);

        String examplePath = Paths.get(pathToDir, "..", "imaging_test_example").toAbsolutePath().toString();
        String scriptPath = Paths.get(examplePath, "importer.py").toAbsolutePath().toString();
        String dataPath = Paths.get(examplePath, "data").toAbsolutePath().toString();

        PersonalAccessTokenPermId patToken = null;
        try
        {
            patToken = getOrCreatePersonPAT(token, "import_imaging_test_examples");
            runImporter(pythonPath, scriptPath, openbisUrl, dataPath, patToken.getPermId());
        } finally
        {
            if (patToken != null)
            {
                api.deletePersonalAccessTokens(token, Arrays.asList(patToken),
                        new PersonalAccessTokenDeletionOptions().setReason("imaging_test_import_cleanup"));
            }
        }
    }

    private static void runImporter(String pythonPath, String scriptPath, String openbisUrl, String dataPath, String token) {
        ProcessBuilder processBuilder = new ProcessBuilder(pythonPath,
                scriptPath, openbisUrl, dataPath, token);
        processBuilder.redirectErrorStream(false);

        String fullOutput;
        try
        {
            Process process = processBuilder.start();
            fullOutput =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0)
            {
                String error =
                        new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new UserFailureException("Script evaluation failed: " + error);
            }
        } catch (IOException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
