package tla.backend.es.query;

import tla.backend.es.model.TextEntity;
import tla.backend.es.query.dependencies.TextIdsOfSentencesContainingLemmaDependency;
import tla.backend.service.ModelClass;

/**
 * ES query builder for searching texts containing occurrences of specific lemma.
 * Lemma occurrence counts per text are gonna be in
 * the result's {@link #AGG_ID_TEXT_IDS} aggregation.
 *
 * @see ESQueryResult#getAggregation(String)
 */
@ModelClass(TextEntity.class)
public class TextsContainingLemmaOccurrenceQueryBuilder extends TextSearchQueryBuilder {

    /**
     * instantiates a query builder for searching lemma attestations, which is
     * basically a text search query builder fed by a sentence query builder
     * dependency with a text IDs aggregation and a nested token query looking
     * for the specified lemma.
     */
    public TextsContainingLemmaOccurrenceQueryBuilder(String lemmaId) {
        super();
        this.dependsOn(new TextIdsOfSentencesContainingLemmaDependency(this, lemmaId));
    }

}
