package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.from.utils.IAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectHelper extends BasicImportHelper
{

    public enum Attribute implements IAttribute
    {
        Identifier("Identifier", false, true),
        Code("Code", true, true),
        Space("Space", true, true),
        Description("Description", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    Map<String, Project> acumulator = new HashMap<>();

    SpaceHelper spaceHelper;

    public ProjectHelper(SpaceHelper spaceHelper)
    {
        this.spaceHelper = spaceHelper;
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
        String code = getValueByColumnName(header, values, Attribute.Code);
        String space = getValueByColumnName(header, values, Attribute.Space);
        String description =
                getValueByColumnName(header, values, Attribute.Description);
        Space space1 = spaceHelper.getSpace(new SpacePermId(space));

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
