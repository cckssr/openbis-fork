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

package ch.systemsx.cisd.openbis.generic.shared.dto;

import ch.systemsx.cisd.openbis.generic.shared.IServer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Embeddable;
import java.io.Serializable;


public class SampleTypeTypeGroupsId implements Serializable
{
    private static final long serialVersionUID = IServer.VERSION;

    private SampleTypePE sampleType;
    private TypeGroupPE typeGroup;

    public SampleTypePE getSampleType()
    {
        return sampleType;
    }

    public void setSampleType(SampleTypePE sampleType)
    {
        this.sampleType = sampleType;
    }

    public TypeGroupPE getTypeGroup()
    {
        return typeGroup;
    }

    public void setTypeGroup(TypeGroupPE typeGroup)
    {
        this.typeGroup = typeGroup;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof SampleTypeTypeGroupsId))
            return false;
        SampleTypeTypeGroupsId castOther = (SampleTypeTypeGroupsId) other;
        return new EqualsBuilder()
                .append(this.sampleType.getId(), castOther.sampleType.getId())
                .append(this.typeGroup.getId(), castOther.typeGroup.getId())
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(sampleType.getId()).append(typeGroup.getId())
                .toHashCode();
    }

}
