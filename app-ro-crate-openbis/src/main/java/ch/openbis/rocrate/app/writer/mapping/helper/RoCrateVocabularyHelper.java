package ch.openbis.rocrate.app.writer.mapping.helper;

import ch.eth.sis.rocrate.facade.IVocabularyTerm;
import ch.eth.sis.rocrate.facade.IVocabularyType;
import ch.eth.sis.rocrate.facade.impl.VocabularyTerm;
import ch.eth.sis.rocrate.facade.impl.VocabularyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoCrateVocabularyHelper
{

    public record VocabularyHelperResult(Map<Vocabulary, IVocabularyType> typeMap,
                                         Map<ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm, VocabularyTerm> termMap)
    {
    }

    public static VocabularyHelperResult mapVocabularyTypes(List<Vocabulary> vocabularies)
    {
        Map<Vocabulary, IVocabularyType> typeMap = new LinkedHashMap<>();
        Map<ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm, VocabularyTerm>
                termMap = new LinkedHashMap<>();

        for (Vocabulary vocabulary : vocabularies)
        {
            String vocabId = "openBIS:term" + vocabulary.getCode();

            List<IVocabularyTerm> collect = vocabulary.getTerms().stream()
                    .map(x -> new VocabularyTerm(vocabId + x.getCode(), x.getLabel(),
                            x.getDescription())).collect(
                            Collectors.toList());

            IVocabularyType vocabularyType =
                    new VocabularyType(vocabId, vocabulary.getDescription(), collect);
            typeMap.put(vocabulary, vocabularyType);
            for (ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm a : vocabulary.getTerms())
            {
                VocabularyTerm term =
                        new VocabularyTerm(a.getCode(), a.getLabel(), a.getDescription());
                termMap.put(a, term);
            }

        }

        return new VocabularyHelperResult(typeMap, termMap);
    }
}
