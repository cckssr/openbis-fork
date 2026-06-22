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
package ch.ethz.sis.openbis.generic.server.asapi.v3.search.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.sis.openbis.generic.server.asapi.v3.search.PSQLTypes;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

public abstract class AbstractSQLExecutor implements ISQLExecutor
{

    private static final Logger OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, AbstractSQLExecutor.class);

    private static final Map<Class<?>, PSQLTypes> TYPE_CONVERSION_MAP = new HashMap<>();

    static
    {
        TYPE_CONVERSION_MAP.put(Boolean.class, PSQLTypes.BOOLEAN);

        TYPE_CONVERSION_MAP.put(Character.class, PSQLTypes.CHARACTER);
        TYPE_CONVERSION_MAP.put(String.class, PSQLTypes.VARCHAR);

        TYPE_CONVERSION_MAP.put(Double.class, PSQLTypes.FLOAT8);
        TYPE_CONVERSION_MAP.put(Float.class, PSQLTypes.FLOAT4);

        TYPE_CONVERSION_MAP.put(Long.class, PSQLTypes.INT8);
        TYPE_CONVERSION_MAP.put(Integer.class, PSQLTypes.INT4);
        TYPE_CONVERSION_MAP.put(Short.class, PSQLTypes.INT2);
        TYPE_CONVERSION_MAP.put(Byte.class, PSQLTypes.INT2);
    }

    public abstract Connection getConnection();

    @Override
    public List<Map<String, Object>> execute(final String sqlQuery, final List<Object> args)
    {
        if (OPERATION_LOG.isDebugEnabled())
        {
            OPERATION_LOG.debug("QUERY: " + sqlQuery);
        }

        if (OPERATION_LOG.isTraceEnabled())
        {
            OPERATION_LOG.trace("ARGS: " + Arrays.deepToString(args.toArray()));
        }

        final List<Map<String, Object>> results = new ArrayList<>(1000);

        try (final PreparedStatement preparedStatement = getConnection().prepareStatement(sqlQuery))
        {
            setArgsForPreparedStatement(args, preparedStatement);

            preparedStatement.setFetchSize(10_000);

            try (final ResultSet resultSet = preparedStatement.executeQuery())
            {
                final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                final int columnCount = resultSetMetaData.getColumnCount();

                // optimization
                if (columnCount == 1)
                {
                    String columnName = resultSetMetaData.getColumnName(1);
                    boolean isLongCompatibleColumn =
                            Set.of(Types.BIGINT, Types.INTEGER, Types.SMALLINT, Types.TINYINT).contains(resultSetMetaData.getColumnType(1));

                    // optimization
                    if (isLongCompatibleColumn)
                    {
                        while (resultSet.next())
                        {
                            results.add(new SingleEntryMap<>(columnName, resultSet.getLong(1)));
                        }
                    } else
                    {
                        while (resultSet.next())
                        {
                            results.add(new SingleEntryMap<>(columnName, resultSet.getObject(1)));
                        }
                    }
                } else
                {
                    final List<String> columnNames = new ArrayList<>(columnCount);
                    for (int index = 0; index < columnCount; index++)
                    {
                        columnNames.add(resultSetMetaData.getColumnName(index + 1));
                    }

                    while (resultSet.next())
                    {
                        final Map<String, Object> row = new HashMap<>(columnCount * 2, 0.5f);
                        for (int index = 0; index < columnCount; index++)
                        {
                            row.put(columnNames.get(index), resultSet.getObject(index + 1));
                        }
                        results.add(row);
                    }
                }
            }
        } catch (SQLException ex)
        {
            throw new RuntimeException(ex);
        }

        if (OPERATION_LOG.isDebugEnabled())
        {
            OPERATION_LOG.debug("RESULTS COUNT: " + results.size());
        }

        if (OPERATION_LOG.isTraceEnabled())
        {
            OPERATION_LOG.trace("RESULTS: " + results);
        }

        return results;
    }

    private void setArgsForPreparedStatement(final List<Object> args, final PreparedStatement preparedStatement) throws SQLException
    {
        for (int index = 0; index < args.size(); index++)
        {
            final Object object = args.get(index);
            if (object != null && object.getClass().isArray())
            {
                final Object[] objectArray = (Object[]) object;
                final Class<?> arrayObjectType = object.getClass().getComponentType();
                final PSQLTypes psqlType = TYPE_CONVERSION_MAP.get(arrayObjectType);

                if (psqlType == null)
                {
                    throw new IllegalArgumentException("JDBCSQLExecutor don't support arrays of type: " + object.getClass().getName()
                            + " - With elements of type: " + arrayObjectType.getName() + " - Data: " + Arrays.toString(objectArray));
                }

                preparedStatement.setArray(index + 1, preparedStatement.getConnection().createArrayOf(psqlType.toString(), objectArray));
            } else if (object instanceof Date)
            {
                final Date date = (Date) object;
                preparedStatement.setTimestamp(index + 1, new Timestamp(date.getTime()));
            } else if (object instanceof Calendar)
            {
                final Calendar calendar = (Calendar) object;
                preparedStatement.setTimestamp(index + 1, new Timestamp(calendar.getTimeInMillis()), calendar);
            } else
            {
                preparedStatement.setObject(index + 1, object);
            }
        }
    }

    private static final class SingleEntryMap<K, V> extends AbstractMap<K, V>
    {
        private final K key;

        private final V value;

        private final Entry<K, V> entry;

        private final Set<Entry<K, V>> entrySet;

        public SingleEntryMap(K key, V value)
        {
            this.key = key;
            this.value = value;
            this.entry = new SimpleImmutableEntry<>(key, value);
            this.entrySet = Collections.singleton(entry);
        }

        @Override
        public V get(Object key)
        {
            return this.key.equals(key) ? value : null;
        }

        @Override
        public boolean containsKey(Object key)
        {
            return this.key.equals(key);
        }

        @Override
        public int size()
        {
            return 1;
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }

        @Override
        public Set<Entry<K, V>> entrySet()
        {
            return entrySet;
        }
    }

}
