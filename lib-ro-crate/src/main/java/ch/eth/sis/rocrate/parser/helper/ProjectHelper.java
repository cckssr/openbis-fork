package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.ProjectImportHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectHelper extends BasicImportHelper
{
    Map<String, Project> acumulator = new HashMap<>();

    SpaceHelper spaceHelper;

    public ProjectHelper(ImportModes mode,
            ImportOptions options, SpaceHelper spaceHelper)
    {
        super(mode, options);
        this.spaceHelper = spaceHelper;
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return null;
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, ProjectImportHelper.Attribute.Code);
        String space = getValueByColumnName(header, values, ProjectImportHelper.Attribute.Space);
        String description =
                getValueByColumnName(header, values, ProjectImportHelper.Attribute.Description);
        Space space1 = spaceHelper.getSpace(space);

        Project project = new Project();
        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
        projectFetchOptions.withSpace();
        project.setFetchOptions(projectFetchOptions);

        project.setCode(code);
        project.setDescription(description);
        project.setSpace(space1);
        acumulator.put("/" + space + "/" + code, project);

    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {

    }

    @Override
    protected void validateHeader(Map<String, Integer> header)
    {

    }

    public Project getProject(String key)
    {
        return acumulator.get(key);
    }

    public Map<ProjectIdentifier, Project> getResult()
    {
        return acumulator.values().stream().collect(
                Collectors.toMap(x -> new ProjectIdentifier(x.getSpace().getCode(), x.getCode()),
                        x -> x));
    }

}
