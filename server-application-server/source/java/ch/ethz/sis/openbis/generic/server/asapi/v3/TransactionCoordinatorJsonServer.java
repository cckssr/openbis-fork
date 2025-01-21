/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.sis.afsapi.api.OperationsAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionCoordinatorApi;
import ch.ethz.sis.openbis.generic.server.sharedapi.v3.json.ObjectMapperResource;
import ch.ethz.sis.transaction.TransactionOperationException;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.common.api.server.AbstractApiJsonServiceExporter;

/**
 * @author pkupczyk
 */
@Controller
public class TransactionCoordinatorJsonServer extends AbstractApiJsonServiceExporter
{
    @Resource(name = ObjectMapperResource.NAME)
    private ObjectMapper objectMapper;

    @Autowired
    private ITransactionCoordinatorApi transactionCoordinatorApi;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        setObjectMapper(objectMapper);
        establishService(ITransactionCoordinatorApi.class, new AfsTypesConvertingDecorator(transactionCoordinatorApi),
                ITransactionCoordinatorApi.SERVICE_NAME, ITransactionCoordinatorApi.JSON_SERVICE_URL);
        super.afterPropertiesSet();
    }

    @RequestMapping({ ITransactionCoordinatorApi.JSON_SERVICE_URL, "/openbis" + ITransactionCoordinatorApi.JSON_SERVICE_URL })
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException,
            IOException
    {
        if (request.getMethod().equals(HttpMethod.OPTIONS.name()))
        {
            return;
        }

        super.handleRequest(request, response);
    }

    private static class AfsTypesConvertingDecorator implements ITransactionCoordinatorApi
    {

        private static final DateTimeFormatter AFS_DATE_FORMAT = DateTimeFormatter.ISO_INSTANT;

        private final ITransactionCoordinatorApi transactionCoordinatorApi;

        public AfsTypesConvertingDecorator(final ITransactionCoordinatorApi transactionCoordinatorApi)
        {
            this.transactionCoordinatorApi = transactionCoordinatorApi;
        }

        @Override public void beginTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
        {
            transactionCoordinatorApi.beginTransaction(transactionId, sessionToken, interactiveSessionKey);
        }

        @Override public <T> T executeOperation(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
                final String participantId, final String operationName, final Object[] operationArguments)
                throws TransactionOperationException
        {
            if (ITransactionCoordinatorApi.AFS_SERVER_PARTICIPANT_ID.equals(participantId))
            {
                for (Method method : OperationsAPI.class.getDeclaredMethods())
                {
                    if (method.getName().equals(operationName) && method.getParameters().length != operationArguments.length)
                    {
                        throw new UserFailureException(
                                "Incorrect number of arguments for AFS server operation '" + operationName + "'. Expected "
                                        + method.getParameters().length
                                        + " but got " + operationArguments.length);
                    }
                }

                Object[] convertOperationArguments = convertOperationArguments(operationName, operationArguments);

                T operationResult = transactionCoordinatorApi.executeOperation(transactionId, sessionToken, interactiveSessionKey, participantId,
                        operationName, convertOperationArguments);

                return (T) convertOperationResult(operationName, operationResult);
            } else
            {
                return transactionCoordinatorApi.executeOperation(transactionId, sessionToken, interactiveSessionKey, participantId,
                        operationName, operationArguments);
            }
        }

        @Override public void commitTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
        {
            transactionCoordinatorApi.commitTransaction(transactionId, sessionToken, interactiveSessionKey);
        }

        @Override public void rollbackTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
        {
            transactionCoordinatorApi.rollbackTransaction(transactionId, sessionToken, interactiveSessionKey);
        }

        @Override public int getMajorVersion()
        {
            return transactionCoordinatorApi.getMajorVersion();
        }

        @Override public int getMinorVersion()
        {
            return transactionCoordinatorApi.getMinorVersion();
        }

        private Object[] convertOperationArguments(String operationName, Object[] operationArguments)
        {
            Object[] convertedOperationArguments = Arrays.copyOf(operationArguments, operationArguments.length);

            for (Method method : OperationsAPI.class.getDeclaredMethods())
            {
                if (method.getName().equals(operationName))
                {
                    int index = 0;

                    for (Parameter methodParameter : method.getParameters())
                    {
                        Object operationArgument = operationArguments[index];

                        if (methodParameter.getType().isArray() && methodParameter.getType().getComponentType().equals(byte.class)
                                && operationArgument instanceof String)
                        {
                            // for byte[] operation arguments accept also String values
                            convertedOperationArguments[index] = Base64.getUrlDecoder().decode((String) operationArgument);
                        } else if (methodParameter.getType().equals(Long.class) && operationArgument instanceof Number)
                        {
                            // for Long operation arguments accept also other Number values
                            convertedOperationArguments[index] = ((Number) operationArgument).longValue();
                        } else
                        {
                            convertedOperationArguments[index] = operationArgument;
                        }

                        index++;
                    }
                }
            }

            return convertedOperationArguments;
        }

        private Object convertOperationResult(String operationName, Object operationResult)
        {
            if (operationResult instanceof List)
            {
                List<Object> resultList = (List<Object>) operationResult;
                List<Object> convertedResultList = new ArrayList<>();

                for (Object item : resultList)
                {
                    if (item instanceof ch.ethz.sis.afsapi.dto.File)
                    {
                        ch.ethz.sis.afsapi.dto.File file = (File) item;
                        Map<String, Object> convertedFile = new HashMap<>();
                        convertedFile.put("owner", file.getOwner());
                        convertedFile.put("path", file.getPath());
                        convertedFile.put("name", file.getName());
                        convertedFile.put("directory", file.getDirectory());
                        convertedFile.put("size", file.getSize());
                        convertedFile.put("lastModifiedTime", formatOffsetDateTime(file.getLastModifiedTime()));
                        convertedResultList.add(convertedFile);
                    }
                }

                return convertedResultList;
            }

            return operationResult;
        }

        private String formatOffsetDateTime(OffsetDateTime dateTime)
        {
            if (dateTime != null)
            {
                return AFS_DATE_FORMAT.format(dateTime);
            } else
            {
                return null;
            }
        }

    }

}
