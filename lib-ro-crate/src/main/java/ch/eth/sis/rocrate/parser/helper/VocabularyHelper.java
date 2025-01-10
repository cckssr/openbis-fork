package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VocabularyHelper extends BasicImportHelper
{
    Map<String, Vocabulary> accumulatorCodeToVocabulary = new HashMap<>();

    private enum Attribute implements IAttribute
    {
        Version("Version", false, false),
        Code("Code", true, true),
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

    public VocabularyHelper(ImportModes mode,
            ImportOptions options)
    {
        super(mode, options);
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return null;
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        String code = getValueByColumnName(header, values, VocabularyHelper.Attribute.Code);
        return accumulatorCodeToVocabulary.containsKey(code);
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, VocabularyHelper.Attribute.Code);
        String internal = getValueByColumnName(header, values, VocabularyHelper.Attribute.Internal);
        boolean isInternalNamespace = false;
        VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
        fetchOptions.withTerms();

        String description =
                getValueByColumnName(header, values, VocabularyHelper.Attribute.Description);
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setCode(code);
        vocabulary.setDescription(description);
        vocabulary.setManagedInternally(isInternalNamespace);
        vocabulary.setFetchOptions(fetchOptions);
        vocabulary.setTerms(new ArrayList<>());
        accumulatorCodeToVocabulary.put(code, vocabulary);

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

    public Map<String, Vocabulary> getResult()
    {
        return accumulatorCodeToVocabulary;
    }
}
