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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer;

/**
 * One AFS file, as listed for a SAMPLE/EXPERIMENT/DATA_SET owner in the resource list's {@code x:binaryData[@source='afs']} block.
 */
public class IncomingAfsFile
{
    private final String path;

    private final long length;

    private final String lastModified;

    private final String hash;

    public IncomingAfsFile(String path, long length, String lastModified, String hash)
    {
        this.path = path;
        this.length = length;
        this.lastModified = lastModified;
        this.hash = hash;
    }

    public String getPath()
    {
        return path;
    }

    public long getLength()
    {
        return length;
    }

    public String getLastModified()
    {
        return lastModified;
    }

    public String getHash()
    {
        return hash;
    }
}
