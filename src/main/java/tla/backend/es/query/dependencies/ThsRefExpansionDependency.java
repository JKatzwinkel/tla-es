package tla.backend.es.query.dependencies;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import tla.backend.es.query.PassportIncludingQueryBuilder;
import tla.backend.es.query.TLAQueryBuilder;
import tla.backend.es.query.ThsSearchQueryBuilder;
import tla.domain.command.PassportSpec;

@Slf4j
public class ThsRefExpansionDependency extends TLAQueryBuilder.QueryDependency<PassportSpec> {

    public ThsRefExpansionDependency(
        TLAQueryBuilder query,
        Consumer<PassportSpec> blockedMethod,
        Function<TLAQueryBuilder, PassportSpec> blockingMethod
    ) {
        super(query, blockedMethod, blockingMethod);
    }

    /**
     * Queue a new query builder for retrieval of the IDs of all thesaurus entries descending from
     * those specified.
     *
     * This adds a new expansion-mode {@link ThsSearchQueryBuilder} to the dependency list of the waiting
     * {@link PassportIncludingQueryBuilder}. This dependency will retrieve the IDs of all thesaurus
     * entries descending of any of the thesaurus entries specified (i.e. who are located within the
     * subtrees under the specified thesaurus entries). The results are fed into the waiting query builder's
     * {@link PassportIncludingQueryBuilder#setPassport(PassportSpec)} method.
     *
     */
    public static ThsRefExpansionDependency of(
        PassportIncludingQueryBuilder waitingQuery,
        String passportKey,
        Collection<String> thsIds
    ) {
        ThsSearchQueryBuilder expansionQuery = new ThsSearchQueryBuilder();
        expansionQuery.setRootIds(thsIds.toArray(new String[]{}));
        expansionQuery.setExpansion(true);
        return new ThsRefExpansionDependency(
            expansionQuery,
            waitingQuery::setPassport,
            query -> {
                log.info("result aggregations: {}", query.getResult().getHits().getAggregations());
                var expanded = new PassportSpec();
                expanded.put(
                    passportKey,
                    PassportSpec.ThsRefPassportValue.of(
                        query.getResult().getIDAggValues(),
                        false
                    )
                );
                return expanded;
            }
        );
    }

}
