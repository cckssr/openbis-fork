/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyTermCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyTermPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyTermUpdate;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.VersionUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class VocabularyTermImportHelper extends BasicImportHelper
{
    private static final String VOCABULARY_CODE_FIELD = "Code";

    private enum Attribute implements IAttribute {
        Version("Version", false, false),
        Code("Code", true, true),
        Label("Label", true, false),
        Description("Description", true, false),
        Internal("Internal", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    private String vocabularyCode;

    private Vocabulary vocabulary;

    private List<String> vocabularyCodes;

    private final Map<String, Integer> versions;

    private final DelayedExecutionDecorator delayedExecutor;

    private final AttributeValidator<Attribute> attributeValidator;

    public VocabularyTermImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, Map<String, Integer> versions)
    {
        super(mode, options);
        this.versions = versions;
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
    }

    @Override public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        Map<String, Integer> header = parseHeader(page.get(start), false);
        vocabularyCode = getValueByColumnName(header, page.get(start + 1), VOCABULARY_CODE_FIELD);
        VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
        fetchOptions.withTerms();
        vocabulary = delayedExecutor.getVocabulary(new VocabularyPermId(vocabularyCode), fetchOptions);
        if(vocabulary != null) {
            vocabularyCodes = vocabulary.getTerms().stream().map(VocabularyTerm::getCode).collect(
                    Collectors.toList());
        } else {
            vocabularyCodes = new ArrayList<>();
        }
        super.importBlock(page, pageIndex, start + 2, end);
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.VOCABULARY_TERM;
    }

    @Override
    protected void validateLine(Map<String, Integer> header, List<String> values) {
        String vocabularyTermCode = getValueByColumnName(header, values, Attribute.Code);

        String isInternalVocabularyTermValue = getValueByColumnName(header, values, Attribute.Internal);
        boolean isInternalVocabularyTerm = ImportUtils.isTrue(isInternalVocabularyTermValue);

        if(isInternalVocabularyTerm && !delayedExecutor.isSystem())
        {
            if(vocabulary != null && !vocabularyCodes.contains(vocabularyTermCode))
            {
                throw new UserFailureException("Non-system user can not create internal vocabulary terms!");
            }
        }
    }

    @Override protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        return isNewVersionWithInternalNamespace(header, values, versions,
                delayedExecutor.isSystem(),
                ImportTypes.VOCABULARY_TERM.getType() + "-" + vocabularyCode,
                Attribute.Version, Attribute.Code, Attribute.Internal);
    }

    @Override protected void updateVersion(Map<String, Integer> header, List<String> values)
    {
        String version = getValueByColumnName(header, values, Attribute.Version);
        String code = getValueByColumnName(header, values, Attribute.Code);

        if (version == null || version.isEmpty()) {
            Integer storedVersion = VersionUtils.getStoredVersion(versions, ImportTypes.VOCABULARY_TERM.getType() + "-" + vocabularyCode, code);
            storedVersion++;
            version = storedVersion.toString();
        }

        VersionUtils.updateVersion(version, versions, ImportTypes.VOCABULARY_TERM.getType() + "-" + vocabularyCode, code);
    }

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        VocabularyTermPermId termId = new VocabularyTermPermId(code, vocabularyCode);

        VocabularyTermFetchOptions fetchOptions = new VocabularyTermFetchOptions();
        fetchOptions.withVocabulary();

        return this.delayedExecutor.getVocabularyTerm(termId, fetchOptions) != null;
    }

    @Override protected void createObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String label = getValueByColumnName(header, values, Attribute.Label);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        boolean isInternalNamespace = ImportUtils.isTrue(internal);

        VocabularyPermId vocabularyPermId = new VocabularyPermId(vocabularyCode);

        VocabularyTermCreation creation = new VocabularyTermCreation();
        creation.setVocabularyId(vocabularyPermId);
        creation.setCode(code);
        creation.setLabel(label);
        creation.setDescription(description);
        creation.setManagedInternally(isInternalNamespace);

        this.delayedExecutor.createVocabularyTerm(creation);
    }

    @Override protected void updateObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String label = getValueByColumnName(header, values, Attribute.Label);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        VocabularyTermPermId termId = new VocabularyTermPermId(code, vocabularyCode);

        VocabularyTermUpdate update = new VocabularyTermUpdate();
        update.setManagedInternally(ImportUtils.isTrue(internal));
        update.setVocabularyTermId(termId);
        if (label != null)
        {
            if (label.equals("--DELETE--") || label.equals("__DELETE__"))
            {
                update.setLabel("");
            } else if (!label.isEmpty())
            {
                update.setLabel(label);
            }
        }
        if (description != null)
        {
            if (description.equals("--DELETE--") || description.equals("__DELETE__"))
            {
                update.setDescription("");
            } else if (!description.isEmpty())
            {
                update.setDescription(description);
            }
        }
        this.delayedExecutor.updateVocabularyTerm(update);
    }

    @Override protected void validateHeader(Map<String, Integer> headers)
    {
        attributeValidator.validateHeaders(Attribute.values(), headers);
    }
}
