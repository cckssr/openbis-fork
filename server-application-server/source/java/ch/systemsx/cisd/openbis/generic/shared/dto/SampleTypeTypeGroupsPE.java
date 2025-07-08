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
import ch.systemsx.cisd.openbis.generic.shared.basic.ICustomIdHolder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = TableNames.SAMPLE_TYPE_TYPE_GROUPS_TABLE, uniqueConstraints =
        { @UniqueConstraint(columnNames =
                { ColumnNames.SAMPLE_TYPE_COLUMN, ColumnNames.TYPE_GROUP_COLUMN }) })
@IdClass(SampleTypeTypeGroupsId.class)
public final class SampleTypeTypeGroupsPE extends HibernateAbstractRegistrationHolder implements
        Serializable, ICustomIdHolder<SampleTypeTypeGroupsId>
{
    private static final long serialVersionUID = IServer.VERSION;

    private SampleTypePE sampleType;

    private TypeGroupPE typeGroup;

    private boolean managedInternally;

    @NotNull(message = ValidationMessages.SAMPLE_TYPE_NOT_NULL_MESSAGE)
    @ManyToOne
    @JoinColumn(name = ColumnNames.SAMPLE_TYPE_COLUMN)
    @Id
    public SampleTypePE getSampleType()
    {
        return sampleType;
    }

    public void setSampleType(SampleTypePE sampleType)
    {
        this.sampleType = sampleType;
    }

    @NotNull(message = ValidationMessages.TYPE_GROUP_NOT_NULL_MESSAGE)
    @ManyToOne
    @JoinColumn(name = ColumnNames.TYPE_GROUP_COLUMN)
    @Id
    public TypeGroupPE getTypeGroup()
    {
        return typeGroup;
    }

    public void setTypeGroup(TypeGroupPE typeGroup)
    {
        this.typeGroup = typeGroup;
    }

    @NotNull
    @Column(name = ColumnNames.IS_MANAGED_INTERNALLY)
    public boolean isManagedInternally()
    {
        return managedInternally;
    }

    public void setManagedInternally(final boolean managedInternally)
    {
        this.managedInternally = managedInternally;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof SampleTypeTypeGroupsPE))
            return false;
        SampleTypeTypeGroupsPE castOther = (SampleTypeTypeGroupsPE) other;
        return new EqualsBuilder()
                .append(this.sampleType.getId(), castOther.sampleType.getId())
                .append(this.typeGroup.getName(), castOther.typeGroup.getName())
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(sampleType.getId()).append(typeGroup.getName())
                .toHashCode();
    }

    @Override
    @Transient
    public SampleTypeTypeGroupsId getId()
    {
        SampleTypeTypeGroupsId id = new SampleTypeTypeGroupsId();
        id.setTypeGroup(typeGroup);
        id.setSampleType(sampleType);
        return id;
    }

}
