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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import ch.systemsx.cisd.openbis.generic.shared.dto.hibernate.PatternValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Source;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import ch.systemsx.cisd.common.reflection.ClassUtils;
import ch.systemsx.cisd.common.reflection.ModifiedShortPrefixToStringStyle;
import ch.systemsx.cisd.openbis.generic.shared.IServer;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import ch.systemsx.cisd.openbis.generic.shared.util.EqualsHashUtils;
import ch.systemsx.cisd.openbis.generic.shared.dto.hibernate.JsonStringType;
import ch.systemsx.cisd.openbis.generic.shared.hibernate.type.DoubleArrayJavaType;
import ch.systemsx.cisd.openbis.generic.shared.hibernate.type.LongArrayJavaType;
import ch.systemsx.cisd.openbis.generic.shared.hibernate.type.StringArrayJavaType;
import ch.systemsx.cisd.openbis.generic.shared.hibernate.type.DateArrayJavaType;

/**
 * Persistence entity representing entity property.
 *
 * @author Tomasz Pylak
 * @author Izabela Adamczyk
 */
@MappedSuperclass
@PatternValue
public abstract class EntityPropertyPE extends HibernateAbstractRegistrationHolder implements
        IUntypedValueSetter, IEntityPropertyHolder
{
    private static final long serialVersionUID = IServer.VERSION;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected IEntityPropertiesHolder entity;

    /**
     * The value of this entity property.
     * <p>
     * Like in the database, no value is set if <code>value</code> is from controlled vocabulary.
     * </p>
     */
    private String value;

    private Long[] integerArrayValue;

    private Double[] realArrayValue;

    private Date[] timestampArrayValue;

    private String[] stringArrayValue;

    private String jsonValue;

    /**
     * The vocabulary term.
     * <p>
     * Not <code>null</code> if <code>value</code> is from controlled vocabulary.
     * </p>
     */
    private VocabularyTermPE vocabularyTerm;

    protected transient Long id;

    protected EntityTypePropertyTypePE entityTypePropertyType;

    /**
     * Person who modified this entity.
     * <p>
     * This is specified at update time.
     * </p>
     */
    private PersonPE author;

    private Date modificationDate;

    protected boolean entityFrozen;

    /**
     * Special field for multi-value properties hashcode computing
     */
    protected transient Long index;

    protected boolean unique;

    public <T extends EntityTypePropertyTypePE> void setEntityTypePropertyType(
            final T entityTypePropertyType)
    {
        this.entityTypePropertyType = entityTypePropertyType;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public void setIndex(final long index)
    {
        this.index = index;
    }

    public void setEntityFrozen(boolean frozen)
    {
        this.entityFrozen = frozen;
    }

    public void setValue(final String value)
    {
        this.value = value;
    }

    public void setUnique(final boolean unique)
    {
        this.unique = unique;
    }

    private void clearValues() {
        this.value = null;
        this.vocabularyTerm = null;
        this.integerArrayValue = null;
        this.stringArrayValue = null;
        this.realArrayValue = null;
        this.timestampArrayValue = null;
        this.jsonValue = null;
        computeUnique();
    }

    private void computeUnique()
    {
        if(entityTypePropertyType != null)
        {
            this.unique = entityTypePropertyType.isUnique();
        } else {
            this.unique = false;
        }
    }

    @Column(name = ColumnNames.VALUE_COLUMN)
    public String getValue()
    {
        return value;
    }

    @NotNull
    @Column(name = ColumnNames.IS_UNIQUE)
    public boolean isUnique()
    {
        return unique;
    }

    public void setVocabularyTerm(final VocabularyTermPE vt)
    {
        this.vocabularyTerm = vt;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = ColumnNames.VOCABULARY_TERM_COLUMN)
    public VocabularyTermPE getVocabularyTerm()
    {
        return vocabularyTerm;
    }

    @JavaType(LongArrayJavaType.class)
    @Column(name = ColumnNames.INTEGER_ARRAY_VALUE_COLUMN)
    @JdbcTypeCode(SqlTypes.ARRAY)
    public Long[] getIntegerArrayValue()
    {
        return integerArrayValue;
    }

    public void setIntegerArrayValue(final Long[] values)
    {
        this.integerArrayValue = values;
    }


    public void setRealArrayValue(final Double[] values)
    {
        this.realArrayValue = values;
    }

    @Column(name = ColumnNames.REAL_ARRAY_VALUE_COLUMN)
    @JavaType(DoubleArrayJavaType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    public Double[] getRealArrayValue()
    {
        return realArrayValue;
    }

    public void setTimestampArrayValue(final Date[] values)
    {
        this.timestampArrayValue = values;
    }

    @Column(name = ColumnNames.TIMESTAMP_ARRAY_VALUE_COLUMN)
    @JavaType(DateArrayJavaType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    public Date[] getTimestampArrayValue()
    {
        return timestampArrayValue;
    }

    public void setStringArrayValue(final String[] values)
    {
        this.stringArrayValue = values;
    }

    @Column(name = ColumnNames.STRING_ARRAY_VALUE_COLUMN)
    @JavaType(StringArrayJavaType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    public String[] getStringArrayValue()
    {
        return stringArrayValue;
    }

    @Type(JsonStringType.class)
    @Column(name = ColumnNames.JSON_VALUE_COLUMN)
    public String getJsonValue()
    {
        return jsonValue;
    }

    public void setJsonValue(String jsonValue)
    {
        this.jsonValue = jsonValue;
    }

    //
    // IUntypedValueSetter
    //

    @Override
    public void setUntypedValue(final String valueOrNull,
            final VocabularyTermPE vocabularyTermOrNull,
            SamplePE sampleOrNull, Long[] integerArrayOrNull, Double[] realArrayOrNull,
            String[] stringArrayOrNull, Date[] timestampArrayOrNull, String jsonOrNull)
    {
        assert valueOrNull != null || vocabularyTermOrNull != null
                || integerArrayOrNull != null || realArrayOrNull != null
                || stringArrayOrNull != null || timestampArrayOrNull != null
                || jsonOrNull != null : "Either value, array value, json, vocabulary term or material should not be null.";
        clearValues();
        if (vocabularyTermOrNull != null)
        {
            setVocabularyTerm(vocabularyTermOrNull);
        } else if (integerArrayOrNull != null) {
            setIntegerArrayValue(integerArrayOrNull);
        } else if (realArrayOrNull != null) {
            setRealArrayValue(realArrayOrNull);
        }else if (stringArrayOrNull != null) {
            setStringArrayValue(stringArrayOrNull);
        }else if (timestampArrayOrNull != null) {
            setTimestampArrayValue(timestampArrayOrNull);
        }else if (jsonOrNull != null) {
            setJsonValue(jsonOrNull);
        } else
        {
            setValue(valueOrNull);
        }
    }

    @Version
    @Column(name = ColumnNames.MODIFICATION_TIMESTAMP_COLUMN, nullable = false)
    @Source(SourceType.DB)
    public Date getModificationDate()
    {
        return modificationDate;
    }

    public void setModificationDate(Date versionDate)
    {
        this.modificationDate = versionDate;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ColumnNames.PERSON_AUTHOR_COLUMN, nullable = false, updatable = true)
    public PersonPE getAuthor()
    {
        return author;
    }

    public void setAuthor(PersonPE author)
    {
        this.author = author;
    }

    /**
     * Sets the <var>entity</var> of this property.
     * <p>
     * <i>Do not use directly, instead, call
     * {@link IEntityPropertiesHolder#addProperty(EntityPropertyPE)} with <code>this</code>
     * object!</i>
     */
    void setEntity(final IEntityPropertiesHolder entity)
    {
        this.entity = entity;
    }

    //
    // Object
    //

    @Override
    public final String toString()
    {
        final ToStringBuilder builder =
                new ToStringBuilder(this,
                        ModifiedShortPrefixToStringStyle.MODIFIED_SHORT_PREFIX_STYLE);
        builder.append("entityTypePropertyType", getEntityTypePropertyType());
        builder.append("value", tryGetUntypedValue());
        return builder.toString();
    }

    //
    // IEntityProperty
    //

    @Override
    public String tryGetUntypedValue()
    {
        if (getVocabularyTerm() != null)
        {
            final String labelOrNull = getVocabularyTerm().getLabel();
            return getVocabularyTerm().getCode()
                    + (labelOrNull != null ? " " + getVocabularyTerm().getLabel() : "");
        } else
        {
            if (this.integerArrayValue != null)
            {
                return convertArrayToString(this.integerArrayValue);
            }
            if (getRealArrayValue() != null)
                return convertArrayToString(this.realArrayValue);
            if (getTimestampArrayValue() != null)
                return convertTimestampArrayToString(this.timestampArrayValue);
            if (getStringArrayValue() != null)
                return convertArrayToString(this.stringArrayValue);
            if (getJsonValue() != null)
                return getJsonValue();
            return getValue();
        }
    }

    private String convertTimestampArrayToString(Date[] array) {
        if (array == null || array.length == 0)
            return "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss XX");
        return Stream.of(array)
                .map(d -> d == null ? "" : dateFormat.format(d))
                .reduce((x, y) -> x + ", " + y)
                .get();
    }

    private static String convertArrayToString(final Object[] array)
    {
        try
        {
            return MAPPER.writeValueAsString(array);
        } catch (final JsonProcessingException e)
        {
            throw new IllegalArgumentException("Not supported value for array: " +
                    Arrays.toString(array) + ".", e);
        }
    }

    /**
     * Creates an {@link EntityPropertyPE} from given <var>entityKind</var>.
     */
    public final static <T extends EntityPropertyPE> T createEntityProperty(
            final EntityKind entityKind)
    {
        assert entityKind != null : "Unspecified entity kind";
        return ClassUtils.createInstance(entityKind.<T> getEntityPropertyClass());
    }

    @Override
    public final boolean equals(final Object obj)
    {
        EqualsHashUtils.assertDefined(getEntityTypePropertyType(), "etpt");
        if (obj == this)
        {
            return true;
        }
        if (obj instanceof EntityPropertyPE == false)
        {
            return false;
        }
        final EntityPropertyPE that = (EntityPropertyPE) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(getEntity(), that.getEntity());
        builder.append(getEntityTypePropertyType(), that.getEntityTypePropertyType());
        builder.append(tryGetUntypedValue(), that.tryGetUntypedValue());
        return builder.isEquals();
    }

    @Override
    public final int hashCode()
    {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(getEntity());
        builder.append(getEntityTypePropertyType());
        builder.append(tryGetUntypedValue());
        builder.append(index);
        builder.append(id);
        return builder.toHashCode();
    }

}
