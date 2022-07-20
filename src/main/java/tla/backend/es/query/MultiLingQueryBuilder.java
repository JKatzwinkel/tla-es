package tla.backend.es.query;


import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import tla.domain.command.TranslationSpec;
import tla.domain.model.Language;

public interface MultiLingQueryBuilder extends TLAQueryBuilder {

    public default void setTranslation(TranslationSpec translation) {
        BoolQuery translationsQuery = BoolQuery.of(q -> q);
        if (translation != null && translation.getLang() != null) {
            var termSpecified = translation.getText() != null && !translation.getText().isBlank();
            for (Language lang : translation.getLang()) {
                translationsQuery.should().add(
                    termSpecified
                    ? Query.of(
                        q -> q.match(
                            m -> m.field(
                                String.format("%stranslations.%s", this.nestedPath(), lang)
                            ).query(
                                translation.getText()
                            )
                        )
                    ) : Query.of(
                        q -> q.exists(
                            e -> e.field(
                                String.format("%stranslations.%s", this.nestedPath(), lang)
                            )
                        )
                    )
                );
            }
        }
        this.filter(Query.of(q -> q.bool(translationsQuery)));
    }

}
