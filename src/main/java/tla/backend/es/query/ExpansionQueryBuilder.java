package tla.backend.es.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

public interface ExpansionQueryBuilder extends TLAQueryBuilder {

    final static String ID_FIELD = "id";
    final static int ID_AGG_SIZE = 1000000;

    /**
     * If set to true, query is considered an expansion query, meaning that no paged results
     * are being fetched, and an ID aggregation is added instead.
     */
    public default void setExpansion(boolean expansion) {
        if (expansion) {
            this.aggregate(
                ESQueryResult.AGGS_ID_IDS,
                Aggregation.of(
                    a -> a.terms(
                        ta -> ta.field(ID_FIELD).size(ID_AGG_SIZE).order(
                            List.of(Map.of(ID_FIELD, SortOrder.Asc))
                        )
                    )
                )
            );
        }
    }

    public boolean isExpansion();

    public default void setRootIds(Collection<String> ids) {
        this.must(
            Query.of(
                q -> q.terms(
                    t -> t.field("paths.id.keyword").terms(
                        tsb -> tsb.value(
                            ids.stream().map(
                                id -> FieldValue.of(id)
                            ).toList()
                        )
                    )
                )
            )
        );
    }

    public List<String> getRootIds();

}