/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.server.shuffling;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ServiceVersionHolder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO containing information about data set in a simple form.
 *
 * @author Izabela Adamczyk
 */
@Getter @Setter
public class SimpleDataSetInformationDTO implements Serializable, IDatasetLocation
{
    private static final long serialVersionUID = ServiceVersionHolder.VERSION;

    private String dataStoreCode;

    private String dataSetCode;

    private String dataSetShareId;

    private String dataSetLocation;

    private DataSetArchivingStatus status;

    private boolean isPresentInArchive;

    private Date registrationTimestamp;

    private Date modificationTimestamp;

    private Date immutableDataTimestamp;

    private Date accessTimestamp;

    private Long dataSetSize;

    private String dataSetType;

    private int speedHint;

    private String sampleCode;

    private String spaceCode;

    private String experimentCode;

    private String projectCode;

    private String dataStoreUrl;

    private boolean isStorageConfirmed;

    private Map<String, Integer> orderInContainers = new HashMap<String, Integer>();

    private boolean isH5Folders;

    private boolean isH5ArFolders;

    @Override
    public Integer getOrderInContainer(String containerDataSetCode)
    {
        return orderInContainers.get(containerDataSetCode);
    }

}
