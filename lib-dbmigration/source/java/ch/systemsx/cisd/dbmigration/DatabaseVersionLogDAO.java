/*
 * Copyright ETH 2007 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.dbmigration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.common.db.Script;

/**
 * Class which logs database migration steps in the database.
 *
 * @author Franz-Josef Elmer
 */
public class DatabaseVersionLogDAO implements IDatabaseVersionLogDAO
{
    public static final String DB_VERSION_LOG = "database_version_logs";

    private static final String ENCODING = "utf8";

    private static final String RUN_EXCEPTION = "run_exception";

    private static final String MODULE_CODE = "module_code";

    private static final String RUN_STATUS_TIMESTAMP = "run_status_timestamp";

    private static final String RUN_STATUS = "run_status";

    private static final String MODULE_NAME = "module_name";

    private static final String DB_VERSION = "db_version";

    private static final String SELECT_LAST_ENTRY =
            "select * from " + DB_VERSION_LOG + " where " + RUN_STATUS_TIMESTAMP
                    + " in (select max(" + RUN_STATUS_TIMESTAMP + ") from " + DB_VERSION_LOG + ")";

    private static byte[] getAsByteArray(String string)
    {
        try
        {
            return string.getBytes(ENCODING);
        } catch (UnsupportedEncodingException ex)
        {
            throw new CheckedExceptionTunnel(ex);
        }
    }

    private final DataSource dataSource;

    public DatabaseVersionLogDAO(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public boolean canConnectToDatabase()
    {
        try
        {
            getLastEntry();
            return true;
        } catch (Exception ex)
        {
            return false;
        }
    }

    @Override
    public void createTable(Script script)
    {
        try
        {
            SQLUtils.execute(dataSource, script.getContent(), new SQLUtils.NoParametersSetter());
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LogEntry getLastEntry()
    {
        try
        {
            List<LogEntry> entries = SQLUtils.queryList(dataSource, SELECT_LAST_ENTRY, new SQLUtils.NoParametersSetter(), rs ->
            {
                final LogEntry logEntry = new LogEntry();
                logEntry.setVersion(rs.getString(DB_VERSION));
                logEntry.setModuleName(rs.getString(MODULE_NAME));
                logEntry.setRunStatus(rs.getString(RUN_STATUS));
                logEntry.setRunStatusTimestamp(rs.getDate(RUN_STATUS_TIMESTAMP));
                try
                {
                    final byte[] moduleCodeOrNull = rs.getBytes(MODULE_CODE);
                    final String moduleCodeString =
                            (moduleCodeOrNull != null) ? new String(moduleCodeOrNull, ENCODING) : "";
                    logEntry.setModuleCode(moduleCodeString);

                    final byte[] runExceptionOrNull = rs.getBytes(RUN_EXCEPTION);
                    final String runExceptionString =
                            (runExceptionOrNull != null) ? new String(runExceptionOrNull, ENCODING) : "";
                    logEntry.setRunException(runExceptionString);
                } catch (UnsupportedEncodingException ex)
                {
                    throw new CheckedExceptionTunnel(ex);
                }
                return logEntry;
            });

            return entries.size() == 0 ? null : entries.get(entries.size() - 1);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a new entry into the version log with {@link LogEntry.RunStatus#START}.
     *
     * @param moduleScript The script of the module to be logged.
     */
    @Override
    public void logStart(final Script moduleScript)
    {
        try
        {
            SQLUtils.execute(dataSource, "insert into " + DB_VERSION_LOG + " (" + DB_VERSION + "," + MODULE_NAME
                    + "," + RUN_STATUS + "," + RUN_STATUS_TIMESTAMP + "," + MODULE_CODE
                    + ") values (?,?,?,?,?)", ps ->
            {
                ps.setString(1, moduleScript.getVersion());
                ps.setString(2, moduleScript.getName());
                ps.setString(3, LogEntry.RunStatus.START.toString());
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.setBytes(5, getAsByteArray(moduleScript.getContent()));
            });
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update log entry specified by version and module name to {@link LogEntry.RunStatus#SUCCESS}.
     *
     * @param moduleScript The script of the successfully applied module.
     */
    @Override
    public void logSuccess(final Script moduleScript)
    {
        try
        {
            SQLUtils.execute(dataSource, "update " + DB_VERSION_LOG + " SET " + RUN_STATUS + " = ? , "
                    + RUN_STATUS_TIMESTAMP + " = ? " + "where " + DB_VERSION + " = ? and "
                    + MODULE_NAME + " = ?", ps ->
            {
                ps.setString(1, LogEntry.RunStatus.SUCCESS.toString());
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                ps.setString(3, moduleScript.getVersion());
                ps.setString(4, moduleScript.getName());
            });
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update log entry specified by version and module name to {@link LogEntry.RunStatus#FAILED}.
     *
     * @param moduleScript Script of the failed module.
     * @param runException Exception causing the failure.
     */
    @Override
    public void logFailure(final Script moduleScript, Throwable runException)
    {
        final StringWriter stringWriter = new StringWriter();
        runException.printStackTrace(new PrintWriter(stringWriter));

        try
        {
            SQLUtils.execute(dataSource, "update " + DB_VERSION_LOG + " SET " + RUN_STATUS + " = ?, "
                    + RUN_STATUS_TIMESTAMP + " = ?, " + RUN_EXCEPTION + " = ? where " + DB_VERSION
                    + " = ? and " + MODULE_NAME + " = ?", ps ->
            {
                ps.setString(1, LogEntry.RunStatus.FAILED.toString());
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                ps.setBytes(3, getAsByteArray(stringWriter.toString()));
                ps.setString(4, moduleScript.getVersion());
                ps.setString(5, moduleScript.getName());
            });
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

}
