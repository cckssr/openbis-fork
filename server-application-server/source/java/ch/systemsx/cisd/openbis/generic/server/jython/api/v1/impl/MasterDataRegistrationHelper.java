/*
 * Copyright ETH 2019 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.server.jython.api.v1.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.ISessionWorkspaceProvider;

/**
 * Helper class to be used in initialize-master-data.py.
 *
 * @author Franz-Josef Elmer
 */
public class MasterDataRegistrationHelper {
    private static Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, MasterDataRegistrationHelper.class);

    private File masterDataFolder;

    public MasterDataRegistrationHelper(Collection<?> systemPaths) {
        for (Object systemPath : systemPaths) {
            if (systemPath != null) {
                String systemPathString = String.valueOf(systemPath);
                if (systemPathString.contains("core-plugins")) {
                    masterDataFolder = new File(new File(systemPathString), "master-data");
                    if (masterDataFolder.exists() == false) {
                        throw new IllegalArgumentException("Folder does not exist: " + masterDataFolder.getAbsolutePath());
                    }
                    if (masterDataFolder.isFile()) {
                        throw new IllegalArgumentException("Is not a folder but a file: " + masterDataFolder.getAbsolutePath());
                    }
                    operationLog.info("Master data folder: " + masterDataFolder.getAbsolutePath());
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Does not contain path to the core plugin: " + systemPaths);
    }

    public String[] uploadToAsSessionWorkspace(final String sessionToken, final String... relativeFilePaths) throws IOException
    {
        final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();
        final String uploadId = UUID.randomUUID().toString();
        final String[] destinations = new String[relativeFilePaths.length];

        for (int i = 0; i < relativeFilePaths.length; i++)
        {
            destinations[i] = uploadId + "/" + relativeFilePaths[i];
            sessionWorkspaceProvider.write(sessionToken, destinations[i], new FileInputStream(masterDataFolder.getCanonicalPath() + "/" + relativeFilePaths[i]));
        }

        return destinations;
    }

}
