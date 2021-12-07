package tla.backend.es.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;

import lombok.Getter;
import tla.backend.es.model.meta.Indexable;
import tla.domain.dto.extern.PageInfo;

/**
 * ES search hits container with paging information.
 *
 * <code>&lt;T&gt;</code>: an {@link Indexable} entity class
 */
@Getter
public class ESQueryResult<T extends Indexable> {

    /**
     * How many search results fit in one single page.
     */
    public static final int SEARCH_RESULT_PAGE_SIZE = 20;

    /**
     * IDs aggregation identifier
     */
    public static final String AGGS_ID_IDS = "ids";

    private Map<String, Map<String, Long>> aggregations;

    private SearchHits<T> hits;

    private PageInfo pageInfo;

    public ESQueryResult() {
        this.aggregations = new HashMap<>();
    }

    public ESQueryResult(SearchHits<T> hits, Pageable page) {
        this();
        this.hits = hits;
        this.pageInfo = page.isUnpaged() ? null : pageInfo(hits, page);
    }

    /**
     * if there is an IDs aggregation, extract IDs from it.
     */
    public Collection<String> getIDAggValues() {
        return this.getAggregation(AGGS_ID_IDS).keySet();
    }

    /**
     * extract a terms aggregation of the specified name.
     *
     * @return map of aggregated terms and corresponding document counts, or an empty map if the
     * aggregation doesn't exist
     */
    public Map<String, Long> getAggregation(String agg) {
        Aggregations aggregations = (Aggregations) this.hits.getAggregations().aggregations();
        if (aggregations == null || aggregations.get(agg) == null) {
            return Collections.emptyMap();
        }
        return (
            (Terms) aggregations.get(agg)
        ).getBuckets().stream().collect(
            Collectors.toMap(
                Terms.Bucket::getKeyAsString, Terms.Bucket::getDocCount
            )
        );
    }

    /**
     * save terms aggregation results.
     */
    public void addAggregations(Map<String, Map<String, Long>> aggs) {
        this.aggregations.putAll(aggs);
    }

    /**
     * Create a serializable transfer object containing page information
     * about an ES search result.
     */
    public static PageInfo pageInfo(SearchHits<?> hits, Pageable pageable) {
        return PageInfo.builder()
            .number(pageable.getPageNumber())
            .totalElements(hits.getTotalHits())
            .size(SEARCH_RESULT_PAGE_SIZE)
            .numberOfElements(
                Math.min(
                    SEARCH_RESULT_PAGE_SIZE,
                    hits.getSearchHits().size()
                )
            ).totalPages(
                (int) hits.getTotalHits() / SEARCH_RESULT_PAGE_SIZE + (
                    hits.getTotalHits() % SEARCH_RESULT_PAGE_SIZE < 1 ? 0 : 1
                )
            ).build();
    }

}