/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.dbmigration.h2;

import static ch.systemsx.cisd.dbmigration.MassUploadFileType.TSV;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.common.db.ISequenceNameMapper;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.dbmigration.IMassUploader;
import ch.systemsx.cisd.dbmigration.SQLUtils;

/**
 * A {@link IMassUploader} for the H2 database.
 *
 * @author Bernd Rinn
 */
public class H2MassUploader implements IMassUploader
{
    private static final Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, H2MassUploader.class);

    private final ISequenceNameMapper sequenceNameMapper;

    private final DataSource dataSource;

    /**
     * Creates an instance for the specified data source and sequence mapper.
     */
    public H2MassUploader(final DataSource dataSource, final ISequenceNameMapper sequenceNameMapper)
            throws SQLException
    {
        this.sequenceNameMapper = sequenceNameMapper;
        this.dataSource = dataSource;
    }

    private final static class MassUploadRecord
    {
        final File massUploadFile;

        final String data;

        final String[] columnNamesOrNull;

        final String tableName;

        final BitSet isBinaryColumn;

        MassUploadRecord(final File massUploadFile, final String tableName,
                final BitSet isBinaryColumn)
        {
            this.massUploadFile = massUploadFile;
            this.data = null;
            this.columnNamesOrNull = null;
            this.tableName = tableName;
            this.isBinaryColumn = isBinaryColumn;
        }

        MassUploadRecord(final String data, final String tableName, final BitSet isBinaryColumn)
        {
            this(data, tableName, null, isBinaryColumn);
        }

        MassUploadRecord(final String data, final String tableName,
                final String[] columnNamesOrNull, final BitSet isBinaryColumn)
        {
            this.massUploadFile = null;
            this.data = data;
            this.columnNamesOrNull = columnNamesOrNull;
            this.tableName = tableName;
            this.isBinaryColumn = isBinaryColumn;
        }
    }

    @Override
    public void performMassUpload(String tableName, String data)
    {
        try
        {
            final DatabaseMetaData dbMetaData = dataSource.getConnection().getMetaData();
            try
            {
                final BitSet isBinaryColumn = findBinaryColumns(dbMetaData, tableName);
                final MassUploadRecord massUploadRecord =
                        new MassUploadRecord(data, tableName, isBinaryColumn);
                performMassUpload(massUploadRecord);
            } finally
            {
                dbMetaData.getConnection().close();
            }
        } catch (Exception ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    @Override
    public void performMassUpload(String tableName, String[] columnNames, String data)
    {
        try
        {
            final DatabaseMetaData dbMetaData = dataSource.getConnection().getMetaData();
            try
            {
                final BitSet isBinaryColumn = findBinaryColumns(dbMetaData, tableName, columnNames);
                final MassUploadRecord massUploadRecord =
                        new MassUploadRecord(data, tableName, isBinaryColumn);
                performMassUpload(massUploadRecord);
            } finally
            {
                dbMetaData.getConnection().close();
            }
        } catch (Exception ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    @Override
    public final void performMassUpload(final File[] massUploadFiles)
    {
        String task = "Get database metadata";
        try
        {
            final List<MassUploadRecord> massUploadRecords =
                    new ArrayList<MassUploadRecord>(massUploadFiles.length);
            final DatabaseMetaData dbMetaData = dataSource.getConnection().getMetaData();
            try
            {
                for (final File massUploadFile : massUploadFiles)
                {
                    final String[] splitName = StringUtils.split(massUploadFile.getName(), "=");
                    assert splitName.length == 2 : "Missing '=' in name of file '"
                            + massUploadFile.getName() + "'.";
                    final String tableNameWithExtension = splitName[1];
                    final boolean tsvFileType = TSV.isOfType(tableNameWithExtension);
                    assert tsvFileType : "Not a " + TSV.getFileType() + " file: "
                            + massUploadFile.getName();
                    final String tableName =
                            tableNameWithExtension.substring(0, tableNameWithExtension
                                    .lastIndexOf('.'));
                    final BitSet isBinaryColumn = findBinaryColumns(dbMetaData, tableName);
                    massUploadRecords.add(new MassUploadRecord(massUploadFile, tableName,
                            isBinaryColumn));
                }
            } finally
            {
                task = "Close connection";
                dbMetaData.getConnection().close();
            }
            for (final MassUploadRecord record : massUploadRecords)
            {
                performMassUpload(record);
            }
            for (final MassUploadRecord record : massUploadRecords)
            {
                fixSequence(record.tableName);
            }
        } catch (final SQLException ex)
        {
            throw new RuntimeException("Task '" + task + "' failed.", ex);
        }
    }

    private final void performMassUpload(final MassUploadRecord record)
    {
        try
        {
            if (operationLog.isInfoEnabled())
            {
                operationLog.info("Perform mass upload of file '" + record.massUploadFile
                        + "' to table '" + record.tableName + "'.");
            }
            final List<String[]> rows =
                    (record.data == null) ? readTSVFile(record.massUploadFile)
                            : readTSVFile(record.data);
            final int numberOfRows = rows.size();
            final int numberOfColumns;
            if (record.columnNamesOrNull != null)
            {
                numberOfColumns = record.columnNamesOrNull.length;
            } else
            {
                numberOfColumns = (numberOfRows > 0) ? rows.get(0).length : 0;
            }
            final StringBuilder insertSql = new StringBuilder();
            insertSql.append("insert into ");
            insertSql.append(record.tableName);
            if (record.columnNamesOrNull != null)
            {
                insertSql.append(" (");
                for (int i = 0; i < record.columnNamesOrNull.length; ++i)
                {
                    insertSql.append(record.columnNamesOrNull[i]);
                    if (i < record.columnNamesOrNull.length - 1)
                    {
                        insertSql.append(',');
                    }
                }
                insertSql.append(')');
            }
            insertSql.append(" values (");
            for (int i = 0; i < numberOfColumns; i++)
            {
                insertSql.append("?,");
            }
            insertSql.setLength(insertSql.length() - 1);
            insertSql.append(')');

            for (String[] row : rows)
            {
                SQLUtils.execute(dataSource, insertSql.toString(), new SQLUtils.ParametersSetter()
                {
                    @Override public void setParameters(final PreparedStatement ps) throws SQLException
                    {
                        {
                            for (int colNo = 0; colNo < numberOfColumns; ++colNo)
                            {
                                ps.setObject(colNo + 1, tryGetValue(row, colNo));
                            }
                        }
                    }

                    private Object tryGetValue(final String[] row, final int colNo)
                    {
                        final String stringValueOrNull = row[colNo];
                        if (stringValueOrNull == null)
                        {
                            return null;
                        }
                        if (record.isBinaryColumn.get(colNo))
                        {
                            return stringValueOrNull.getBytes();
                        } else
                        {
                            return stringValueOrNull;
                        }
                    }
                });
            }
        } catch (final Exception ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    private final BitSet findBinaryColumns(final DatabaseMetaData dbMetaData, final String tableName)
            throws SQLException
    {
        final BitSet binary = new BitSet();
        final ResultSet rs = dbMetaData.getColumns(null, null, tableName.toUpperCase(), "%");
        int columnNo = 0;
        while (rs.next())
        {
            final int typeCode = rs.getInt(5);
            binary.set(columnNo, typeCode == Types.BINARY || typeCode == Types.VARBINARY);
            ++columnNo;
        }
        rs.close();
        return binary;
    }

    private final BitSet findBinaryColumns(final DatabaseMetaData dbMetaData,
            final String tableName, final String[] columnNames) throws SQLException
    {
        final ResultSet rs = dbMetaData.getColumns(null, null, tableName.toUpperCase(), "%");
        final Map<String, Integer> typeMap = new HashMap<String, Integer>();
        while (rs.next())
        {
            final String colName = rs.getString(4);
            final int typeCode = rs.getInt(5);
            typeMap.put(colName, typeCode);
        }
        rs.close();
        final BitSet binary = new BitSet();
        for (int i = 0; i < columnNames.length; ++i)
        {
            final Integer typeCode = typeMap.get(columnNames[i]);
            if (typeCode != null)
            {
                binary.set(i, typeCode == Types.BINARY || typeCode == Types.VARBINARY);
            }
        }
        return binary;
    }

    private final List<String[]> readTSVFile(final String data) throws IOException
    {
        final BufferedReader reader = new BufferedReader(new StringReader(data));
        return readTSVFile(reader);
    }

    private final List<String[]> readTSVFile(final File tsvFile) throws IOException
    {
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(FileUtils.openInputStream(tsvFile)));
        return readTSVFile(reader);
    }

    private final List<String[]> readTSVFile(final BufferedReader reader) throws IOException
    {
        final List<String[]> result = new ArrayList<String[]>();
        try
        {
            String line = reader.readLine();
            int numberOfColumns = -1;
            while (line != null)
            {
                if (line.trim().length() > 0)
                {
                    final String[] cols = StringUtils.splitPreserveAllTokens(line, '\t');
                    if (numberOfColumns < 0)
                    {
                        numberOfColumns = cols.length;
                    }
                    if (numberOfColumns != cols.length)
                    {
                        throw new IllegalArgumentException("line '" + line + "', cols found: "
                                + cols.length + ", cols expected: " + numberOfColumns);
                    }
                    for (int i = 0; i < cols.length; ++i)
                    {
                        cols[i] = StringUtils.replace(cols[i], "\\\\011", "\t");
                        cols[i] = StringUtils.replace(cols[i], "\\\\012", "\n");
                        if ("\\N".equals(cols[i]))
                        {
                            cols[i] = null;
                        }
                    }
                    result.add(cols);
                }
                line = reader.readLine();
            }
        } finally
        {
            IOUtils.closeQuietly(reader);
        }
        return result;
    }

    private final void fixSequence(final String tableName)
    {
        final String sequenceName = sequenceNameMapper.getSequencerForTable(tableName);
        if (sequenceName == null)
        {
            return;
        }
        try
        {
            final long maxId =
                    SQLUtils.queryObject(dataSource,
                            String.format("select max(id) from %s", tableName), new SQLUtils.NoParametersSetter(),
                            new SQLUtils.ToValueResultsMapper<>());
            final long newSequenceValue = maxId + 1;
            if (operationLog.isInfoEnabled())
            {
                operationLog.info("Updating sequence " + sequenceName + " for table " + tableName
                        + " to value " + newSequenceValue);
            }
            SQLUtils.execute(dataSource,
                    String.format("alter sequence %s restart with %d", sequenceName,
                            newSequenceValue), new SQLUtils.NoParametersSetter());
        } catch (final SQLException ex)
        {
            operationLog.error("Failed to set new value for sequence '" + sequenceName
                    + "' of table '" + tableName + "'.", ex);
        }
    }

}
