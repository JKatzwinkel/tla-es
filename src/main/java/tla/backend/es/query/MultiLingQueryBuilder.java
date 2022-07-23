package tla.backend.es.query;


import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import tla.domain.command.TranslationSpec;
import tla.domain.model.Language;

public interface MultiLingQueryBuilder extends TLAQueryBuilder {

    public default void setTranslation(TranslationSpec translation) {
        if (translation == null || translation.getLang() == null) {
            return;
        }
        Function<Language, QueryVariant> generator;
        var termSpecified = translation.getText() != null && !translation.getText().isBlank();
        if (termSpecified) {
            generator = lang -> MatchQuery.of(
                q -> q.field(
                    String.format("%stranslations.%s", this.nestedPath(), lang)
                ).query(
                    translation.getText()
                )
            );
        } else {
            generator = lang -> ExistsQuery.of(
                q -> q.field(
                    String.format("%stranslations.%s", this.nestedPath(), lang)
                )
            );
        }
        List<Query> translationQueries = Arrays.asList(
            translation.getLang()
        ).stream().map(
            lang -> new Query(generator.apply(lang))
        ).toList();
        this.filter(
            Query.of(
                q -> q.bool(
                    BoolQuery.of(
                        bq -> bq.should(translationQueries)
                    )
                )
            )
        );
    }

}
