package ch.ethz.sis.pathinfo;

import java.util.Date;

public class DataSetFileRecord
{
    // Attribute names as defined in database schema
    public long id;

    public Long dase_id;

    public Long parent_id;

    public String relative_path;

    public String file_name;

    public long size_in_bytes;

    public Integer checksum_crc32;

    public String checksum;

    public boolean is_directory;

    public Date last_modified;
}
