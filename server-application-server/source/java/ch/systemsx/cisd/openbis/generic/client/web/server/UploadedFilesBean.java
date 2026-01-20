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
package ch.systemsx.cisd.openbis.generic.client.web.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.springframework.web.multipart.MultipartFile;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.common.spring.IUncheckedMultipartFile;
import ch.systemsx.cisd.openbis.common.spring.MultipartFileAdapter;
import ch.systemsx.cisd.openbis.generic.shared.ISessionWorkspaceProvider;

/**
 * A bean that contains the uploaded files.
 * 
 * @author Christian Ribeaud
 */
public final class UploadedFilesBean
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, UploadedFilesBean.class);

    private static final String CLASS_SIMPLE_NAME = UploadedFilesBean.class.getSimpleName();

    private List<IUncheckedMultipartFile> multipartFiles = new ArrayList<IUncheckedMultipartFile>();

    private final File createTempFile(String sessionToken, ISessionWorkspaceProvider sessionWorkspaceProvider) throws IOException
    {
        File tempFolder = sessionWorkspaceProvider.getSessionWorkspace(sessionToken);
        final File tempFile = File.createTempFile(CLASS_SIMPLE_NAME, null, tempFolder);
        tempFile.deleteOnExit();
        return tempFile;
    }

    public final void addMultipartFile(String sessionToken, final MultipartFile multipartFile, ISessionWorkspaceProvider sessionWorkspaceProvider)
    {
        addMultipartFile(sessionToken, multipartFile, sessionWorkspaceProvider, false);
    }

    public final void addMultipartFile(String sessionToken, final MultipartFile multipartFile, ISessionWorkspaceProvider sessionWorkspaceProvider, boolean keepOriginalFileName)
    {
        assert multipartFile != null : "Unspecified multipart file.";
        try
        {
            File tempFile = null;
            String originalFilename =
                    getOriginalFilename(multipartFile.getOriginalFilename(), false);
            if (keepOriginalFileName) {
                File sessionWorkspaceFolder = sessionWorkspaceProvider.getSessionWorkspace(sessionToken);
                tempFile = new File(sessionWorkspaceFolder, originalFilename);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                Files.createFile(tempFile.toPath());
                tempFile.deleteOnExit();
            } else {
                tempFile = createTempFile(sessionToken, sessionWorkspaceProvider);
            }
//            Files.createDirectories(Path.of(tempFile.getParent()));
//            multipartFile.transferTo(tempFile);

            Path target = tempFile.toPath();
            Files.createDirectories(target.getParent());

            try (InputStream in = multipartFile.getInputStream()) {
                Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            operationLog.info("Uploaded file '" + originalFilename + "' to session workspace");

            final FileMultipartFileAdapter multipartFileAdapter =
                    new FileMultipartFileAdapter(multipartFile, tempFile);
            multipartFiles.add(multipartFileAdapter);
        } catch (final IOException ex)
        {
            throw new IOExceptionUnchecked(ex);
        }
    }

    public String getOriginalFilename(String filename, boolean preserveFilename) {
        if (filename == null) {
            // Should never happen.
            return "";
        }
        if (preserveFilename) {
            // Do not try to strip off a path...
            return filename;
        }

        // Check for Unix-style path
        int unixSep = filename.lastIndexOf('/');
        // Check for Windows-style path
        int winSep = filename.lastIndexOf('\\');
        // Cut off at latest possible point
        int pos = Math.max(winSep, unixSep);
        if (pos != -1)  {
            // Any sort of path separator found...
            return filename.substring(pos + 1);
        }
        else {
            // A plain name
            return filename;
        }
    }

    public final Iterable<IUncheckedMultipartFile> iterable()
    {
        return multipartFiles;
    }

    /**
     * Returns the number of files uploaded.
     */
    public final int size()
    {
        return multipartFiles.size();
    }

    /**
     * Deletes the transferred files.
     */
    public final void deleteTransferredFiles()
    {
        for (final IUncheckedMultipartFile multipartFile : iterable())
        {
            ((FileMultipartFileAdapter) multipartFile).destFile.delete();
        }
    }

    //
    // Helper classes
    //

    private final static class FileMultipartFileAdapter extends MultipartFileAdapter
    {
        private final File destFile;

        FileMultipartFileAdapter(final MultipartFile multipartFile, final File destFile)
        {
            super(multipartFile);
            this.destFile = destFile;
        }

        //
        // MultipartFileAdapter
        //

        @Override
        public final byte[] getBytes()
        {
            try
            {
                return FileUtils.readFileToByteArray(destFile);
            } catch (final IOException ex)
            {
                throw new IOExceptionUnchecked(ex);
            }
        }

        @Override
        public final InputStream getInputStream()
        {
            try
            {
                return FileUtils.openInputStream(destFile);
            } catch (final IOException ex)
            {
                throw new IOExceptionUnchecked(ex);
            }
        }

        @Override
        public final void transferTo(final File dest)
        {
            throw new UnsupportedOperationException();
        }
    }
}
