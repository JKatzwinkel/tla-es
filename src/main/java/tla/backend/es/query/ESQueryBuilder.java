package tla.backend.es.query;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.domain.model.meta.AbstractBTSBaseClass;

/**
 * This is an abstract search command adapter.
 */
@Slf4j
@Getter
public abstract class ESQueryBuilder implements TLAQueryBuilder {

    /**
     * The desired DTO type in which search results ought to be sent.
     */
    private Class<? extends AbstractBTSBaseClass> dtoClass;

    private BoolQuery nativeRootQueryBuilder;
    private Map<String, Aggregation> nativeAggregationBuilders;
    protected SortSpec sortSpec = SortSpec.DEFAULT;

    private List<TLAQueryBuilder.QueryDependency<?>> dependencies;

    private ESQueryResult<?> result;

    public ESQueryBuilder() {
        this.nativeRootQueryBuilder = BoolQuery.of(q ->q);
        this.nativeAggregationBuilders = new HashMap<>();
        this.dependencies = new LinkedList<>();
    }

    /**
     * Put together an actual Elasticsearch query ready for execution.
     */
    public NativeQuery buildSearchQuery(Pageable page) {
        var qb = new NativeQueryBuilder().withQuery(
            q -> q.bool(getNativeRootQueryBuilder())
        ).withPageable(
            page
        ).withSort(
            this.getSortSpec().primary()
        );
        log.info("query: {}", this.getNativeRootQueryBuilder());
        this.getNativeAggregationBuilders().forEach(
            (name, agg) -> {
                log.info("add aggregation to query: {}", agg);
                qb.withAggregation(name, agg);
            }
        );
        return qb.build();
    }

    public ESQueryResult<?> setResult(ESQueryResult<?> result) {
        this.result = result;
        return result;
    }

    public void setDTOClass(Class<? extends AbstractBTSBaseClass> dtoClass) {
        log.info("set DTO of search command adapter: {}", dtoClass);
        this.dtoClass = dtoClass;
    }

    public void setId(List<String> ids) {
        if (ids != null) {
            log.info("add {} IDs to query", ids.size());
            this.filter(
                Query.of(
                    b -> b.ids(i -> i.values(ids))
                )
            );
        }
    }

    public void setEditor(String name) {
        if (name != null) {
            this.must(
                Query.of(
                    q -> q.bool(
                        b -> b.should(
                            s -> s.match(m -> m.field("editors.author").query(name))
                        ).should(
                            s -> s.match(m -> m.field("editors.contributors").query(name))
                        )
                    )
                )
            );
        }
    }

    public void setSort(String sort) {
        log.info("receive sort order: {}", sort);
        this.sortSpec = SortSpec.from(sort);
    }

}