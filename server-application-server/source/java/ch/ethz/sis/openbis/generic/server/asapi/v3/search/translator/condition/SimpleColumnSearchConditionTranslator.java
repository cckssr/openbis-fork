/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.asapi.v3.search.translator.condition;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractStringValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.StringFieldSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.mapper.TableMapper;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.translator.SearchCriteriaTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.translator.condition.utils.JoinInformation;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.translator.condition.utils.TranslatorUtils;

import java.util.List;
import java.util.Map;

import static ch.ethz.sis.openbis.generic.server.asapi.v3.search.translator.SQLLexemes.*;

public class SimpleColumnSearchConditionTranslator implements IConditionTranslator<StringFieldSearchCriteria>
{
    private final String columnName;

    public SimpleColumnSearchConditionTranslator(String column) {
        columnName = column;
    }

    @Override
    public Map<String, JoinInformation> getJoinInformationMap(
            StringFieldSearchCriteria criterion, TableMapper tableMapper,
            IAliasFactory aliasFactory)
    {
        return null;
    }

    @Override
    public void translate(StringFieldSearchCriteria criterion, TableMapper tableMapper,
            List<Object> args, StringBuilder sqlBuilder,
            Map<String, JoinInformation> aliases,
            Map<String, String> dataTypeByPropertyCode)
    {
        switch (criterion.getFieldType())
        {
            case ATTRIBUTE:
            {
                final AbstractStringValue value = criterion.getFieldValue();
                final boolean useWildcards = criterion.isUseWildcards();
                if (value != null && value.getValue() != null)
                {
                    final String stringValue = value.getValue();
                    translateSearchByCodeCondition(sqlBuilder, tableMapper, value.getClass(), stringValue, useWildcards,
                            args);
                } else
                {
                    sqlBuilder.append(SearchCriteriaTranslator.MAIN_TABLE_ALIAS).append(PERIOD).append(columnName)
                            .append(SP).append(IS_NOT_NULL);
                }
                break;
            }

            case PROPERTY:
            case ANY_PROPERTY:
            case ANY_FIELD:
            {
                throw new IllegalArgumentException();
            }
        }
    }

    void translateSearchByCodeCondition(final StringBuilder sqlBuilder, final TableMapper tableMapper,
            final Class<?> valueClass, final String stringValue, final boolean useWildcards,
            final List<Object> args)
    {
        sqlBuilder.append(SearchCriteriaTranslator.MAIN_TABLE_ALIAS).append(PERIOD)
                .append(columnName).append(SP);
        TranslatorUtils.appendStringComparatorOp(valueClass, stringValue,
                useWildcards, sqlBuilder, args);
    }
}
