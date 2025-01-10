package ch.systemsx.cisd.dbmigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

public class SQLUtils
{

    public static void execute(final DataSource dataSource, final String sql, ParametersSetter parametersSetter) throws SQLException
    {
        try (final Connection connection = dataSource.getConnection(); final PreparedStatement statement = connection.prepareStatement(sql);)
        {
            parametersSetter.setParameters(statement);
            statement.execute();
        }
    }

    public static <T> List<T> queryList(final DataSource dataSource, final String sql, final ParametersSetter parametersSetter,
            final ResultsMapper<T> resultsMapper) throws SQLException
    {
        try (final Connection connection = dataSource.getConnection(); final PreparedStatement statement = connection.prepareStatement(sql);)
        {
            parametersSetter.setParameters(statement);
            ResultSet resultSet = statement.executeQuery();
            List<T> results = new ArrayList<>();
            while (resultSet.next())
            {
                T result = resultsMapper.mapRow(resultSet);
                results.add(result);
            }
            return results;
        }
    }

    public static <T> T queryObject(final DataSource dataSource, final String sql, final ParametersSetter parametersSetter,
            final ResultsMapper<T> resultsMapper) throws SQLException
    {
        List<T> results = queryList(dataSource, sql, parametersSetter, resultsMapper);
        if (results.isEmpty())
        {
            return null;
        } else if (results.size() == 1)
        {
            return results.get(0);
        } else
        {
            throw new RuntimeException("Expected query to return only one result but it returned " + results.size() + ". Query: " + sql);
        }
    }

    public interface ParametersSetter
    {

        void setParameters(PreparedStatement ps) throws SQLException;

    }

    public static class NoParametersSetter implements ParametersSetter
    {

        @Override public void setParameters(final PreparedStatement ps) throws SQLException
        {
            // do nothing
        }
    }

    public interface ResultsMapper<R>
    {

        R mapRow(ResultSet rs) throws SQLException;

    }

    public static class ToMapResultsMapper implements ResultsMapper<Map<String, Object>>
    {

        private List<String> columnNames;

        @Override public Map<String, Object> mapRow(final ResultSet rs) throws SQLException
        {
            List<String> columnNames = getColumnNames(rs);
            Map<String, Object> row = new HashMap<>();

            for (int i = 0; i < columnNames.size(); i++)
            {
                String columnName = columnNames.get(i);
                Object columnValue = rs.getObject(i + 1);
                row.put(columnName, columnValue);
            }

            return row;
        }

        private List<String> getColumnNames(ResultSet rs) throws SQLException
        {
            if (columnNames == null)
            {
                ResultSetMetaData metaData = rs.getMetaData();
                List<String> columnNames = new ArrayList<>();
                for (int i = 0; i < metaData.getColumnCount(); i++)
                {
                    columnNames.add(metaData.getColumnName(i + 1));
                }
                this.columnNames = columnNames;
            }

            return this.columnNames;
        }
    }

    public static class ToValueResultsMapper<T> implements ResultsMapper<T>
    {

        private final ToMapResultsMapper toMapResultsMapper = new ToMapResultsMapper();

        @Override public T mapRow(final ResultSet rs) throws SQLException
        {
            Map<String, Object> row = toMapResultsMapper.mapRow(rs);
            if (row.size() == 1)
            {
                return (T) row.values().iterator().next();
            } else
            {
                throw new RuntimeException("Expected query to return a single column but it returned " + row.size() + " columns.");
            }
        }
    }

}
