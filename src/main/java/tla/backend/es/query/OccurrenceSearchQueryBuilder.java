package tla.backend.es.query;

import java.util.List;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;

import tla.backend.es.model.TextEntity;
import tla.backend.service.ModelClass;
import tla.domain.model.SentenceToken.Lemmatization;

/**
 * look up lemma occurrences. Lemma occurrence counts per text are gonna be in
 * the result's {@link #AGG_ID_TEXT_IDS} aggregation.
 *
 * @see ESQueryResult#getAggregation(String)
 */
@ModelClass(TextEntity.class)
public class OccurrenceSearchQueryBuilder extends TextSearchQueryBuilder {

    public final static String AGG_ID_TEXT_IDS = "text_ids";

    /**
     * instantiates a query builder for searching lemma attestations, which is
     * basically a text search query builder fed by a sentence query builder
     * dependency with a text IDs aggregation and a nested token query looking
     * for the specified lemma.
     */
    public OccurrenceSearchQueryBuilder(String lemmaId) {
        super();
        SentenceSearchQueryBuilder sentenceQuery = sentenceQuery(lemmaId);
        this.dependsOn(
            sentenceQuery,
            this::setId,
            sentenceDependency -> sentenceDependency.getResult().getAggregation(
                AGG_ID_TEXT_IDS
            ).keySet().toArray(new String[]{})
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

    /**
     * create sentence query builder matching sentences containing specified lemma,
     * aggregating IDs of containing text entities in a terms aggregation named
     * {@link #AGG_ID_TEXT_IDS}.
     */
    static SentenceSearchQueryBuilder sentenceQuery(String lemmaId) {
        var query = new SentenceSearchQueryBuilder();
        query.setTokens(List.of(occurrenceTokenQuery(lemmaId)));
        query.aggregate(
            AggregationBuilders.terms(AGG_ID_TEXT_IDS).field(
                "context.textId"
            ).order(
                BucketOrder.key(true)
            ).size(
                ExpansionQueryBuilder.ID_AGG_SIZE
            )
        );
        return query;
    }

}
