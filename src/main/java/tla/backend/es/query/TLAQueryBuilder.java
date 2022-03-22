package tla.backend.es.query;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.service.ModelClass;

/**
 * Implementations must be annotated with {@link ModelClass}.
 */
public interface TLAQueryBuilder {

    /**
     * Represents a dependency relationship to a search query builder, consisting of the query builder
     * instance, a method belonging to the dependent query builder waiting for results, and a functional
     * producer generating the input for the waiting method, i.e. some function returning results from
     * the prerequisite query's {@link ESQueryResult} from {@link TLAQueryBuilder#getResult()}.
     */
    @Slf4j
    public static class QueryDependency<D> {

        /**
         * query that must run first
         */
        @Getter
        private TLAQueryBuilder query;

        /**
         * method waiting for query execution result
         */
        private Consumer<D> dependentMethod;

        /**
         * dependency's method providing us with the result we need
         */
        private Function<TLAQueryBuilder, D> inputMethod;

        public QueryDependency(TLAQueryBuilder query, Consumer<D> blockedMethod, Function<TLAQueryBuilder, D> blockingMethod) {
            log.info("{}: adding dependency on method {} in query adapter {}", this, blockingMethod, query);
            this.query = query;
            this.dependentMethod = blockedMethod;
            this.inputMethod = blockingMethod;
        }

        /**
         * invoke inputMethod on query dependency and feed the return value into the
         * dependent method of the owning query.
         */
        public void resolve() {
            log.info("Invoke blocking method {} of query adapter {}..", this.inputMethod, this.query);
            var input = this.inputMethod.apply(this.query);
            log.info("Feed return value {} into blocked method {}..", input, this.dependentMethod);
            this.dependentMethod.accept(input);
        }

    }

    /**
     * Adds another native search query builder instance to this builder's dependency list.
     * Dependencies are executed first, and a producer function feeds the result of their execution
     * into a consumer method belonging to this builder instance.
     *
     * @param query the search query builder to depend on
     * @param blockedMethod consumer method waiting for query execution results
     * @param blockingMethod producer function feeding input into the consumer
     */
    public default <D> TLAQueryBuilder dependsOn(
        TLAQueryBuilder query, Consumer<D> blockedMethod, Function<TLAQueryBuilder, D> blockingMethod
    ) {
        return this.dependsOn(new QueryDependency<D>(query, blockedMethod, blockingMethod));
    }

    /**
     * Adds a dependency to the IDs aggregation of an expansion query.
     *
     * @param query search query builder on which we wait to be executed
     * @param blockedMethod method of the dependent query waiting to consume the execution result's IDs aggregation values
     * @see #dependsOn(TLAQueryBuilder, Consumer, Function)
     */
    public default TLAQueryBuilder dependsOn(
        ExpansionQueryBuilder query, Consumer<Collection<String>> blockedMethod
    ) {
        return this.dependsOn(
            query, blockedMethod, dep -> dep.getResult().getIDAggValues()
        );
    }

    /**
     * Add a dependency.
     */
    public default <D> TLAQueryBuilder dependsOn(QueryDependency<D> dependency) {
        this.getDependencies().add(dependency);
        return this;
    }

    public List<QueryDependency<?>> getDependencies();

    /**
     * Resolve dependencies and return list of native query builders in a sequence 
     */
    public default List<QueryDependency<?>> resolveDependencies() {
        List<QueryDependency<?>> deps = new LinkedList<QueryDependency<?>>();
        for (QueryDependency<?> dep : this.getDependencies()) {
            deps.addAll(
                dep.getQuery().resolveDependencies()
            );
            deps.add(dep);
        }
        return deps;
    }

    /**
     * ES root query builder which queries for individual properties get added to
     * either as <code>must</code> or <code>should</code> clause, or as a <code>filter</code>.
     */
    public BoolQueryBuilder getNativeRootQueryBuilder();

    /**
     * Create JSON string representation of native ES query.
     */
    public default String toJson() {
        return this.getNativeRootQueryBuilder().toString();
    }

    public List<AbstractAggregationBuilder<?>> getNativeAggregationBuilders();

    /**
     * returns ES search hits and page information wrapped together into one object.
     */
    public ESQueryResult<?> getResult();
    public ESQueryResult<?> setResult(ESQueryResult<?> result);

    /**
     * Add criterion to root query's <code>must</code> clause list.
     */
    default BoolQueryBuilder must(QueryBuilder clause) {
        return this.getNativeRootQueryBuilder().must(clause);
    }
    /**
     * Add criterion to root query's <code>should</code> clause list.
     */
    default BoolQueryBuilder should(QueryBuilder clause) {
        return this.getNativeRootQueryBuilder().should(clause);
    }
    /**
     * add filter
     */
    default BoolQueryBuilder filter(QueryBuilder criterion) {
        return this.getNativeRootQueryBuilder().filter(criterion);
    }

    /**
     * add aggregation
     */
    default AbstractAggregationBuilder<?> aggregate(AbstractAggregationBuilder<?> agg) {
        this.getNativeAggregationBuilders().add(agg);
        return agg;
    }

    public default Class<? extends Indexable> getModelClass() {
        for (Annotation a : this.getClass().getAnnotationsByType(ModelClass.class)) {
            return ((ModelClass) a).value();
        }
        return null;
    }

    /**
     * queries for elements with a nested mapping should override this and
     * return the actual path where they are nested (ending with the <code>"."</code> path delimiter).
     */
    default String nestedPath() {
        return "";
    }

}