package ch.ethz.sis.openbis.generic.server.xls.export;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.exporter.ExportEntityCollector;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.ICustomASServiceExecutor;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.context.CustomASServiceContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityCollectorExtendedService implements ICustomASServiceExecutor
{
    private final String code = "entity-collector-extended";

    private final String label = "Entity Collector Extended Service";

    private final String description = "Entity collector for internal and external services";

    @Override
    public Object executeService(CustomASServiceContext context,
            CustomASServiceExecutionOptions options)
    {
        return collectEntities(context.getSessionToken(), options.getParameters());
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public static Set<ExportablePermId> collectEntities(String sessionToken, Map<String, Object> parameters) {

        List<Map<String, Object>> nodeExportMaps = (List<Map<String, Object>>) parameters.get("nodeExportList");
        if (nodeExportMaps == null || nodeExportMaps.isEmpty()) {
            throw new IllegalArgumentException("The parameter nodeExportList cannot be null or empty.");
        }

        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        Set<ExportablePermId> allPermIds = new HashSet<>();

        for (Map<String, Object> nodeExportMap : nodeExportMaps) {
            String kind = (String) nodeExportMap.get("kind");
            String permId = (String) nodeExportMap.get("permId");
            boolean withLevelsAbove = (boolean) nodeExportMap.get("withLevelsAbove");
            boolean withLevelsBelow = (boolean) nodeExportMap.get("withLevelsBelow");
            boolean withObjectsAndDataSetsParents = (boolean) nodeExportMap.get("withObjectsAndDataSetsParents");
            boolean withObjectsAndDataSetsChildren = (boolean) nodeExportMap.get("withObjectsAndDataSetsChildren");
            boolean withObjectsAndDataSetsOtherSpaces = (boolean) nodeExportMap.get("withObjectsAndDataSetsOtherSpaces");

            ExportableKind rootKind = ExportableKind.valueOf(kind);
            ExportablePermId root = new ExportablePermId(rootKind, permId);
            ExportEntityCollector.collectEntities(api, sessionToken, allPermIds, root, withLevelsAbove, withLevelsBelow, withObjectsAndDataSetsParents, withObjectsAndDataSetsChildren, withObjectsAndDataSetsOtherSpaces);
        }
        return allPermIds;
    }
}
