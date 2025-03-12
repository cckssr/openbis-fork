package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.excel.v3.from.utils.IAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VocabularyTermHelper extends BasicImportHelper
{
    Map<String, Map<String, VocabularyTerm>> accumulator = new HashMap<>();

    public static final String VOCABULARY_CODE_FIELD = "Code";

    String vocabularyCode = null;

    EntityTypePermId permId;

    ImportTypes importTypes;

    VocabularyHelper vocabularyHelper;

    public VocabularyTermHelper(VocabularyHelper vocabularyHelper)
    {
        this.vocabularyHelper = vocabularyHelper;
    }

    public enum Attribute implements IAttribute
    {
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
                VOCABULARY_CODE_FIELD);
        this.accumulator.put(vocabularyCode, new HashMap<>());
        super.importBlock(page, pageIndex, start + 2, end);
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {

        String code =
                getValueByColumnName(header, values, Attribute.Code);
        if (this.vocabularyCode.equals(code))
        {
            return;
        }

        String label =
                getValueByColumnName(header, values, Attribute.Label);
        String description = getValueByColumnName(header, values,
                Attribute.Description);
        String internal =
                getValueByColumnName(header, values, Attribute.Internal);
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
