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

package ch.ethz.sis.openbis.generic.asapi.v3.typegroup.create;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.ObjectToString;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.create.ICreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.create.IObjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.ITypeGroupId;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonObject("as.dto.typegroup.create.TypeGroupAssignmentCreation")
public class TypeGroupAssignmentCreation implements ICreation, IObjectCreation
{
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private IEntityTypeId sampleTypeId;

    @JsonProperty
    private ITypeGroupId typeGroupId;

    @JsonProperty
    private Boolean managedInternally;

    @JsonIgnore
    public IEntityTypeId getSampleTypeId()
    {
        return sampleTypeId;
    }

    public void setSampleTypeId(
            IEntityTypeId sampleTypeId)
    {
        this.sampleTypeId = sampleTypeId;
    }

    @JsonIgnore
    public ITypeGroupId getTypeGroupId()
    {
        return typeGroupId;
    }

    public void setTypeGroupId(ITypeGroupId typeGroupId)
    {
        this.typeGroupId = typeGroupId;
    }

    @JsonIgnore
    public Boolean isManagedInternally()
    {
        return managedInternally;
    }

    public void setManagedInternally(Boolean managedInternally)
    {
        this.managedInternally = managedInternally;
    }

    @Override
    public String toString()
    {
        return new ObjectToString(this).append("sampleTypeId", sampleTypeId)
                .append("typeGroupId", typeGroupId)
                .append("managedInternally", managedInternally).toString();
    }
}
