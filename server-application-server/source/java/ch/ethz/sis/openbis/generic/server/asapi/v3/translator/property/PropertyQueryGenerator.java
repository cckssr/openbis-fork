/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.translator.property;

import org.apache.commons.lang3.StringUtils;

/**
 * @author pkupczyk
 */
public class PropertyQueryGenerator
{

    public static void main(String[] args)
    {
        createExperimentPropertyQuery();
        createExperimentPropertyHistoryQuery();
        createSamplePropertyQuery();
        createSamplePropertyHistoryQuery();
        createDataSetPropertyQuery();
        createDataSetPropertyHistoryQuery();
    }

    private static void createExperimentPropertyQuery()
    {
        PropertyQueryParams params = new PropertyQueryParams();
        params.propertyTable = "experiment_properties";
        params.propertyTableEntityIdColumn = "expe_id";
        params.propertyTableEntityTypePropertyTypeIdColumn = "etpt_id";
        params.entityTypePropertyTypeTable = "experiment_type_property_types";
        System.out.println("Experiment property: \n" + createPropertyQuery(params));
    }

    private static void createExperimentPropertyHistoryQuery()
    {
        PropertyHistoryQueryParams params = new PropertyHistoryQueryParams();
        params.propertyHistoryTable = "experiment_properties_history";
        params.propertyHistoryTableEntityIdColumn = "expe_id";
        params.propertyHistoryTableEntityTypePropertyTypeIdColumn = "etpt_id";
        params.entityTypePropertyTypeTable = "experiment_type_property_types";
        System.out.println("Experiment property history: \n" + createPropertyHistoryQuery(params));
    }

    private static void createSamplePropertyQuery()
    {
        PropertyQueryParams params = new PropertyQueryParams();
        params.propertyTable = "sample_properties";
        params.propertyTableEntityIdColumn = "samp_id";
        params.propertyTableEntityTypePropertyTypeIdColumn = "stpt_id";
        params.entityTypePropertyTypeTable = "sample_type_property_types";
        System.out.println("Sample property: \n" + createPropertyQuery(params));
    }

    private static void createSamplePropertyHistoryQuery()
    {
        PropertyHistoryQueryParams params = new PropertyHistoryQueryParams();
        params.propertyHistoryTable = "sample_properties_history";
        params.propertyHistoryTableEntityIdColumn = "samp_id";
        params.propertyHistoryTableEntityTypePropertyTypeIdColumn = "stpt_id";
        params.entityTypePropertyTypeTable = "sample_type_property_types";
        System.out.println("Sample property history: \n" + createPropertyHistoryQuery(params));
    }

    private static void createDataSetPropertyQuery()
    {
        PropertyQueryParams params = new PropertyQueryParams();
        params.propertyTable = "data_set_properties";
        params.propertyTableEntityIdColumn = "ds_id";
        params.propertyTableEntityTypePropertyTypeIdColumn = "dstpt_id";
        params.entityTypePropertyTypeTable = "data_set_type_property_types";
        System.out.println("DataSet property: \n" + createPropertyQuery(params));
    }

    private static void createDataSetPropertyHistoryQuery()
    {
        PropertyHistoryQueryParams params = new PropertyHistoryQueryParams();
        params.propertyHistoryTable = "data_set_properties_history";
        params.propertyHistoryTableEntityIdColumn = "ds_id";
        params.propertyHistoryTableEntityTypePropertyTypeIdColumn = "dstpt_id";
        params.entityTypePropertyTypeTable = "data_set_type_property_types";
        System.out.println("DataSet property history: \n" + createPropertyHistoryQuery(params));
    }

    public static String javize(String query)
    {

        StringBuilder sb = new StringBuilder();

        for (String line : StringUtils.trim(query).split("\\n"))
        {
            if (line.isEmpty())
            {
                continue;
            }

            sb.append("+ \"");
            sb.append(line);
            sb.append("\"\n");
        }

        return sb.toString().substring(2);
    }

    public static String createPropertyQuery(PropertyQueryParams params)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("p." + params.propertyTableEntityIdColumn + " as objectId, ");
        sb.append("pt.code as propertyCode, ");
        sb.append("p.value as propertyValue, ");
        sb.append("cvt.code as vocabularyPropertyValue \n");
        sb.append("from ");
        sb.append(params.propertyTable + " p \n");
        sb.append("left join controlled_vocabulary_terms cvt on p.cvte_id = cvt.id \n");
        sb.append("join " + params.entityTypePropertyTypeTable + " etpt on p." + params.propertyTableEntityTypePropertyTypeIdColumn
                + " = etpt.id \n");
        sb.append("join property_types pt on etpt.prty_id = pt.id \n");
        sb.append("where p." + params.propertyTableEntityIdColumn + " = any(?{1})\n");

        return javize(sb.toString());
    }

    public static String createPropertyHistoryQuery(PropertyHistoryQueryParams params)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("ph." + params.propertyHistoryTableEntityIdColumn + " as objectId, ");
        sb.append("ph.pers_id_author as authorId, ");
        sb.append("pt.code as propertyCode, ");
        sb.append("ph.value as propertyValue, ");
        sb.append("ph.vocabulary_term as vocabularyPropertyValue, ");
        sb.append("ph.valid_from_timestamp as validFrom, ");
        sb.append("ph.valid_until_timestamp as validTo \n");
        sb.append("from ");
        sb.append(params.propertyHistoryTable + " ph \n");
        sb.append("join " + params.entityTypePropertyTypeTable + " etpt on ph." + params.propertyHistoryTableEntityTypePropertyTypeIdColumn
                + " = etpt.id \n");
        sb.append("join property_types pt on etpt.prty_id = pt.id \n");
        sb.append("where ph." + params.propertyHistoryTableEntityIdColumn + " = any(?{1})\n");
        return javize(sb.toString());
    }

    public static class PropertyQueryParams
    {
        public String propertyTable;

        public String propertyTableEntityIdColumn;

        public String propertyTableEntityTypePropertyTypeIdColumn;

        public String entityTypePropertyTypeTable;

    }

    public static class PropertyHistoryQueryParams
    {
        public String propertyHistoryTable;

        public String propertyHistoryTableEntityIdColumn;

        public String propertyHistoryTableEntityTypePropertyTypeIdColumn;

        public String entityTypePropertyTypeTable;
    }

}
