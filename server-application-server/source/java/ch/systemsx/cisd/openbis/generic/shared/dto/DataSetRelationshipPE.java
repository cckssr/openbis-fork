/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.shared.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import ch.systemsx.cisd.openbis.generic.shared.IServer;
import org.hibernate.annotations.SourceType;

/**
 * <i>Persistent Entity</i> object representing data set relationship.
 * 
 * @author Pawel Glyzewski
 */
@Entity
@Table(name = TableNames.DATA_SET_RELATIONSHIPS_VIEW, uniqueConstraints = @UniqueConstraint(columnNames =
{ ColumnNames.DATA_PARENT_COLUMN, ColumnNames.DATA_CHILD_COLUMN, ColumnNames.RELATIONSHIP_COLUMN }))
@IdClass(DataSetRelationshipId.class)
public class DataSetRelationshipPE implements Serializable
{
    private static final long serialVersionUID = IServer.VERSION;

    private DataPE parentDataSet;
    
    private boolean parentFrozen;
    
    private boolean containerFrozen;

    private DataPE childDataSet;
    
    private boolean childFrozen;
    
    private boolean componentFrozen;

    private PersonPE author;

    private Date registrationDate;

    private Date modificationDate;

    private RelationshipTypePE relationshipType;

    private Integer ordinal;

    /**
     * Deletion information.
     * <p>
     * If not <code>null</code>, then this data set is considered <i>deleted</i> (moved to trash).
     * </p>
     */
    private DeletionPE deletion;

    @SuppressWarnings("unused")
    private DataSetRelationshipPE()
    {
    }

    public DataSetRelationshipPE(DataPE parentDataSet, DataPE childDataSet, RelationshipTypePE relationshipType,
            Integer ordinal, PersonPE author)
    {
        setParentDataSet(parentDataSet);
        setChildDataSet(childDataSet);
        this.relationshipType = relationshipType;
        this.ordinal = ordinal;
        this.author = author;
    }

    @NotNull(message = ValidationMessages.PARENT_NOT_NULL_MESSAGE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = ColumnNames.DATA_PARENT_COLUMN)
    @Id
    public DataPE getParentDataSet()
    {
        return parentDataSet;
    }

    public void setParentDataSet(DataPE parentDataSet)
    {
        this.parentDataSet = parentDataSet;
        if (parentDataSet != null)
        {
            parentFrozen = parentDataSet.isFrozen() && parentDataSet.isFrozenForChildren();
            containerFrozen = parentDataSet.isFrozen() && parentDataSet.isFrozenForComponents();
        }
    }

    @NotNull
    @Column(name = ColumnNames.PARENT_FROZEN_COLUMN, nullable = false)
    public boolean isParentFrozen()
    {
        if (parentDataSet != null)
        {
            parentFrozen = parentDataSet.isFrozen() && parentDataSet.isFrozenForChildren();
        }
        return parentFrozen;
    }

    public void setParentFrozen(boolean parentFrozen)
    {
        this.parentFrozen = parentFrozen;
    }

    @NotNull
    @Column(name = ColumnNames.CONTAINER_FROZEN_COLUMN, nullable = false)
    public boolean isContainerFrozen()
    {
        if (parentDataSet != null)
        {
            containerFrozen = parentDataSet.isFrozen() && parentDataSet.isFrozenForComponents();
        }
        return containerFrozen;
    }

    public void setContainerFrozen(boolean containerFrozen)
    {
        this.containerFrozen = containerFrozen;
    }

    @NotNull(message = ValidationMessages.CHILD_NOT_NULL_MESSAGE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = ColumnNames.DATA_CHILD_COLUMN)
    @Id
    public DataPE getChildDataSet()
    {
        return childDataSet;
    }

    public void setChildDataSet(DataPE childDataSet)
    {
        this.childDataSet = childDataSet;
        if (childDataSet != null)
        {
            childFrozen = childDataSet.isFrozen() && childDataSet.isFrozenForParents();
            componentFrozen = childDataSet.isFrozen() && childDataSet.isFrozenForContainers();
        }
    }

    @NotNull
    @Column(name = ColumnNames.CHILD_FROZEN_COLUMN, nullable = false)
    public boolean isChildFrozen()
    {
        if (childDataSet != null)
        {
            childFrozen = childDataSet.isFrozen() && childDataSet.isFrozenForParents();
        }
        return childFrozen;
    }

    public void setChildFrozen(boolean childFrozen)
    {
        this.childFrozen = childFrozen;
    }

    @NotNull
    @Column(name = ColumnNames.COMPONENT_FROZEN_COLUMN, nullable = false)
    public boolean isComponentFrozen()
    {
        if (childDataSet != null)
        {
            componentFrozen = childDataSet.isFrozen() && childDataSet.isFrozenForContainers();
        }
        return componentFrozen;
    }

    public void setComponentFrozen(boolean componentFrozen)
    {
        this.componentFrozen = componentFrozen;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ColumnNames.PERSON_AUTHOR_COLUMN)
    public PersonPE getAuthor()
    {
        return author;
    }

    public void setAuthor(PersonPE author)
    {
        this.author = author;
    }

    @Column(name = ColumnNames.REGISTRATION_TIMESTAMP_COLUMN, nullable = false, insertable = false, updatable = false)
    @Generated(GenerationTime.INSERT)
    public Date getRegistrationDate()
    {
        return HibernateAbstractRegistrationHolder.getDate(registrationDate);
    }

    public void setRegistrationDate(final Date registrationDate)
    {
        this.registrationDate = registrationDate;
    }

    @Version
    @Column(name = ColumnNames.MODIFICATION_TIMESTAMP_COLUMN, nullable = false)
    public Date getModificationDate()
    {
        return modificationDate;
    }

    public void setModificationDate(Date versionDate)
    {
        this.modificationDate = versionDate;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = ColumnNames.DELETION_COLUMN)
    public DeletionPE getDeletion()
    {
        return deletion;
    }

    public void setDeletion(final DeletionPE deletion)
    {
        this.deletion = deletion;
    }

    @NotNull(message = ValidationMessages.RELATIONSHIP_NOT_NULL_MESSAGE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = ColumnNames.RELATIONSHIP_COLUMN)
    @Id
    public RelationshipTypePE getRelationshipType()
    {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipTypePE relationship)
    {
        this.relationshipType = relationship;
    }

    @Column(name = ColumnNames.ORDINAL_COLUMN)
    public Integer getOrdinal()
    {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal)
    {
        this.ordinal = ordinal;
    }

}
