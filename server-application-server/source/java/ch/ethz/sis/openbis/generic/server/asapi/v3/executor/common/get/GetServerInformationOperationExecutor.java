/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.get;

import java.io.StringWriter;
import java.util.*;

import javax.annotation.Resource;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.RoleWithHierarchy;
import ch.systemsx.cisd.openbis.generic.shared.dto.RoleAssignmentPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.get.GetServerInformationOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.get.GetServerInformationOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.get.GetServerPublicInformationOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.get.GetServerPublicInformationOperationResult;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.OperationExecutor;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import ch.systemsx.cisd.openbis.BuildAndEnvironmentInfo;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.pat.IPersonalAccessTokenConfig;
import ch.systemsx.cisd.openbis.generic.shared.Constants;
import ch.systemsx.cisd.openbis.generic.shared.pat.PersonalAccessTokenConstants;

/**
 * @author Franz-Josef Elmer
 */
@Component
public class GetServerInformationOperationExecutor
        extends OperationExecutor<GetServerInformationOperation, GetServerInformationOperationResult>
        implements IGetServerInformationOperationExecutor
{
    @Autowired
    private IApplicationServerInternalApi server;

    @Resource(name = ExposablePropertyPlaceholderConfigurer.PROPERTY_CONFIGURER_BEAN_NAME)
    private ExposablePropertyPlaceholderConfigurer configurer;

    @Autowired
    private IGetServerPublicInformationOperationExecutor getPublicInformationExecutor;

    @Autowired
    private IPersonalAccessTokenConfig personalAccessTokenConfig;

    @Override
    protected Class<? extends GetServerInformationOperation> getOperationClass()
    {
        return GetServerInformationOperation.class;
    }

    private String servicePropertiesString;
    private long lastServicePropertiesUpdate;
    private static final long FIVE_MINUTES = 1000L * 60L * 5L;

    @Override
    protected GetServerInformationOperationResult doExecute(IOperationContext context, GetServerInformationOperation operation)
    {
        Map<String, String> info = new TreeMap<>();
        info.putAll(getPublicInformation(context));

        info.put("api-version", server.getMajorVersion() + "." + server.getMinorVersion());
        info.put("project-samples-enabled", Boolean.toString(CommonServiceProvider.getCommonServer().isProjectSamplesEnabled(null)));
        info.put("archiving-configured", Boolean.toString(CommonServiceProvider.getCommonServer().isArchivingConfigured(null)));
        info.put("enabled-technologies", configurer.getPropertyValue(Constants.ENABLED_MODULES_KEY));
        info.put("create-continuous-sample-codes", configurer.getPropertyValue(Constants.CREATE_CONTINUOUS_SAMPLES_CODES_KEY));
        info.put(PersonalAccessTokenConstants.PERSONAL_ACCESS_TOKENS_ENABLED_KEY,
                Boolean.toString(personalAccessTokenConfig.arePersonalAccessTokensEnabled()));
        info.put(PersonalAccessTokenConstants.PERSONAL_ACCESS_TOKENS_MAX_VALIDITY_PERIOD,
                Long.toString(personalAccessTokenConfig.getPersonalAccessTokensMaxValidityPeriod()));
        info.put(PersonalAccessTokenConstants.PERSONAL_ACCESS_TOKENS_VALIDITY_WARNING_PERIOD,
                Long.toString(personalAccessTokenConfig.getPersonalAccessTokensValidityWarningPeriod()));
        info.put("openbis-version", BuildAndEnvironmentInfo.INSTANCE.getVersion());

        if(isInstanceAdmin(context)) {
            long currentTimeMillis = System.currentTimeMillis();
            String expiryValueStr = configurer.getPropertyValue("server.information.expiry.duration.ms", String.valueOf(FIVE_MINUTES));
            long expiry = parseLong(expiryValueStr, FIVE_MINUTES);
            if(servicePropertiesString == null || lastServicePropertiesUpdate + expiry <= currentTimeMillis) {
                lastServicePropertiesUpdate = currentTimeMillis;
                Properties propertiesCopy = new Properties();
                configurer.getResolvedProps().forEach((key, value) -> {
                    if(key.toString().toLowerCase().contains("password")) {
                        propertiesCopy.setProperty((String) key, "*****");
                    } else {
                        propertiesCopy.setProperty((String) key, (String) value);
                    }
                });

                StringWriter writer = new StringWriter();
                try {
                    propertiesCopy.store(writer, "");
                    servicePropertiesString = writer.toString();
                    info.put("as-service-properties", servicePropertiesString);
                } catch (Exception e) {
                    info.put("as-service-properties", e.toString());
                }

            } else
            {
                info.put("as-service-properties", servicePropertiesString);
            }
        }


        return new GetServerInformationOperationResult(info);
    }

    /**
     * exception-save way to parse long value with default value
     */
    private long parseLong(String longValue, Long defaultValue) {
        try {
            return Long.parseLong(longValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, String> getPublicInformation(IOperationContext context)
    {
        GetServerPublicInformationOperation operation = new GetServerPublicInformationOperation();
        GetServerPublicInformationOperationResult result =
                (GetServerPublicInformationOperationResult) getPublicInformationExecutor.execute(context, Collections.singletonList(operation))
                        .get(operation);
        return result.getServerInformation();
    }

    private boolean isInstanceAdmin(IOperationContext context)
    {
        Set<RoleAssignmentPE> roles = context.getSession() != null && context.getSession().tryGetPerson() != null ?
                context.getSession().tryGetPerson().getAllPersonRoles() :
                Collections.emptySet();

        for (RoleAssignmentPE role : roles)
        {
            if (RoleWithHierarchy.RoleCode.ADMIN.equals(role.getRole()) && role.getRoleWithHierarchy().isInstanceLevel())
            {
                return true;
            }
        }

        return false;
    }

}
