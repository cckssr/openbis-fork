/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.pathinfo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import ch.systemsx.cisd.common.db.mapper.StringArrayMapper;
import net.lemnik.eodsql.Select;
import net.lemnik.eodsql.TransactionQuery;
import net.lemnik.eodsql.Update;

/**
 * Data Access Object for feeding and updating pathinfo database.
 * 
 * @author Franz-Josef Elmer
 */
public interface IPathInfoDAO extends TransactionQuery
{
    @Select("select id from data_sets where code = ?{1}")
    public Long tryGetDataSetId(String code);

    @Select("insert into data_sets (code, location) values (?{1}, ?{2}) returning id")
    public long createDataSet(String code, String location);

    @Update("delete from data_sets where code = ?{1}")
    public void deleteDataSet(String code);

    @Select("insert into data_set_files (dase_id, parent_id, relative_path, file_name, "
            + "size_in_bytes, is_directory, checksum_crc32, checksum, last_modified) "
            + "values (?{1}, ?{2}, ?{3}, ?{4}, ?{5}, ?{6}, ?{7}, ?{8}, ?{9}) returning id")
    public long createDataSetFile(long dataSetId, Long parentId, String relativePath,
            String fileName, long sizeInBytes, boolean directory, Integer checksumCRC32, String checksum, Date lastModifiedDate);

    @Select("select last_seen_timestamp from last_feeding_event where data_store_kind = ?{1}")
    public Date getLastSeenTimestamp(String dataStoreKind);

    @Update("delete from last_feeding_event where data_store_kind = ?{1}")
    public void deleteLastSeenTimestamp(String dataStoreKind);

    @Update("insert into last_feeding_event (last_seen_timestamp, data_store_kind) values (?{1}, ?{2})")
    public void createLastSeenTimestamp(Date registrationTimestamp, String dataStoreKind);

    @Update(sql = "insert into data_set_files (dase_id, parent_id, relative_path, file_name, "
            + "size_in_bytes, checksum_crc32, checksum, is_directory, last_modified) values "
            + "(?{1.dataSetId}, ?{1.parentId}, ?{1.relativePath}, ?{1.fileName}, ?{1.sizeInBytes}, "
            + "?{1.checksumCRC32}, ?{1.checksum}, ?{1.directory}, ?{1.lastModifiedDate})", batchUpdate = true)
    public void createDataSetFiles(Collection<PathEntryDTO> filePaths);

    @Select("select f.id, d.code as data_set_code, relative_path " +
            "from data_set_files as f join data_sets as d on f.dase_id = d.id " +
            "where checksum_crc32 is null and is_directory = 'F'")
    public List<PathEntryDTO> listDataSetFilesWithUnkownChecksum();

    @Update("update data_set_files set checksum_crc32 = ?{2}, checksum = ?{3} where id = ?{1}")
    public void updateChecksum(long id, int checksumCRC32, String checksum);

    @Select(sql = "select d.code as data_set_code, size_in_bytes " +
            "from data_set_files as f join data_sets as d on f.dase_id = d.id " +
            "where d.code = any(?{1}) and parent_id is null", parameterBindings = { StringArrayMapper.class })
    public List<PathEntryDTO> listDataSetsSize(String[] dataSetCodes);

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
