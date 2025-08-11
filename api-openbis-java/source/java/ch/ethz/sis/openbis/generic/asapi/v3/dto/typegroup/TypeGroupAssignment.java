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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IRegistrationDateHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IRegistratorHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

@JsonObject("as.dto.typegroup.TypeGroupAssignment")
public class TypeGroupAssignment implements Serializable, IRegistrationDateHolder,
        IRegistratorHolder
{

    @JsonProperty
    private SampleType sampleType;

    @JsonProperty
    private TypeGroup typeGroup;

    @JsonProperty
    private Person registrator;

    @JsonProperty
    private Date registrationDate;

    @JsonProperty
    private boolean managedInternally;

    @JsonProperty
    private TypeGroupAssignmentFetchOptions fetchOptions;

    @JsonIgnore
    public SampleType getSampleType()
    {
        return sampleType;
    }

    public void setSampleType(SampleType sampleType)
    {
        this.sampleType = sampleType;
    }

    @JsonIgnore
    public TypeGroup getTypeGroup()
    {
        return typeGroup;
    }

    public void setTypeGroup(TypeGroup typeGroup)
    {
        this.typeGroup = typeGroup;
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
    public TypeGroupAssignmentFetchOptions getFetchOptions()
    {
        return fetchOptions;
    }

    public void setFetchOptions(
            TypeGroupAssignmentFetchOptions fetchOptions)
    {
        this.fetchOptions = fetchOptions;
    }

    @JsonIgnore
    public boolean isManagedInternally()
    {
        return managedInternally;
    }

    public void setManagedInternally(boolean managedInternally)
    {
        this.managedInternally = managedInternally;
    }

    @Override
    public String toString()
    {
        return new ObjectToString(this)
                .append("sampleType", sampleType != null ? sampleType.getCode() : null)
                .append("typeGroup", typeGroup != null ? typeGroup.getCode() : null)
                .append("managedInternally", managedInternally).toString();
    }
}
