package ch.ethz.sis.pathinfo;

import java.util.List;

import net.lemnik.eodsql.BaseQuery;
import net.lemnik.eodsql.Select;

public interface IPathInfoAutoClosingDAO extends BaseQuery
{

    static String SELECT_DATA_SET_FILES =
            "SELECT id, parent_id, relative_path, file_name, size_in_bytes, checksum_crc32, checksum, "
                    + "is_directory, last_modified FROM data_set_files ";

    static String SELECT_DATA_SET_FILES_WITH_DATA_SET_INFO =
            "SELECT f.id, f.dase_id, f.parent_id, f.relative_path, f.file_name, f.size_in_bytes, "
                    + "f.checksum_crc32, f.checksum, f.is_directory, f.last_modified, s.code "
                    + " FROM data_set_files f LEFT JOIN data_sets s ON (s.id = f.dase_id) ";

    @Select("SELECT id FROM data_sets WHERE code = ?{1}")
    public Long tryToGetDataSetId(String dataSetCode);

    @Select(SELECT_DATA_SET_FILES + "WHERE dase_id = ?{1}")
    public List<DataSetFileRecord> listDataSetFiles(long dataSetId);

    @Select(SELECT_DATA_SET_FILES + "WHERE dase_id = ?{1} AND parent_id is null")
    public DataSetFileRecord getDataSetRootFile(long dataSetId);

    @Select(SELECT_DATA_SET_FILES + "WHERE dase_id = ?{1} AND relative_path = ?{2}")
    public DataSetFileRecord tryToGetRelativeDataSetFile(long dataSetId, String relativePath);

    @Select(SELECT_DATA_SET_FILES + "WHERE dase_id = ?{1} AND parent_id = ?{2}")
    public List<DataSetFileRecord> listChildrenByParentId(long dataSetId, long parentId);

    @Select(SELECT_DATA_SET_FILES + "WHERE dase_id = ?{1} AND relative_path ~ ?{2}")
    public List<DataSetFileRecord> listDataSetFilesByRelativePathRegex(long dataSetId,
            String relativePathRegex);

    @Select(SELECT_DATA_SET_FILES + "WHERE dase_id = ?{1} AND relative_path LIKE ?{2}")
    public List<DataSetFileRecord> listDataSetFilesByRelativePathLikeExpression(long dataSetId,
            String relativePathLikeExpression);

    @Select(SELECT_DATA_SET_FILES_WITH_DATA_SET_INFO + "WHERE LOWER(relative_path) LIKE LOWER(?{1})")
    public List<ExtendedDataSetFileRecord> listFilesByRelativePathLikeExpression(String relativePathLikeExpression);

    @Select(SELECT_DATA_SET_FILES
            + "WHERE dase_id = ?{1} AND relative_path = ?{2} || file_name AND file_name ~ ?{3}")
    public List<DataSetFileRecord> listDataSetFilesByFilenameRegex(
            long dataSetId, String startingPath, String filenameRegex);

    @Select(SELECT_DATA_SET_FILES
            + "WHERE dase_id = ?{1} AND relative_path = ?{2} || file_name AND file_name LIKE ?{3}")
    public List<DataSetFileRecord> listDataSetFilesByFilenameLikeExpression(
            long dataSetId, String startingPath,
            String filenameLikeExpression);

}
