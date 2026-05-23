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
package ch.systemsx.cisd.openbis.generic.shared.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import ch.systemsx.cisd.openbis.generic.shared.IServer;

/**
 * Persistence entity representing data set type - property type relation.
 * 
 * @author Izabela Adamczyk
 */
@Entity
@Table(name = TableNames.DATA_SET_TYPE_PROPERTY_TYPE_TABLE, uniqueConstraints =
{ @UniqueConstraint(columnNames =
{ ColumnNames.DATA_SET_TYPE_COLUMN, ColumnNames.PROPERTY_TYPE_COLUMN }) })
public class DataSetTypePropertyTypePE extends EntityTypePropertyTypePE
{

    private static final long serialVersionUID = IServer.VERSION;

    public static final DataSetTypePropertyTypePE[] EMPTY_ARRAY = new DataSetTypePropertyTypePE[0];

    @NotNull(message = ValidationMessages.DATA_SET_TYPE_NOT_NULL_MESSAGE)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = DataSetTypePE.class)
    @JoinColumn(name = ColumnNames.DATA_SET_TYPE_COLUMN)
    private EntityTypePE getEntityTypeInternal()
    {
        return entityType;
    }

    //
    // EntityTypePropertyTypePE
    //

    @Override
    @Transient
    public EntityTypePE getEntityType()
    {
        return getEntityTypeInternal();
    }

    @Override
    // This setter sets the bidirectional connection. That's why we must have an
    // another internal
    // plain setter for Hibernate.
    public void setEntityType(EntityTypePE entityType)
    {
        ((DataSetTypePE) entityType).addDataSetTypePropertyType(this);
    }

    @Override
    @SequenceGenerator(name = SequenceNames.DATA_SET_TYPE_PROPERTY_TYPE_SEQUENCE, sequenceName = SequenceNames.DATA_SET_TYPE_PROPERTY_TYPE_SEQUENCE, allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.DATA_SET_TYPE_PROPERTY_TYPE_SEQUENCE)
    public Long getId()
    {
        return id;
    }

    @Override
    public void setPropertyType(PropertyTypePE propertyType)
    {
        propertyType.addDataSetTypePropertyType(this);
    }

}
