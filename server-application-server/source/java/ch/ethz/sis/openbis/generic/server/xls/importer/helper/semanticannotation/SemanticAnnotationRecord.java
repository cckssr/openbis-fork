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

package ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

public final class SemanticAnnotationRecord
{
    private final String semanticAnnotationId;

    private final String semanticAnnotationVersionId;

    private final String semanticAnnotationAccessionId;


    public SemanticAnnotationRecord(String semanticAnnotationId,
            String semanticAnnotationVersionId,
            String semanticAnnotationAccessionId)
    {
        this.semanticAnnotationId = semanticAnnotationId;
        this.semanticAnnotationVersionId = semanticAnnotationVersionId;
        this.semanticAnnotationAccessionId = semanticAnnotationAccessionId;
    }


    public String getSemanticAnnotationId()
    {
        return semanticAnnotationId;
    }

    public String getSemanticAnnotationVersionId()
    {
        return semanticAnnotationVersionId;
    }

    public String getSemanticAnnotationAccessionId()
    {
        return semanticAnnotationAccessionId;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
                .append("SemanticAnnotaitonRecord:")
                .append(semanticAnnotationId).append(":")
                .append(semanticAnnotationVersionId).append(":")
                .append(semanticAnnotationAccessionId).toString();
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(semanticAnnotationId);
        builder.append(semanticAnnotationVersionId);
        builder.append(semanticAnnotationAccessionId);
        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }

        SemanticAnnotationRecord other = (SemanticAnnotationRecord) obj;
        return Objects.equals(semanticAnnotationId, other.semanticAnnotationId)
                && Objects.equals(semanticAnnotationVersionId, other.semanticAnnotationVersionId)
                && Objects.equals(semanticAnnotationAccessionId, other.semanticAnnotationAccessionId);

    }
}
