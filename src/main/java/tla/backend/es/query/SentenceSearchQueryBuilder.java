package tla.backend.es.query;

import java.util.Collection;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.SentenceEntity;
import tla.backend.service.ModelClass;
import tla.domain.command.PassportSpec;

@Slf4j
@Getter
@ModelClass(SentenceEntity.class)
public class SentenceSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    public final static String AGG_ID_TEXT_IDS = "text_ids";

    public void setTokens(Collection<TokenSearchQueryBuilder> tokenQueries) {
        if (tokenQueries == null) {
            return;
        }
        BoolQuery tokenQuery = BoolQuery.of(
            bq -> bq.must(
                tokenQueries.stream().map(
                    query -> Query.of(
                        q -> q.nested(
                            n -> n.path("tokens").query(
                                nq -> nq.bool(
                                    query.getNativeRootQueryBuilder()
                                )
                            ).scoreMode(ChildScoreMode.None)
                        )
                    )
                ).toList()
            )
        );
        this.filter(Query.of(q -> q.bool(tokenQuery)));
    }

    public void setPassport(PassportSpec spec) {
        log.info("set sentence search passport specs");
        if (spec != null && !spec.isEmpty()) {
            log.info("spawn text search dependency");
            var textSearchQuery = new TextSearchQueryBuilder();
            textSearchQuery.setExpansion(true);
            textSearchQuery.setPassport(spec);
            this.dependsOn(
                textSearchQuery,
                this::setTextIds
            );
        }
    }

    public void setTextIds(Collection<String> textIds) {
        if (textIds != null) {
            log.info("sentence query: receive {} text IDs", textIds.size());
            this.filter(
                Query.of(
                    q -> q.terms(
                        t -> t.field("context.textId").terms(
                            tsb -> tsb.value(
                                textIds.stream().map(
                                    textId -> FieldValue.of(textId)
                                ).toList()
                            )
                        )
                    )
                )
            );
        }
    }

}