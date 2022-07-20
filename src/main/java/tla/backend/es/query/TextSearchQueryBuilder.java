package tla.backend.es.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.TextEntity;
import tla.backend.service.ModelClass;

@Slf4j
@Getter
@ModelClass(TextEntity.class)
public class TextSearchQueryBuilder extends PassportIncludingQueryBuilder implements ExpansionQueryBuilder {

    public static final String AGG_ID_DATE = "passport.date.date.date";

    private boolean expansion;

    private List<String> rootIds;

    public TextSearchQueryBuilder() {
        this.aggregate(
            AGG_ID_DATE,
            Aggregation.of(
                a -> a.terms(
                    ta -> ta.field(
                        String.format("%s.id.keyword", AGG_ID_DATE)
                    ).size(1000).order(
                        List.of(Map.of(AGG_ID_DATE, SortOrder.Asc))
                    )
                )
            )
        );
    }

    @Override
    public void setExpansion(boolean expansion) {
        log.info("text query: set IDs aggregation");
        ExpansionQueryBuilder.super.setExpansion(expansion);
        this.expansion = expansion;
    }

    @Override
    public void setRootIds(Collection<String> ids) {
        // TODO Auto-generated method stub
    }

}
