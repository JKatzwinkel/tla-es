package tla.backend.es.query;

import java.util.List;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;

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
            AggregationBuilders.terms(AGG_ID_TEXT_IDS).field(
                "context.textId"
            ).order(
                BucketOrder.key(true)
            ).size(
                ExpansionQueryBuilder.ID_AGG_SIZE
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
