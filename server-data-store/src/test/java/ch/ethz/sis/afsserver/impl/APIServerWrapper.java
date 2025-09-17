/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.impl;


import ch.ethz.sis.afsclient.client.ChunkEncoderDecoder;
import ch.ethz.sis.afsserver.core.AbstractPublicAPIWrapper;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.impl.ApiRequest;
import ch.ethz.sis.afsserver.server.impl.ApiResponseBuilder;
import ch.ethz.sis.afsserver.server.performance.PerformanceAuditor;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class APIServerWrapper extends AbstractPublicAPIWrapper
{

    private static final Logger logger = LogManager.getLogger(APIServerWrapper.class);

    private APIServer apiServer;
    private String interactiveSessionKey;
    private String transactionManagerKey;
    private String sessionToken;

    private final ApiResponseBuilder apiResponseBuilder;

    public APIServerWrapper(APIServer apiServer, String interactiveSessionKey, String transactionManagerKey, String sessionToken) {
        this.apiServer = apiServer;
        this.interactiveSessionKey = interactiveSessionKey;
        this.transactionManagerKey = transactionManagerKey;
        this.sessionToken = sessionToken;
        this.apiResponseBuilder = new ApiResponseBuilder();
    }

    public <E> E process(Class<E> responseType, String method, Map<String, Object> params, byte[] data) {
        PerformanceAuditor performanceAuditor = new PerformanceAuditor();
        // Random Session token just works for tests with dummy authentication
        Map<String, Object> requestParams = prepareParameters(params, data);

        ApiRequest request = new ApiRequest("test", method, requestParams, sessionToken, interactiveSessionKey, transactionManagerKey);

        try {
            Response response = apiServer.processOperation(request, apiResponseBuilder, performanceAuditor);
            return (E) response.getResult();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private Map<String, Object> prepareParameters(Map<String, Object> params, byte[] data) {
        if (data == null)
        {
            return params;
        }
        Map<String, Object> updatedParams = new HashMap<>(params);
        updatedParams.put("chunks", ChunkEncoderDecoder.decodeChunks(data));
        return Collections.unmodifiableMap(updatedParams);
    }

}