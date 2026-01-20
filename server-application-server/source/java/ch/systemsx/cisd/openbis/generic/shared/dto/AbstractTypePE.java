/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.Length;

import ch.systemsx.cisd.openbis.generic.client.web.client.dto.GenericConstants;
import ch.systemsx.cisd.openbis.generic.shared.IServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.IIdentityHolder;

/**
 * Abstract Persistence Entity representing a type.
 * 
 * @author Izabela Adamczyk
 */
@MappedSuperclass
public abstract class AbstractTypePE extends AbstractIdAndCodeHolder<AbstractTypePE> implements IIdentityHolder
{
    private static final long serialVersionUID = IServer.VERSION;

    transient protected Long id;

    protected String code;

    private String description;

    @Override
    @Transient
    public String getPermId()
    {
        return getCode();
    }

    @Override
    @Transient
    public String getIdentifier()
    {
        return getCode();
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public abstract void setCode(final String code);

    @Column(name = ColumnNames.DESCRIPTION_COLUMN)
    @org.hibernate.validator.constraints.Length(max = GenericConstants.DESCRIPTION_2000, message = ValidationMessages.DESCRIPTION_LENGTH_MESSAGE)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    //
    // AbstractIdAndCodeHolder
    //

    @Override
    @Column(name = ColumnNames.CODE_COLUMN, insertable = false, updatable = false)
    public abstract String getCode();

    @Override
    ToStringBuilder createStringBuilder()
    {
        final ToStringBuilder builder = super.createStringBuilder();
        builder.append("description", getDescription());
        return builder;
    }
}
