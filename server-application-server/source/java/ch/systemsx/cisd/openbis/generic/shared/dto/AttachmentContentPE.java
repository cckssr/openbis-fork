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

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import ch.systemsx.cisd.openbis.generic.shared.IServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.IIdHolder;

/**
 * An attachment content <i>Persistent Entity</i>.
 * 
 * @author Christian Ribeaud
 */
@Entity
@Table(name = TableNames.ATTACHMENT_CONTENT_TABLE)
public class AttachmentContentPE implements IIdHolder, Serializable
{
    private static final long serialVersionUID = IServer.VERSION;

    private byte[] value;

    transient private Long id;

    @Override
    @SequenceGenerator(name = SequenceNames.ATTACHMENT_CONTENT_SEQUENCE, sequenceName = SequenceNames.ATTACHMENT_CONTENT_SEQUENCE, allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.ATTACHMENT_CONTENT_SEQUENCE)
    public Long getId()
    {
        return id;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    /**
     * Returns bytes blob stored in the attachment.
     */
    @Column(name = ColumnNames.VALUE_COLUMN, updatable = false)
    @NotNull(message = ValidationMessages.VALUE_NOT_NULL_MESSAGE)
    // @Type(type = "org.springframework.orm.hibernate3.support.BlobByteArrayType")
    public byte[] getValue()
    {
        return value;
    }

    public void setValue(final byte[] value)
    {
        this.value = value;
    }
}
