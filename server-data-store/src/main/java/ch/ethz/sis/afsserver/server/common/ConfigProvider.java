/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.server.common;

import java.io.File;

import ch.ethz.sis.afsserver.server.observer.impl.OpenBISUtils;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;

/**
 * The default implementation of {@link IConfigProvider}.
 * <p>
 * Currently it is a simple wrapper around DssPropertyParametersUtil, but in the future we might consider replacing it.
 *
 * @author Kaloyan Enimanev
 */
public class ConfigProvider implements IConfigProvider
{
    private final Configuration configuration;

    public ConfigProvider(Configuration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public File getStoreRoot()
    {
        return new File(AtomicFileSystemServerParameterUtil.getStorageRoot(configuration));
    }

    @Override
    public String getDataStoreCode()
    {
        return OpenBISUtils.AFS_DATA_STORE_CODE;
    }

}
