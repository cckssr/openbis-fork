package ch.ethz.sis.rdf.main.parser.repair;

import ch.ethz.sis.rdf.main.parser.ResourceParsingResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * This has been dummied out. In case you want to use it, provide a mechanism to set a file name and
 * a mechanism to pass the ResourceParsingResult, as well as a database. The table definitions are
 * included as fields. See BIS-2361 for more information.
 */
public class VocabRepairSqlite
{
    private static final String INSERT_SQL =
            "INSERT INTO vocab_fix(object_name, property_code, value) VALUES(?, ?, ?)";

    public void insert(List<ResourceParsingResult.VocabRepairInfo> infos)
    {

        String tableVocabFix =
                "CREATE TABLE IF NOT EXISTS vocab_fix  (vocab_fix_id INTEGER PRIMARY KEY, object_name TEXT NOT NULL, property_code TEXT NOT NULL, value TEXT NOT NULL, imported datetime);";
        String tableFile =
                "CREATE TABLE IF NOT EXISTS file (file_id INTEGER PRIMARY KEY, file_name TEXT NOT NULL, dt datetime default current_timestamp, UNIQUE(file_name))";

        Connection connection = null;
        try
        {
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:/cluster/work/swisspkcdw/rdf_kispi_dataset/kispi_all_ttls/out.sqlite");
            connection.setAutoCommit(false);
            PreparedStatement ps;
            ps = connection.prepareStatement(INSERT_SQL);

            for (ResourceParsingResult.VocabRepairInfo info : infos)
            {

                ps.setString(1, info.getSampleObject().name);
                ps.setString(2, info.getPropertyType().code);
                ps.setString(3, info.getValue());

                ps.addBatch();
            }
            String filename = "out.db";
            //String filename = Config.getINSTANCE().getFilename();

            PreparedStatement fileStatement =
                    connection.prepareStatement("INSERT INTO file(file_name) VALUES (?)");
            fileStatement.setString(1, filename);
            fileStatement.executeUpdate();
            ps.executeBatch();

            connection.commit();
        } catch (SQLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

}
