/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;

/**
 * Writes the AFS files of a Sample or Experiment as an {@code x:binaryData} block nested inside that owner's own {@code x:xd}
 * element. AFS has no identity of its own (a file is only ever addressed as {@code owner+path} against the AFS server, never
 * independently registered), so unlike {@link DataSetDeliverer} its content is attached data on an already-existing owner, not a
 * synchronizable entity in its own right - it never gets its own {@code <url>} block.
 * <p>
 * Empty directories are delivered too, as {@code x:dirNode} elements, so the harvester can recreate them. A directory that
 * contains files doesn't need its own entry - the file paths already imply it exists.
 * <p>
 * Trashed content is delivered too, using its real path under {@code /.afs.trash/...}
 * <p>
 * Delivers nothing when {@code afs-url} isn't configured for this DataSource.
 */
class AfsDataWriter
{
    private final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, getClass());

    private final URI afsServerUri;

    AfsDataWriter(DeliveryContext context)
    {
        String afsUrl = context.getAfsUrl();
        afsServerUri = (afsUrl == null || afsUrl.isBlank()) ? null : URI.create(afsUrl);
    }

    void write(XMLStreamWriter writer, String ownerPermId, String sessionToken) throws XMLStreamException
    {
        if (afsServerUri == null)
        {
            return;
        }
        AfsClient afsClient = new AfsClient(afsServerUri);
        afsClient.setSessionToken(sessionToken);
        // one recursive listing covers both live content and .afs.trash, so directory emptiness can be checked
        // against everything at once
        List<File> files = new ArrayList<>();
        List<File> directories = new ArrayList<>();
        if (list(afsClient, ownerPermId, files, directories) == false)
        {
            return;
        }
        List<File> emptyDirectories = emptyDirectoriesAmong(directories, files);
        if (files.isEmpty() && emptyDirectories.isEmpty())
        {
            return;
        }
        writer.writeStartElement("x:binaryData");
        writer.writeAttribute("source", "afs");
        for (File file : files)
        {
            writer.writeStartElement("x:fileNode");
            writer.writeAttribute("path", file.getPath());
            writer.writeAttribute("length", Long.toString(file.getSize()));
            writer.writeAttribute("lastModified", file.getLastModifiedTime().toString());
            writer.writeAttribute("hash", hash(afsClient, ownerPermId, file.getPath()));
            writer.writeEndElement();
        }
        for (File directory : emptyDirectories)
        {
            writer.writeStartElement("x:dirNode");
            writer.writeAttribute("path", directory.getPath());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Lists everything under the owner's root, live and trashed alike, split into files and directories. Returns
     * {@code false} if the owner has no AFS store yet.
     */
    private boolean list(AfsClient afsClient, String ownerPermId, List<File> files, List<File> directories)
    {
        File[] entries;
        try
        {
            entries = afsClient.list(ownerPermId, "/", true);
        } catch (Exception e)
        {
            if (AfsClientUploadHelper.isPathNotInStoreError(e))
            {
                return false;
            }
            throw wrap(ownerPermId, e);
        }
        for (File entry : entries)
        {
            // The trash root itself is internal; snapshot content is part of the delivered state.
            if (entry.getPath().equals(TRASH_ROOT_PATH) || entry.getPath().startsWith(BACKUP_PATH_PREFIX))
            {
                continue;
            }
            if (Boolean.TRUE.equals(entry.getDirectory()))
            {
                directories.add(entry);
            } else
            {
                files.add(entry);
            }
        }
        return true;
    }

    // same value as ch.ethz.sis.afs.manager.TrashRootProvider.TRASH_FOLDER_NAME; duplicated here so we don't need a
    // dependency on lib-transactional-file-system just for this one constant
    private static final String TRASH_ROOT_PATH = "/.afs.trash";

    private static final String SNAPSHOTS_DIRECTORY = ".afs.snapshots";

    private static final String BACKUP_PATH_PREFIX = "/.afs-sync-backup-";

    /**
     * Returns the directories that have no file anywhere below them. Only these need to be listed explicitly - a file's
     * path already implies every directory above it exists.
     */
    static List<File> emptyDirectoriesAmong(List<File> directories, List<File> files)
    {
        List<File> empty = new ArrayList<>();
        for (File directory : directories)
        {
            if (isInSnapshots(directory.getPath()) == false && hasDescendantFile(directory.getPath(), files) == false)
            {
                empty.add(directory);
            }
        }
        return empty;
    }

    private static boolean isInSnapshots(String path)
    {
        return path.contains("/" + SNAPSHOTS_DIRECTORY + "/") || path.endsWith("/" + SNAPSHOTS_DIRECTORY);
    }

    private static boolean hasDescendantFile(String directoryPath, List<File> dataFiles)
    {
        String prefix = directoryPath.endsWith("/") ? directoryPath : directoryPath + "/";
        for (File file : dataFiles)
        {
            if (file.getPath().startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    private String hash(AfsClient afsClient, String ownerPermId, String path)
    {
        try
        {
            return afsClient.hash(ownerPermId, path);
        } catch (Exception e)
        {
            throw wrap(ownerPermId, e);
        }
    }

    private RuntimeException wrap(String ownerPermId, Exception e)
    {
        operationLog.error("Could not deliver AFS data for owner " + ownerPermId, e);
        return new RuntimeException("Could not deliver AFS data for owner " + ownerPermId, e);
    }

}
