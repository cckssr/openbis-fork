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

import java.io.Serializable;

/// Synthetic object to not pass to much information around
public class SampleTypeTypeGroupsTechId implements Serializable, ICompositeIdHolder
{
    private static final long serialVersionUID = IServer.VERSION;

    private final Long sampleTypeTechId;
    private final Long typeGroupTechId;

    public Long getSampleTypeTechId()
    {
        return sampleTypeTechId;
    }

    public Long getTypeGroupTechId()
    {
        return typeGroupTechId;
    }

    public SampleTypeTypeGroupsTechId(Long sampleTypeTechId, Long typeGroupTechId)
    {
        this.sampleTypeTechId = sampleTypeTechId;
        this.typeGroupTechId = typeGroupTechId;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof SampleTypeTypeGroupsTechId))
            return false;
        SampleTypeTypeGroupsTechId castOther = (SampleTypeTypeGroupsTechId) other;
        return new EqualsBuilder()
                .append(this.sampleTypeTechId, castOther.sampleTypeTechId)
                .append(this.typeGroupTechId, castOther.typeGroupTechId)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(sampleTypeTechId).append(typeGroupTechId)
                .toHashCode();
    }
}
