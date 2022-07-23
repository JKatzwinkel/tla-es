package tla.backend.es.query;

import java.util.List;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import tla.backend.es.model.SentenceEntity;
import tla.backend.service.ModelClass;
import tla.domain.model.SentenceToken.Lemmatization;

/**
 * create sentence query builder matching sentences containing specified lemma,
 * aggregating IDs of containing text entities in a terms aggregation named
 * {@link #AGG_ID_TEXT_IDS}.
 */
@ModelClass(SentenceEntity.class)
public class SentencesContainingLemmaOccurrenceQueryBuilder extends SentenceSearchQueryBuilder {

    public SentencesContainingLemmaOccurrenceQueryBuilder(String lemmaId) {
        super();
        setTokens(List.of(occurrenceTokenQuery(lemmaId)));
        aggregate(
            AGG_ID_TEXT_IDS,
            Aggregation.of(
                a -> a.terms(
                    ta -> ta.field("context.textId").size(
                        ExpansionQueryBuilder.ID_AGG_SIZE
                    )
                )
            )
        );
    }

    /**
     * create builder for nested query matching sentence tokens lemmatized with
     * specified lemma ID.
     */
    static TokenSearchQueryBuilder occurrenceTokenQuery(String lemmaId) {
        var tokenQuery = new TokenSearchQueryBuilder();
        tokenQuery.setLemma(
            new Lemmatization(lemmaId, null)
        );
        return tokenQuery;
    }
}
