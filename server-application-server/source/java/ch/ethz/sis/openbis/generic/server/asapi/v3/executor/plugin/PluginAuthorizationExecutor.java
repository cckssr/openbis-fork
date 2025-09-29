/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.plugin;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.IPluginId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import ch.systemsx.cisd.openbis.generic.server.authorization.annotation.Capability;
import ch.systemsx.cisd.openbis.generic.server.authorization.annotation.RolesAllowed;
import ch.systemsx.cisd.openbis.generic.server.codeplugin.CodePluginsConfiguration;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.RoleWithHierarchy;
import ch.systemsx.cisd.openbis.generic.shared.dto.ScriptPE;

/**
 * @author pkupczyk
 */
@Component
public class PluginAuthorizationExecutor implements IPluginAuthorizationExecutor
{

    @Resource(name = ExposablePropertyPlaceholderConfigurer.PROPERTY_CONFIGURER_BEAN_NAME)
    private ExposablePropertyPlaceholderConfigurer configurer;

    private CodePluginsConfiguration codePluginsConfiguration;

    @PostConstruct
    private void init()
    {
        codePluginsConfiguration = new CodePluginsConfiguration(configurer.getResolvedProps());
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.PROJECT_OBSERVER, RoleWithHierarchy.SPACE_ETL_SERVER })
    @Capability("GET_PLUGIN")
    public void canGet(IOperationContext context)
    {
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.PROJECT_OBSERVER, RoleWithHierarchy.SPACE_ETL_SERVER })
    @Capability("SEARCH_PLUGIN")
    public void canSearch(IOperationContext context)
    {
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("CREATE_PLUGIN")
    public void canCreate(IOperationContext context)
    {
        checkAllowedUser(context);
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("UPDATE_PLUGIN")
    public void canUpdate(IOperationContext context, IPluginId id, ScriptPE entity)
    {
        checkAllowedUser(context);
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("DELETE_PLUGIN")
    public void canDelete(IOperationContext context, IPluginId entityId, ScriptPE entity)
    {
        checkAllowedUser(context);
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("EVALUATE_PLUGIN")
    public void canEvaluate(IOperationContext context)
    {
        checkEnabled(context);
        checkAllowedUser(context);
    }

    private void checkEnabled(IOperationContext context)
    {
        codePluginsConfiguration.checkEnabled();
    }

    private void checkAllowedUser(IOperationContext context)
    {
        codePluginsConfiguration.checkAllowedUser(context.getSession().getUserName());
    }

}
