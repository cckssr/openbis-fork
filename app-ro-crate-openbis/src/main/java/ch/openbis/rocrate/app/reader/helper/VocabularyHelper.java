package ch.openbis.rocrate.app.reader.helper;

import ch.eth.sis.rocrate.facade.IVocabularyTerm;
import ch.eth.sis.rocrate.facade.IVocabularyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;

public class VocabularyHelper
{

    public static Vocabulary mapVocabulary(IVocabularyType vocabularyType)
    {
        Vocabulary vocabulary = new Vocabulary();
        {
            VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
            fetchOptions.withRegistrator();
            fetchOptions.withTerms();
            vocabulary.setFetchOptions(fetchOptions);
        }
        String code = getCode(vocabularyType);
        VocabularyPermId vocabularyPermId = new VocabularyPermId(code);
        vocabulary.setCode(code);
        vocabulary.setDescription(vocabularyType.getDescription());

        vocabulary.setTerms(
                vocabularyType.getTerms().stream().map(x -> mapTerm(vocabularyType, x)).toList());
        return vocabulary;
    }

    private static String getCode(IVocabularyType vocabularyType)
    {
        if (vocabularyType.getId().startsWith("openBIS:"))
        {
            return vocabularyType.getId().replace("openBIS:term", "");
        }
        String[] split = vocabularyType.getId().split(":");
        return OpenBisModel.makeOpenBisCodeCompliant(split[split.length - 1]);

    }

    public static ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm mapTerm(
            IVocabularyType vocabularyType, IVocabularyTerm term)
    {
        ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm result =
                new ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm();
        VocabularyTermFetchOptions fetchOptions = new VocabularyTermFetchOptions();
        fetchOptions.withVocabulary();
        result.setCode(mapCode(vocabularyType, term));
        result.setFetchOptions(fetchOptions);
        result.setLabel(term.getLabel());
        result.setDescription(term.getDescription());
        return result;
    }

    private static String mapCode(IVocabularyType type, IVocabularyTerm term)
    {
        return OpenBisModel.makeOpenBisCodeCompliant(term.getId().replaceAll(type.getId(), ""));
    }

}
