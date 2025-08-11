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

package ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.ObjectToString;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.exceptions.NotFetchedException;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonObject("as.dto.typegroup.TypeGroup")
public class TypeGroup implements Serializable, IModificationDateHolder, ICodeHolder,
        IModifierHolder, IRegistrationDateHolder, IRegistratorHolder
{
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private TypeGroupId id;

    @JsonProperty
    private TypeGroupFetchOptions fetchOptions;

    @JsonProperty
    private String code;

    @JsonProperty
    private Person registrator;

    @JsonProperty
    private Date registrationDate;

    @JsonProperty
    private Person modifier;

    @JsonProperty
    private Date modificationDate;

    @JsonProperty
    private Boolean managedInternally;

    @JsonProperty
    private List<TypeGroupAssignment> typeGroupAssignments;

    @JsonProperty
    private Map<String, String> metaData;

    @JsonIgnore
    public TypeGroupId getId()
    {
        return id;
    }

    public void setId(TypeGroupId id)
    {
        this.id = id;
    }

    @JsonIgnore
    public TypeGroupFetchOptions getFetchOptions()
    {
        return fetchOptions;
    }

    public void setFetchOptions(
            TypeGroupFetchOptions fetchOptions)
    {
        this.fetchOptions = fetchOptions;
    }

    @JsonIgnore
    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    @JsonIgnore
    @Override
    public Person getRegistrator()
    {
        return registrator;
    }

    public void setRegistrator(Person registrator)
    {
        this.registrator = registrator;
    }

    @JsonIgnore
    @Override
    public Date getRegistrationDate()
    {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate)
    {
        this.registrationDate = registrationDate;
    }

    @JsonIgnore
    @Override
    public Person getModifier()
    {
        return modifier;
    }

    public void setModifier(Person modifier)
    {
        this.modifier = modifier;
    }

    @JsonIgnore
    @Override
    public Date getModificationDate()
    {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate)
    {
        this.modificationDate = modificationDate;
    }

    @JsonIgnore
    public Boolean isManagedInternally()
    {
        return managedInternally;
    }

    public void setManagedInternally(Boolean managedInternally)
    {
        this.managedInternally = managedInternally;
    }

    @JsonIgnore
    public List<TypeGroupAssignment> getTypeGroupAssignments()
    {
        if (getFetchOptions() != null && getFetchOptions().hasTypeGroupAssignments())
        {
            return typeGroupAssignments;
        } else
        {
            throw new NotFetchedException("Type group assignments have not been fetched.");
        }
    }

    public void setTypeGroupAssignments(
            List<TypeGroupAssignment> typeGroupAssignments)
    {
        this.typeGroupAssignments = typeGroupAssignments;
    }

    @JsonIgnore
    public Map<String, String> getMetaData()
    {
        return metaData;
    }

    public void setMetaData(Map<String, String> metaData)
    {
        this.metaData = metaData;
    }

    @Override
    public String toString()
    {
        return new ObjectToString(this).append("name", code)
                .append("managedInternally", managedInternally).toString();
    }
}
