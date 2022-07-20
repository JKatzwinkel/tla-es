package tla.backend.es.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import tla.domain.model.SentenceToken.Lemmatization;

public class TokenSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    @Override
    public String nestedPath() {
        return "tokens.";
    }

    public void setLemma(Lemmatization lemma) {
        if (lemma != null && !lemma.isEmpty()) {
            this.must(
                Query.of(
                    q -> q.term(
                        t -> t.field(
                            String.format("%slemma.id", this.nestedPath())
                        ).value(lemma.getId())
                    )
                )
            );
        }
    }

}
