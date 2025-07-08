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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.INameHolder;
import ch.systemsx.cisd.common.reflection.ModifiedShortPrefixToStringStyle;
import ch.systemsx.cisd.openbis.generic.shared.IServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.IIdHolder;
import ch.systemsx.cisd.openbis.generic.shared.dto.hibernate.JsonMapUserType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name = TableNames.TYPE_GROUPS_TABLE, uniqueConstraints = {
        @UniqueConstraint(columnNames = { ColumnNames.NAME_COLUMN }) })
@TypeDefs({ @TypeDef(name = "JsonMap", typeClass = JsonMapUserType.class) })
public final class TypeGroupPE implements Serializable,
        IIdHolder, INameHolder,
        IModifierAndModificationDateBean, Comparable<TypeGroupPE>, IMetaDataHolder
{
    private static final long serialVersionUID = IServer.VERSION;

    @SequenceGenerator(name = SequenceNames.TYPE_GROUPS_SEQUENCE, sequenceName = SequenceNames.TYPE_GROUPS_SEQUENCE, allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.TYPE_GROUPS_SEQUENCE)
    private Long id;

//    @Id
    @Column(name = ColumnNames.NAME_COLUMN, unique = true)
//    @NaturalId(mutable=true)
    @NotBlank
    private String name;


    @OptimisticLock(excluded = true)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = ColumnNames.PERSON_MODIFIER_COLUMN)
    private PersonPE modifier;

    @OptimisticLock(excluded = true)
    @Column(name = ColumnNames.MODIFICATION_TIMESTAMP_COLUMN, nullable = false)
    private Date modificationDate;

    @Column(name = ColumnNames.IS_MANAGED_INTERNALLY)
    private boolean managedInternally;

    @Column(name = "meta_data")
    @Type(type = "JsonMap")
    private Map<String, String> metaData;

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    public final void setId(final Long id)
    {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ColumnNames.PERSON_REGISTERER_COLUMN, updatable = false)
    private PersonPE registrator;

    @Column(name = ColumnNames.REGISTRATION_TIMESTAMP_COLUMN, nullable = false, insertable = false, updatable = false)
    @Generated(GenerationTime.INSERT)
    private Date registrationDate;

    /**
     * Ensures that given <var>date</var> is a real one (<code>java.util.Date</code>) and not a <i>SQL</i> one.
     */
    public static Date getDate(final Date date)
    {
        if (date == null)
        {
            return null;
        }
        final String packageName = date.getClass().getPackage().getName();
        if (packageName.equals("java.sql"))
        {
            return new Date(date.getTime());
        }
        return date;
    }


    public Date getRegistrationDate()
    {
        return getDate(registrationDate);
    }

    @Transient
    public Date getRegistrationDateInternal()
    {
        return registrationDate;
    }

    public void setRegistrationDate(final Date registrationDate)
    {
        this.registrationDate = registrationDate;
    }

    public PersonPE getRegistrator()
    {
        return registrator;
    }

    public void setRegistrator(final PersonPE registrator)
    {
        this.registrator = registrator;
    }


    @Override
    public PersonPE getModifier()
    {
        return modifier;
    }

    @Override
    public void setModifier(final PersonPE modifier)
    {
        this.modifier = modifier;
    }


    @Override
    public Date getModificationDate()
    {
        return modificationDate;
    }

    @Override
    public void setModificationDate(Date versionDate)
    {
        this.modificationDate = versionDate;
    }


    @NotNull
    public boolean isManagedInternally()
    {
        return managedInternally;
    }

    public void setManagedInternally(final boolean managedInternally)
    {
        this.managedInternally = managedInternally;
    }

    public Map<String, String> getMetaData()
    {
        return metaData;
    }

    public void setMetaData(Map<String, String> metaData)
    {
        this.metaData = metaData;
    }

    @Override
    public final boolean equals(final Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj instanceof TypeGroupPE == false)
        {
            return false;
        }
        final TypeGroupPE that = (TypeGroupPE) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(getName(), that.getName());
        return builder.isEquals();
    }

    @Override
    public final int hashCode()
    {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(getName());
        return builder.toHashCode();
    }

    @Override
    public final String toString()
    {
        final ToStringBuilder builder =
                new ToStringBuilder(this,
                        ModifiedShortPrefixToStringStyle.MODIFIED_SHORT_PREFIX_STYLE);
        builder.append("name", getName());
        builder.append("managedInternally", isManagedInternally());
        return builder.toString();
    }

    //
    // Compare
    //

    /**
     * If <code>null</code> values are present for <code>name</code>, then they come first.
     */
    @Override
    public int compareTo(TypeGroupPE that)
    {
        final String thatName = that.getName();
        final String thisName = getName();
        if (thisName == null)
        {
            return thatName == null ? 0 : -1;
        }
        if (thatName == null)
        {
            return 1;
        }
        return thisName.compareTo(thatName);
    }
}
