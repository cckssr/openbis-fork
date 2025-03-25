package ch.systemsx.cisd.openbis.dss.generic.shared.api.v1;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

public interface IDssService
{
    public FileInfoDssDTO[] listFilesForDataSet(String sessionToken,
            String dataSetCode, String path, boolean isRecursive) throws IOExceptionUnchecked, IllegalArgumentException;

    String getDownloadUrlForFileForDataSet(String sessionToken, String dataSetCode, String relativePath);
}
