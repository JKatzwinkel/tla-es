package tla.backend.es.query;

import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.Getter;
import tla.backend.es.model.LemmaEntity;
import tla.backend.service.ModelClass;
import tla.domain.command.TypeSpec;
import tla.domain.model.Script;

@Getter
@ModelClass(LemmaEntity.class)
public class LemmaSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    static List<FacetSpec> facetSpecs = List.of(
        FacetSpec.field("wordClass.type", "type"),
        FacetSpec.field("wordClass.subtype", "subtype"),
        FacetSpec.script(
            "script",
            "if (doc['id'].value.startsWith('d')) {return 'demotic';} if (!doc['type'].value.equals('root')) {return 'hieratic';}"
        )
    );

    public void setScript(List<Script> scripts) {
        if (scripts == null) {
            return;
        }
        Query scriptFilter = null;
        if (!scripts.contains(Script.HIERATIC)) {
            if (scripts.contains(Script.DEMOTIC)) {
                scriptFilter = Query.of(
                    q -> q.prefix(
                        pq -> pq.field("id").value("d")
                    )
                );
            }
        } else {
            if (!scripts.contains(Script.DEMOTIC)) {
                scriptFilter = Query.of(
                    q -> q.bool(
                        bq -> bq.mustNot(
                            iq -> iq.prefix(
                                p -> p.field("id").value("d")
                            )
                        )
                    )
                );
            }
        }
        this.filter(scriptFilter);
    }

    public void setTranscription(String transcription) {
        if (transcription != null) {
            this.must(
                Query.of(
                    q -> q.match(
                        mq -> mq.field("words.transcription.unicode").query(transcription)
                    )
                )
            );
        }
    }

    public void setWordClass(TypeSpec wordClass) {
        Query query = null;
        if (wordClass == null) {
            return;
        }
        if (wordClass.getType() != null) {
            if (wordClass.getType().equals("excl_names")) { // TODO
                query = Query.of(
                    q -> q.bool(
                        bq -> bq.mustNot(
                            iq -> iq.term(
                                t -> t.field("type").value("entity_name")
                            )
                        )
                    )
                ); // TODO: ?
            } else if (wordClass.getType().equals("any")) {
            } else if (!wordClass.getType().isBlank()) {
                query = Query.of(
                    q -> q.term(
                        tq -> tq.field("type").value(wordClass.getType())
                    )
                );
            }
        }
        if (wordClass.getSubtype() != null) {
            query = Query.of(
                q -> q.term(
                    tq -> tq.field("subtype").value(wordClass.getSubtype())
                )
            );
        }
        this.must(query);
    }

    public void setRoot(String transcription) { // TODO spawn join query
        if (transcription != null) {
            this.must(
                Query.of(
                    q -> q.match(
                        m -> m.field("relations.root.name").query(transcription)
                    )
                )
            );
        }
    }

    public void setAnno(TypeSpec annotationType) { // TODO spawn join query
        BoolQuery query = BoolQuery.of(q -> q);
        if (annotationType != null) {
            if (annotationType.getType() != null) {
                if (!annotationType.getType().isBlank()) {
                    query.must().add(
                        Query.of(
                            q -> q.term(
                                t -> t.field("relations.contains.eclass").value("BTSAnnotation")
                            )
                        )
                    );
                }
            }
        }
        this.must(
            Query.of(q -> q.bool(query))
        );
    }

    public void setBibliography(String bibliography) {
        if (bibliography != null) {
            this.must(
                Query.of(
                    q -> q.match(
                        m -> m.field(
                            "passport.bibliography.bibliographical_text_field"
                        ).query(bibliography).operator(
                            Operator.And
                        )
                    )
                )
            );
        }
    }

    public void setSort(String sort) {
        super.setSort(sort);
        if (sortSpec.field.equals("root")) {
            sortSpec.field = "relations.root.name";
        }
    }

}