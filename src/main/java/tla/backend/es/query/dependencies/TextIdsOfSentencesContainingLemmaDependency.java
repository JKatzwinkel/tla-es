package tla.backend.es.query.dependencies;

import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.SentencesContainingLemmaOccurrenceQueryBuilder;
import tla.backend.es.query.TLAQueryBuilder.QueryDependency;

/**
 * dependency on a {@link SentencesContainingLemmaOccurrenceQueryBuilder} query which searches
 * for sentences containing tokens lemmatized with a specified lemma ID. The text IDs of the
 * sentences found are used as input for the {@link ESQueryBuilder#setId} method of the waiting
 * query.
 *
 * @author jkatzwinkel
 */
public class TextIdsOfSentencesContainingLemmaDependency extends QueryDependency<String []> {

    public TextIdsOfSentencesContainingLemmaDependency(ESQueryBuilder query, String lemmaId) {
        super(
            new SentencesContainingLemmaOccurrenceQueryBuilder(lemmaId),
            query::setId,
            sentenceDependency -> sentenceDependency.getResult().getAggregation(
                SentencesContainingLemmaOccurrenceQueryBuilder.AGG_ID_TEXT_IDS
            ).keySet().toArray(new String[]{})
        );
    }

}
