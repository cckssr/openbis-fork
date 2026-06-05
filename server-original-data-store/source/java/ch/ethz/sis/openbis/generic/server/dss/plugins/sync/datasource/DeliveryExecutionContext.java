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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.internal.IDataSourceQueryService;

/**
 * @author Franz-Josef Elmer
 */
class DeliveryExecutionContext
{
    private XMLStreamWriter writer;

    private IDataSourceQueryService queryService;

    private String sessionToken;

    private Map<ExportableKind, List<String>> permIdsByKind = Collections.emptyMap();

    private Date requestTimestamp;

    private Set<String> fileServicePaths;

    public XMLStreamWriter getWriter()
    {
        return writer;
    }

    public void setWriter(XMLStreamWriter writer)
    {
        this.writer = writer;
    }

    public IDataSourceQueryService getQueryService()
    {
        return queryService;
    }

    public void setQueryService(IDataSourceQueryService queryService)
    {
        this.queryService = queryService;
    }

    public String getSessionToken()
    {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken)
    {
        this.sessionToken = sessionToken;
    }

    public Map<ExportableKind, List<String>> getPermIdsByKind()
    {
        return permIdsByKind;
    }

    public void setPermIdsByKind(Map<ExportableKind, List<String>> permIdsByKind)
    {
        this.permIdsByKind = permIdsByKind;
    }

    public List<String> getPermIds(ExportableKind kind)
    {
        return permIdsByKind.getOrDefault(kind, Collections.emptyList());
    }

    public Date getRequestTimestamp()
    {
        return requestTimestamp;
    }

    public void setRequestTimestamp(Date requestTimestamp)
    {
        this.requestTimestamp = requestTimestamp;
    }

    public Set<String> getFileServicePaths()
    {
        return fileServicePaths;
    }

    public void setFileServicePaths(Set<String> fileServicePaths)
    {
        this.fileServicePaths = fileServicePaths;
    }
}
