/*
 *  Copyright ETH 2024 Zürich, Scientific IT Services
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

package ch.ethz.sis.afsserver.server.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.ethz.sis.afs.api.TransactionConnectionInformation;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.performance.PerformanceAuditor;
import ch.ethz.sis.libhttp.http.HttpResponse;
import io.netty.handler.codec.http.HttpMethod;

public class HttpDownloadAdapter<CONNECTION extends TransactionConnectionInformation, API> extends AbstractAdapter<CONNECTION, API>
{
    public HttpDownloadAdapter(
            final APIServer<CONNECTION, Request, Response, API> server,
            final JsonObjectMapper jsonObjectMapper)
    {
        super(server, jsonObjectMapper);
    }

    @Override
    protected boolean isValidHttpMethod(final HttpMethod givenMethod, final String apiMethod)
    {
        return "zip".equals(apiMethod) && isValidHttpMethod(givenMethod);
    }

    @Override
    protected boolean isValidHttpMethod(final HttpMethod givenMethod)
    {
        return givenMethod == HttpMethod.GET || givenMethod == HttpMethod.POST;
    }

    @Override
    protected void parseParameters(final String key, final List<String> values, final Map<String, Object> parsedParameters)
    {
        switch (key)
        {
            case "owners":
            case "sources":
                parsedParameters.put(key, values);
                break;
        }
    }

    @Override
    protected HttpResponse process(final String method, final Map<String, Object> parsedParameters, final String sessionToken,
            final String interactiveSessionKey, final String transactionManagerKey, final UUID operationId, final PerformanceAuditor performanceAuditor) throws Exception
    {
        return new HttpResponse(HttpResponse.OK,
                Map.of(HttpResponse.CONTENT_TYPE_HEADER, HttpResponse.CONTENT_TYPE_ZIP),
                new HttpDownloadInputStream<>(server));
    }

    @Override
    public String getPath()
    {
        return "download";
    }

}
