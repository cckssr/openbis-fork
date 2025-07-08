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

package ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonObject("as.dto.typegroup.id.TypeGroupAssignmentId")
public class TypeGroupAssignmentId implements ITypeGroupAssignmentId
{
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private IEntityTypeId sampleTypeId;

    @JsonProperty
    private ITypeGroupId typeGroupId;

    public TypeGroupAssignmentId(IEntityTypeId sampleTypeId, ITypeGroupId typeGroupId)
    {
        setEntityTypeId(sampleTypeId);
        setTypeGroupId(typeGroupId);
    }

    @Override
    public int hashCode()
    {
        return ((getSampleTypeId() == null) ? 0 : getSampleTypeId().hashCode())
                + ((getTypeGroupId() == null) ? 0 : getTypeGroupId().hashCode());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        TypeGroupAssignmentId other = (TypeGroupAssignmentId) obj;

        if (getSampleTypeId() == null)
        {
            if (other.getSampleTypeId() != null)
            {
                return false;
            }
        } else if (!getSampleTypeId().equals(other.getSampleTypeId()))
        {
            return false;
        }

        if (getTypeGroupId() == null)
        {
            if (other.getTypeGroupId() != null)
            {
                return false;
            }
        } else if (!getTypeGroupId().equals(other.getTypeGroupId()))
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return getSampleTypeId() + ", " + getTypeGroupId();
    }

    //
    // JSON-RPC
    //

    @SuppressWarnings("unused")
    private TypeGroupAssignmentId()
    {
        super();
    }

    @JsonIgnore
    @Override
    public IEntityTypeId getSampleTypeId()
    {
        return sampleTypeId;
    }

    @JsonIgnore
    private void setEntityTypeId(IEntityTypeId sampleTypeId)
    {
        if (sampleTypeId == null)
        {
            throw new IllegalArgumentException("Sample type id cannot be null");
        }
        this.sampleTypeId = sampleTypeId;
    }

    @JsonIgnore
    @Override
    public ITypeGroupId getTypeGroupId()
    {
        return typeGroupId;
    }

    @JsonIgnore
    private void setTypeGroupId(ITypeGroupId typeGroupId)
    {
        if (typeGroupId == null)
        {
            throw new IllegalArgumentException("Type group id cannot be null");
        }
        this.typeGroupId = typeGroupId;
    }
}
