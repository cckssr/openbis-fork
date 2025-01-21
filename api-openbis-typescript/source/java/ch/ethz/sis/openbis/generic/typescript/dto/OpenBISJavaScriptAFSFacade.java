package ch.ethz.sis.openbis.generic.typescript.dto;

import java.util.List;

import ch.ethz.sis.openbis.generic.typescript.TypeScriptMethod;
import ch.ethz.sis.openbis.generic.typescript.TypeScriptObject;
import ch.ethz.sis.openbis.generic.typescript.type.Blob;

@TypeScriptObject
public class OpenBISJavaScriptAFSFacade
{

    private OpenBISJavaScriptAFSFacade()
    {
    }

    @TypeScriptMethod(sessionToken = false)
    public List<File> list(final String owner, final String source, final boolean recursively)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    public Blob read(final String owner, final String source, final long offset, final int limit)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    public boolean write(final String owner, final String source, final long offset, final Object data)
    {
        return false;
    }

    @TypeScriptMethod(sessionToken = false)
    public boolean delete(final String owner, final String source)
    {
        return false;
    }

    @TypeScriptMethod(sessionToken = false)
    public boolean copy(final String sourceOwner, final String source, final String targetOwner, final String target)
    {
        return false;
    }

    @TypeScriptMethod(sessionToken = false)
    public boolean move(final String sourceOwner, final String source, final String targetOwner, final String target)
    {
        return false;
    }

    @TypeScriptMethod(sessionToken = false)
    public boolean create(final String owner, final String source, final boolean directory)
    {
        return false;
    }

    @TypeScriptMethod(sessionToken = false)
    public FreeSpace free(final String owner, final String source)
    {
        return null;
    }

    @TypeScriptObject
    public static class File
    {
        private String owner;

        private String path;

        private String name;

        private Boolean directory;

        private Long size;

        private String lastModifiedTime;

        public String getOwner()
        {
            return owner;
        }

        public String getPath()
        {
            return path;
        }

        public String getName()
        {
            return name;
        }

        public Boolean getDirectory()
        {
            return directory;
        }

        public Long getSize()
        {
            return size;
        }

        public String getLastModifiedTime()
        {
            return lastModifiedTime;
        }
    }

    @TypeScriptObject
    public static class FreeSpace
    {

        private Long total;

        private Long free;

        public Long getFree()
        {
            return free;
        }

        public Long getTotal()
        {
            return total;
        }

    }

}
