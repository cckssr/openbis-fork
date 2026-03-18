/*
 * Copyright ETH 2010 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto;

import java.io.Serializable;
import java.util.Arrays;

import com.google.gwt.user.client.rpc.IsSerializable;

import ch.systemsx.cisd.common.shared.basic.string.StringUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.IEntityInformationHolderWithIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.BasicProjectIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ServiceVersionHolder;
//import ch.systemsx.cisd.openbis.plugin.screening.client.web.client.application.detailviewers.WellSearchGrid;

/**
 * Describes a list of materials for which we search
 * 
 * @author Tomasz Pylak
 */
public class WellSearchCriteria implements Serializable
{
    private static final long serialVersionUID = ServiceVersionHolder.VERSION;

    /** points to one experiment */
    public static final class SingleExperimentSearchCriteria implements Serializable
    {
        private static final long serialVersionUID = ServiceVersionHolder.VERSION;

        public static final SingleExperimentSearchCriteria EMPTY_CRITERIA =
                new SingleExperimentSearchCriteria();

        private TechId experimentId;

        private String experimentPermId;

        private String experimentIdentifier; // for display purposes and links in simple view mode

        // GWT only
        private SingleExperimentSearchCriteria()
        {
        }

        public SingleExperimentSearchCriteria(IEntityInformationHolderWithIdentifier experiment)
        {
            this(experiment.getId(), experiment.getPermId(), experiment.getIdentifier());
        }

        public SingleExperimentSearchCriteria(long experimentId, String experimentPermId,
                String experimentIdentifier)
        {
            this.experimentId = new TechId(experimentId);
            this.experimentPermId = experimentPermId;
            this.experimentIdentifier = experimentIdentifier;
        }

        public TechId getExperimentId()
        {
            return experimentId;
        }

        public String getExperimentPermId()
        {
            return experimentPermId;
        }

        public String getExperimentIdentifier()
        {
            return experimentIdentifier;
        }

        @Override
        public String toString()
        {
            return "experiment " + experimentIdentifier;
        }
    }

    public static final class ExperimentSearchCriteriaHolder
    {
        private ExperimentSearchCriteria criteriaOrNull;

        public ExperimentSearchCriteriaHolder(ExperimentSearchCriteria criteriaOrNull)
        {
            this.criteriaOrNull = criteriaOrNull;
        }

        public ExperimentSearchCriteria tryGetCriteria()
        {
            return criteriaOrNull;
        }

        public void setCriteria(ExperimentSearchCriteria criteria)
        {
            this.criteriaOrNull = criteria;
        }

    }

    /** Points to one experiment, all experiments of a project or all accessible experiments. */
    public static final class ExperimentSearchCriteria implements IsSerializable, Serializable
    {
        private static final long serialVersionUID = ServiceVersionHolder.VERSION;

        // if null, all experiments are taken into account (from one or all projects)
        private SingleExperimentSearchCriteria experimentOrNull;

        private BasicProjectIdentifier projectIdOrNull;

        /**
         * Valid only if single experiment is used as a criteria.
         */
        // TODO 2011-06-07, Tomasz Pylak: Determines UI behaviour, should be refactored out from
        // this server side DTO.
        private boolean restrictGlobalScopeLinkToProject = false;

        // GWT only
        private ExperimentSearchCriteria()
        {
        }

        public SingleExperimentSearchCriteria tryGetExperiment()
        {
            return experimentOrNull;
        }

        private ExperimentSearchCriteria(SingleExperimentSearchCriteria experimentOrNull,
                boolean restrictGlobalScopeLinkToProject)
        {
            this.experimentOrNull = experimentOrNull;
            this.projectIdOrNull = null;
            this.restrictGlobalScopeLinkToProject = restrictGlobalScopeLinkToProject;
        }

        private ExperimentSearchCriteria(BasicProjectIdentifier projectIdOrNull)
        {
            this.projectIdOrNull = projectIdOrNull;
        }

        public static ExperimentSearchCriteria createExperiment(
                SingleExperimentSearchCriteria experiment, boolean restrictGlobalScopeLinkToProject)
        {
            return new ExperimentSearchCriteria(experiment, restrictGlobalScopeLinkToProject);
        }

        public static ExperimentSearchCriteria createExperiment(
                IEntityInformationHolderWithIdentifier experiment)
        {
            return createExperiment(experiment, false);
        }

        public static ExperimentSearchCriteria createExperiment(
                IEntityInformationHolderWithIdentifier experiment,
                boolean restrictGlobalScopeLinkToProject)
        {
            return createExperiment(new SingleExperimentSearchCriteria(experiment),
                    restrictGlobalScopeLinkToProject);
        }

        public static ExperimentSearchCriteria createAllExperiments()
        {
            return new ExperimentSearchCriteria(null, false);
        }

        public static final ExperimentSearchCriteria createAllExperimentsForProject(
                BasicProjectIdentifier projectIdentifier)
        {
            if (projectIdentifier == null)
            {
                throw new IllegalArgumentException("Project identifier cannot be null");
            }
            return new ExperimentSearchCriteria(projectIdentifier);
        }

        public BasicProjectIdentifier tryGetProjectIdentifier()
        {
            return projectIdOrNull;
        }

        /**
         * Valid only if single experiment is used as a criteria.<br>
         * It determines the behavior of the link from material detail view in a single experiment context to the material detail view in 'global'
         * context. If this parameter is true, the context will be the project to which the current experiment belongs, otherwise the context will be
         * switched to all experiments.
         */
        public boolean getRestrictGlobalSearchLinkToProject()
        {
            return restrictGlobalScopeLinkToProject;
        }

        public ExperimentSearchByProjectCriteria tryAsSearchByProjectCriteria()
        {
            if (tryGetExperiment() != null)
            {
                return null;
            } else if (projectIdOrNull != null)
            {
                return ExperimentSearchByProjectCriteria
                        .createAllExperimentsForProject(projectIdOrNull);
            } else
            {
                return ExperimentSearchByProjectCriteria.createAllExperimentsForAllProjects();
            }
        }

        @Override
        public String toString()
        {
            if (experimentOrNull == null)
            {
                if (projectIdOrNull == null)
                {
                    return "all experiments";
                } else
                {
                    return "all experiments from project " + projectIdOrNull;
                }
            } else
            {
                return experimentOrNull.toString();
            }
        }

    }

    public static final class ExperimentSearchByProjectCriteria implements IsSerializable,
            Serializable
    {

        private static final long serialVersionUID = ServiceVersionHolder.VERSION;

        private BasicProjectIdentifier projectIdOrNull;

        public static final ExperimentSearchByProjectCriteria createAllExperimentsForProject(
                BasicProjectIdentifier projectIdentifier)
        {
            if (projectIdentifier == null)
            {
                throw new IllegalArgumentException("Project identifier cannot be null");
            }
            return new ExperimentSearchByProjectCriteria(projectIdentifier);
        }

        public static final ExperimentSearchByProjectCriteria createAllExperimentsForAllProjects()
        {
            return new ExperimentSearchByProjectCriteria(null);
        }

        // GWT only
        private ExperimentSearchByProjectCriteria()
        {
        }

        private ExperimentSearchByProjectCriteria(BasicProjectIdentifier projectIdOrNull)
        {
            this.projectIdOrNull = projectIdOrNull;
        }

        public BasicProjectIdentifier tryGetProjectIdentifier()
        {
            return projectIdOrNull;
        }

        public boolean isAllExperiments()
        {
            return projectIdOrNull == null;
        }

        public ExperimentSearchCriteria asExtendedCriteria()
        {
            if (projectIdOrNull != null)
            {
                return ExperimentSearchCriteria.createAllExperimentsForProject(projectIdOrNull);
            } else
            {
                return ExperimentSearchCriteria.createAllExperiments();
            }
        }

        @Override
        public String toString()
        {
            if (isAllExperiments())
            {
                return "all experiments";
            } else
            {
                return projectIdOrNull.toString();
            }
        }
    }

    public static final class AnalysisProcedureCriteria implements Serializable
    {
        private static final long serialVersionUID = ServiceVersionHolder.VERSION;

        public static enum MatchType implements Serializable
        {
            /** match data sets for explicitly specified analysis procedure code. */
            BY_CODE,
            /** match any data set */
            ANY,
            /** match data sets having no analysis procedure property */
            NONE
        }

        public static AnalysisProcedureCriteria createFromCode(String code)
        {
            if (StringUtils.isBlank(code))
            {
                throw new IllegalArgumentException("Cannot construct analysis procedure search "
                        + "criteria of this type without a specified code.");
            }
            return new AnalysisProcedureCriteria(MatchType.BY_CODE, code);
        }

        public static AnalysisProcedureCriteria createAllProcedures()
        {
            return new AnalysisProcedureCriteria(MatchType.ANY, null);
        }

        public static AnalysisProcedureCriteria createNoProcedures()
        {
            return new AnalysisProcedureCriteria(MatchType.NONE, null);
        }

        private MatchType matchType;

        private String analysisProcedureCodeOrNull;

        // GWT only
        private AnalysisProcedureCriteria()
        {
        }

        private AnalysisProcedureCriteria(MatchType matchType, String analysisProcedureCodeOrNull)
        {
            this.matchType = matchType;
            this.analysisProcedureCodeOrNull = analysisProcedureCodeOrNull;
        }

        /**
         * Return true if this criteria selects all data sets regardless of their property values.
         */
        public boolean isAllProcedures()
        {
            return matchType == MatchType.ANY;
        }

        /**
         * Return true if this criteria selects only data sets without APs, false otherwise.
         */
        public boolean isNoProcedures()
        {
            return matchType == MatchType.NONE;
        }

        public String tryGetAnalysisProcedureCode()
        {
            return analysisProcedureCodeOrNull;
        }

        public boolean matches(String codeOrNull)
        {
            if (isAllProcedures())
            {
                return true;
            }
            if (StringUtils.isBlank(codeOrNull))
            {
                return isNoProcedures();
            }
            return codeOrNull.equals(analysisProcedureCodeOrNull);
        }

        @Override
        public String toString()
        {
            return "[Analysis procedure code '" + analysisProcedureCodeOrNull + "', match = '"
                    + matchType + "']";
        }
    }

    private ExperimentSearchCriteria experimentCriteria;

    private AnalysisProcedureCriteria analysisProcedureCriteria;

    // GWT
    @SuppressWarnings("unused")
    private WellSearchCriteria()
    {
    }

    public WellSearchCriteria(ExperimentSearchCriteria experimentCriteria,
            AnalysisProcedureCriteria analysisProcedureCriteria)
    {
        assert experimentCriteria != null;
        assert analysisProcedureCriteria != null;

        this.experimentCriteria = experimentCriteria;
        this.analysisProcedureCriteria = analysisProcedureCriteria;
    }

    @Override
    public String toString()
    {
        return "Search criteria: " + experimentCriteria + ", "
                + analysisProcedureCriteria;
    }

    /** null means all experiments */
    public ExperimentSearchCriteria getExperimentCriteria()
    {
        return experimentCriteria;
    }

    public AnalysisProcedureCriteria getAnalysisProcedureCriteria()
    {
        return analysisProcedureCriteria;
    }

    public static boolean shouldRestrictScopeToProject(ExperimentSearchCriteria searchCriteria)
    {
        return searchCriteria != null && searchCriteria.tryGetProjectIdentifier() != null;
    }

    public static boolean shouldRestrictScopeToProject(
            ExperimentSearchByProjectCriteria searchCriteria)
    {
        return searchCriteria != null && searchCriteria.tryGetProjectIdentifier() != null;
    }

}
