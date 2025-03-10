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

package ch.ethz.sis.openbis.generic.imagingapi.v3.dto;

import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonObject("imaging.dto.ImagingSemanticAnnotation")
public class ImagingSemanticAnnotation implements Serializable
{
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private String ontologyId;
    @JsonProperty
    private String ontologyVersion;
    @JsonProperty
    private String ontologyAnnotationId;

    @JsonIgnore
    public String getOntologyId()
    {
        return ontologyId;
    }

    public void setOntologyId(String ontologyId)
    {
        this.ontologyId = ontologyId;
    }

    @JsonIgnore
    public String getOntologyVersion()
    {
        return ontologyVersion;
    }

    public void setOntologyVersion(String ontologyVersion)
    {
        this.ontologyVersion = ontologyVersion;
    }

    @JsonIgnore
    public String getOntologyAnnotationId()
    {
        return ontologyAnnotationId;
    }

    public void setOntologyAnnotationId(String ontologyAnnotationId)
    {
        this.ontologyAnnotationId = ontologyAnnotationId;
    }

    @Override
    public String toString()
    {
        return String.format("(%s, %s, %s)", ontologyId, ontologyVersion, ontologyAnnotationId);
    }
}


