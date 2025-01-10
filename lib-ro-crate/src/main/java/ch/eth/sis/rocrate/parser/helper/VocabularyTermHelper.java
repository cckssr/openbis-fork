package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.VocabularyTermImportHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VocabularyTermHelper extends BasicImportHelper
{
    Map<String, Map<String, VocabularyTerm>> accumulator = new HashMap<>();

    String vocabularyCode = null;

    EntityTypePermId permId;

    ImportTypes importTypes;

    VocabularyHelper vocabularyHelper;

    public VocabularyTermHelper(ImportModes mode,
            ImportOptions options, VocabularyHelper vocabularyHelper)
    {
        super(mode, options);
        this.vocabularyHelper = vocabularyHelper;
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return null;
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {

        return false;
    }

    @Override
    public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        Map<String, Integer> header = parseHeader(page.get(start), false);
        vocabularyCode = getValueByColumnName(header, page.get(start + 1),
                VocabularyTermImportHelper.VOCABULARY_CODE_FIELD);
        this.accumulator.put(vocabularyCode, new HashMap<>());
        super.importBlock(page, pageIndex, start + 2, end);
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {

        String code =
                getValueByColumnName(header, values, VocabularyTermImportHelper.Attribute.Code);
        if (this.vocabularyCode.equals(code))
        {
            return;
        }

        String label =
                getValueByColumnName(header, values, VocabularyTermImportHelper.Attribute.Label);
        String description = getValueByColumnName(header, values,
                VocabularyTermImportHelper.Attribute.Description);
        String internal =
                getValueByColumnName(header, values, VocabularyTermImportHelper.Attribute.Internal);
        boolean isInternalNamespace = false;
        VocabularyTerm value = new VocabularyTerm();
        value.setCode(code);
        value.setLabel(label);
        value.setDescription(description);
        value.setManagedInternally(isInternalNamespace);
        Vocabulary vocabulary = vocabularyHelper.getResult().get(vocabularyCode);
        List<VocabularyTerm> terms = new ArrayList<>(vocabulary.getTerms());
        terms.add(value);
        vocabulary.setTerms(terms);

        this.accumulator.get(vocabularyCode).put(code, value);

    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        createObject(header, values, page, line);

    }

    @Override
    protected void validateHeader(Map<String, Integer> header)
    {

    }
}
