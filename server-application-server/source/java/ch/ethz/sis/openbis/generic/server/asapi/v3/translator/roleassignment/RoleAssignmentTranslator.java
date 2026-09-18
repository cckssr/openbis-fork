/*
 *  Copyright ETH 2017 - 2025 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.translator.roleassignment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.RoleAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.RoleLevel;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.fetchoptions.RoleAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.id.RoleAssignmentTechId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.OperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.roleassignment.IRoleAssignmentAuthorizationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.roleassignment.RoleAssignmentUtils;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.AbstractCachingTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationResults;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.authorizationgroup.AuthorizationGroupQuery;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.space.SpaceQuery;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.openbis.generic.shared.authorization.IAuthorizationConfig;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.RoleWithHierarchy;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.RoleAssignmentPE;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.lemnik.eodsql.QueryTool;

/**
 * @author Franz-Josef Elmer
 */
@Component
public class RoleAssignmentTranslator
        extends AbstractCachingTranslator<Long, RoleAssignment, RoleAssignmentFetchOptions>
        implements IRoleAssignmentTranslator
{
    @Autowired
    private IRoleAssignmentAuthorizationExecutor authorizationExecutor;

    @Autowired
    private IRoleAssignmentBaseTranslator baseTranslator;

    @Autowired
    private IRoleAssignmentRegistratorTranslator registratorTranslator;

    @Autowired
    private IRoleAssignmentPersonTranslator userTranslator;

    @Autowired
    private IRoleAssignmentAuthorizationGroupTranslator authorizationGroupTranslator;

    @Autowired
    private IRoleAssignmentSpaceTranslator spaceTranslator;

    @Autowired
    private IRoleAssignmentProjectTranslator projectTranslator;

    @Autowired
    private IAuthorizationConfig authorizationConfig;

    @Override
    protected Set<Long> shouldTranslate(TranslationContext context, Collection<Long> inputs, RoleAssignmentFetchOptions fetchOptions)
    {
        try
        {
            authorizationExecutor.canGet(new OperationContext(context.getSession()));
        } catch (AuthorizationFailureException ex)
        {
            return new HashSet<>();
        }

        PersonPE person = context.getSession().tryGetPerson();

        if (person.isSystemUser() || RoleAssignmentUtils.isInstanceAdmin(person) || RoleAssignmentUtils.isETLServer(person))
        {
            // only these users can see all role assignments
            return new HashSet<>(inputs);
        } else
        {
            RoleAssignmentQuery assignmentQuery = QueryTool.getManagedQuery(RoleAssignmentQuery.class);
            List<RoleAssignmentBaseRecord> assignments = assignmentQuery.getRoleAssignments(new LongOpenHashSet(inputs));

            Set<Long> spacesUserIsAdminOf = new HashSet<>();
            Set<Long> projectsUserIsAdminOf = new HashSet<>();

            // find spaces and projects the user is admin of
            for (RoleAssignmentPE role : context.getSession().tryGetPerson().getAllPersonRoles())
            {
                if (role.getRole().equals(RoleWithHierarchy.RoleCode.ADMIN))
                {
                    if (role.getRoleLevel().equals(RoleWithHierarchy.RoleLevel.SPACE))
                    {
                        spacesUserIsAdminOf.add(role.getSpace().getId());
                    } else if (role.getRoleLevel().equals(RoleWithHierarchy.RoleLevel.PROJECT) && authorizationConfig.isProjectLevelEnabled()
                            && authorizationConfig.isProjectLevelUser(context.getSession().tryGetPerson().getUserId()))
                    {
                        projectsUserIsAdminOf.add(role.getProject().getId());
                    }
                }
            }

            // add projects that belong to the spaces the user is admin of
            SpaceQuery spaceQuery = QueryTool.getManagedQuery(SpaceQuery.class);
            Set<Long> projectsInSpacesUserIsAdminOf =
                    spaceQuery.getProjectIds(new LongOpenHashSet(spacesUserIsAdminOf)).stream().map(r -> r.relatedId).collect(Collectors.toSet());
            projectsUserIsAdminOf.addAll(projectsInSpacesUserIsAdminOf);

            // find groups the user belongs to
            AuthorizationGroupQuery groupQuery = QueryTool.getManagedQuery(AuthorizationGroupQuery.class);
            Set<Long> groupsUserBelongsTo = groupQuery.getAuthorizationGroupsForUser(context.getSession().tryGetPerson().getId()).
                    stream().map(r -> r.relatedId).collect(Collectors.toSet());

            Set<Long> result = new HashSet<>();
            for (RoleAssignmentBaseRecord assignment : assignments)
            {
                if (assignment.pers_id_grantee != null && assignment.pers_id_grantee.equals(context.getSession().tryGetPerson().getId()))
                {
                    // roles assigned to the user
                    result.add(assignment.id);
                } else if (assignment.ag_id_grantee != null && groupsUserBelongsTo.contains(assignment.ag_id_grantee))
                {
                    // roles assigned to a group the user belongs to
                    result.add(assignment.id);
                } else if (assignment.space_id != null && spacesUserIsAdminOf.contains(assignment.space_id))
                {
                    // roles assigned to other users/groups but for spaces the user is admin of
                    result.add(assignment.id);
                } else if (assignment.project_id != null && projectsUserIsAdminOf.contains(assignment.project_id))
                {
                    // roles assigned to other users/groups but for projects the user is admin of
                    result.add(assignment.id);
                }
            }
            return result;
        }
    }

    @Override
    protected void filterTranslated(TranslationContext context, Map<Long, RoleAssignment> translated)
    {
        if (authorizationConfig.isProjectLevelEnabled())
        {
            return;
        }

        Set<Long> projectRoleIds = new HashSet<>();

        for (Map.Entry<Long, RoleAssignment> entry : translated.entrySet())
        {
            if (RoleLevel.PROJECT.equals(entry.getValue().getRoleLevel()))
            {
                projectRoleIds.add(entry.getKey());
            }
        }

        for (Long projectRoleId : projectRoleIds)
        {
            translated.remove(projectRoleId);
        }
    }

    @Override
    protected RoleAssignment createObject(TranslationContext context, Long input, RoleAssignmentFetchOptions fetchOptions)
    {
        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setFetchOptions(fetchOptions);
        return roleAssignment;
    }

    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<Long> inputs, RoleAssignmentFetchOptions fetchOptions)
    {
        TranslationResults relations = new TranslationResults();

        relations.put(IRoleAssignmentBaseTranslator.class, baseTranslator.translate(context, inputs, null));
        if (fetchOptions.hasRegistrator())
        {
            relations.put(IRoleAssignmentRegistratorTranslator.class,
                    registratorTranslator.translate(context, inputs, fetchOptions.withRegistrator()));
        }
        if (fetchOptions.hasUser())
        {
            relations.put(IRoleAssignmentPersonTranslator.class, userTranslator.translate(context, inputs, fetchOptions.withUser()));
        }
        if (fetchOptions.hasAuthorizationGroup())
        {
            relations.put(IRoleAssignmentAuthorizationGroupTranslator.class,
                    authorizationGroupTranslator.translate(context, inputs, fetchOptions.withAuthorizationGroup()));
        }
        if (fetchOptions.hasSpace())
        {
            relations.put(IRoleAssignmentSpaceTranslator.class, spaceTranslator.translate(context, inputs, fetchOptions.withSpace()));
        }
        if (fetchOptions.hasProject())
        {
            relations.put(IRoleAssignmentProjectTranslator.class, projectTranslator.translate(context, inputs, fetchOptions.withProject()));
        }

        return relations;
    }

    @Override
    protected void updateObject(TranslationContext context, Long input, RoleAssignment output, Object objectRelations,
            RoleAssignmentFetchOptions fetchOptions)
    {
        TranslationResults relations = (TranslationResults) objectRelations;
        RoleAssignmentBaseRecord baseRecord = relations.get(IRoleAssignmentBaseTranslator.class, input);
        output.setId(new RoleAssignmentTechId(baseRecord.id));
        output.setRole(Role.valueOf(baseRecord.role_code));
        output.setRoleLevel(extractRoleLevel(baseRecord));
        output.setRegistrationDate(baseRecord.registrationDate);

        if (fetchOptions.hasRegistrator())
        {
            output.setRegistrator(relations.get(IRoleAssignmentRegistratorTranslator.class, input));
            output.getFetchOptions().withRegistratorUsing(fetchOptions.withRegistrator());
        }
        if (fetchOptions.hasUser())
        {
            output.setUser(relations.get(IRoleAssignmentPersonTranslator.class, input));
            output.getFetchOptions().withUserUsing(fetchOptions.withUser());
        }
        if (fetchOptions.hasAuthorizationGroup())
        {
            output.setAuthorizationGroup(relations.get(IRoleAssignmentAuthorizationGroupTranslator.class, input));
            output.getFetchOptions().withAuthorizationGroupUsing(fetchOptions.withAuthorizationGroup());
        }
        if (fetchOptions.hasSpace())
        {
            output.setSpace(relations.get(IRoleAssignmentSpaceTranslator.class, input));
            output.getFetchOptions().withSpaceUsing(fetchOptions.withSpace());
        }
        if (fetchOptions.hasProject())
        {
            output.setProject(relations.get(IRoleAssignmentProjectTranslator.class, input));
            output.getFetchOptions().withProjectUsing(fetchOptions.withProject());
        }
    }

    private RoleLevel extractRoleLevel(RoleAssignmentBaseRecord baseRecord)
    {
        if (baseRecord.space_id != null)
        {
            return RoleLevel.SPACE;
        }
        if (baseRecord.project_id != null)
        {
            return RoleLevel.PROJECT;
        }
        return RoleLevel.INSTANCE;
    }

}
