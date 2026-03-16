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
package ch.systemsx.cisd.openbis.plugin.screening.server.dataaccess;

import java.util.List;

import net.lemnik.eodsql.BaseQuery;
import net.lemnik.eodsql.DataIterator;
import net.lemnik.eodsql.Select;
import net.lemnik.eodsql.TypeMapper;

import ch.systemsx.cisd.common.db.mapper.LongArrayMapper;
import ch.systemsx.cisd.common.db.mapper.StringArrayMapper;

/**
 * Screening specific queries on openbis database.
 * 
 * @author Tomasz Pylak
 */
public interface IScreeningQuery extends BaseQuery
{

    public static final int FETCH_SIZE = 1000;

    /**
     * Returns the plate geometry string for the plate with given <var>platePermId</var>, or <code>null</code>, if no plate with that perm id can be
     * found.
     */
    @Select(sql = "select space.code as space_code, pl.code as plate_code, cvte.code as plate_geometry "
            + "      from sample_properties sp "
            + "         join samples pl on pl.id = sp.samp_id "
            + "         join controlled_vocabulary_terms cvte on cvte.id = sp.cvte_id "
            + "         join sample_type_property_types stpt on stpt.id = sp.stpt_id "
            + "         join property_types pt on pt.id = stpt.prty_id "
            + "         join spaces space on pl.space_id = space.id"
            + "      where pt.code = 'PLATE_GEOMETRY' "
            + "         and pt.is_managed_internally = true and pl.perm_id = ?{1}")
    public PlateGeometryContainer tryGetPlateGeometry(String platePermId);

    /**
     * Returns the plate geometry string for the plate with given <var>spaceCode</var> and <var>plateCode</var>, or <code>null</code>, if no plate
     * with that code can be found.
     */
    @Select(sql = "select pl.perm_id, cvte.code as plate_geometry "
            + "      from sample_properties sp "
            + "         join samples pl on pl.id = sp.samp_id "
            + "         join controlled_vocabulary_terms cvte on cvte.id = sp.cvte_id "
            + "         join sample_type_property_types stpt on stpt.id = sp.stpt_id "
            + "         join property_types pt on pt.id = stpt.prty_id "
            + "         join spaces space on pl.space_id = space.id"
            + "      where pt.code = 'PLATE_GEOMETRY' "
            + "         and pt.is_managed_internally = true and space.code = ?{1} and pl.code = ?{2}")
    public PlateGeometryContainer tryGetPlateGeometry(String spaceCode, String plateCode);

    final static String ANALYSIS_PROCEDURE_SELECT =
            "select distinct "
                    + "       ds_props.value as analysisProcedure, ds_type.code as datasetTypeCode"
                    + "  from experiments exp "
                    + "       join data ds on ds.expe_id = exp.id "
                    + "       join samples plate on plate.id = ds.samp_id "
                    + "       join samples well on well.samp_id_part_of = plate.id "
                    + "       join data_set_types ds_type on ds.dsty_id = ds_type.id "
                    + "       join data_set_type_property_types  dst_pt on dst_pt.dsty_id = ds_type.id "
                    + "       left outer join data_set_properties ds_props on ds_props.ds_id = ds.id and ds_props.dstpt_id = dst_pt.id"
                    + "  where "
                    + "       dst_pt.prty_id = (select id from property_types where code='ANALYSIS_PROCEDURE' and is_managed_internally=true)";

    @Select(sql = ANALYSIS_PROCEDURE_SELECT)
    public List<AnalysisProcedureResult> listAllAnalysisProcedures();

    @Select(sql = ANALYSIS_PROCEDURE_SELECT + " and exp.id = ?{1}")
    public List<AnalysisProcedureResult> listAnalysisProceduresForExperiment(long experimentId);

    final static String SELECT_DSS_CODES_FOR_EXPERIMENT = "  select distinct datastore.code "
            + "      from experiments exp "
            + "           join data dataset on dataset.expe_id = exp.id "
            + "           join data_stores datastore on dataset.dast_id = datastore.id "
            + "      where exp.id = ?{1}";

    @Select(sql = SELECT_DSS_CODES_FOR_EXPERIMENT)
    public List<String> listDataStoreCodesForExperiment(long experimentId);

}
