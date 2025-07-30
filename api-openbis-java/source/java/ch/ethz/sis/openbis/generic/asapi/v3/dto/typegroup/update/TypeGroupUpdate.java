/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.ObjectToString;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IMetaDataUpdateHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.FieldUpdateValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.IObjectUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.IUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.ListUpdateMapValues;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonObject("as.dto.typegroup.update.TypeGroupUpdate")
public class TypeGroupUpdate implements IUpdate, IObjectUpdate<ITypeGroupId>, IMetaDataUpdateHolder
{

    @JsonProperty
    private ITypeGroupId typeGroupId;

    @JsonProperty
    private FieldUpdateValue<String> name = new FieldUpdateValue<String>();

    @JsonProperty
    private ListUpdateMapValues metaData = new ListUpdateMapValues();

    @Override
    @JsonIgnore
    public ITypeGroupId getObjectId()
    {
        return getTypeGroupId();
    }

    @JsonIgnore
    public ITypeGroupId getTypeGroupId()
    {
        return typeGroupId;
    }

    @JsonIgnore
    public void setTypeGroupId(ITypeGroupId typeGroupId)
    {
        this.typeGroupId = typeGroupId;
    }

    @JsonIgnore
    public FieldUpdateValue<String> getName()
    {
        return name;
    }

    @JsonIgnore
    public void setName(String name)
    {
        this.name.setValue(name);
    }

    @JsonIgnore
    public ListUpdateMapValues getMetaData()
    {
        return metaData;
    }

    @Override
    public String toString()
    {
        return new ObjectToString(this).append("typeGroupId", typeGroupId).toString();
    }
}
