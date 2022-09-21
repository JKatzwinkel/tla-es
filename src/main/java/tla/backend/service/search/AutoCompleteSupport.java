package tla.backend.service.search;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class AutoCompleteSupport {

    public static final String[] QUERY_FIELDS = {
        "id", "name", "name._2gram", "name._3gram"
    };
    public static final String[] FETCH_FIELDS = {
        "id", "name", "type", "subtype", "eclass"
    };

    public static AutoCompleteSupport DEFAULT = new AutoCompleteSupport();

    @Singular
    private Map<String, Float> queryFields;

    @Builder.Default
    private String[] responseFields = FETCH_FIELDS;

    public AutoCompleteSupport() {
        this.queryFields = Arrays.asList(QUERY_FIELDS).stream().collect(
            Collectors.<String, String, Float>toMap(
                String::toString,
                field -> { return Float.valueOf(1); }
            )
        );
        this.responseFields = FETCH_FIELDS;
    }

    public AutoCompleteSupport(Map<String, Float> queryFields, String[] responseFields) {
        this();
        if (queryFields != null && !queryFields.isEmpty()) {
            queryFields.forEach(
                (field, boost) -> {
                    this.queryFields.merge(
                        field,
                        boost,
                        Float::sum
                    );
                }
            );
        }
        if (responseFields != null) {
            this.responseFields = Stream.concat(
                Arrays.stream(
                    responseFields
                ),
                Arrays.stream(
                    this.responseFields
                )
            ).distinct().toArray(
                String[]::new
            );
        }
    }

    /**
     * prepare multimatch query for autocomplete search.
     */
    protected MultiMatchQuery autoCompleteQueryBuilder(String term) {
        return MultiMatchQuery.of(
            mm -> mm.query(term).fields(
                this.queryFields.keySet().stream().toList()
            ).type(TextQueryType.BoolPrefix).query(
                term
            ).prefixLength(
                term.length()
            )
        );
    }

    /**
     * create native ES search query.
     */
    public NativeQuery autoCompleteQuery(String term, String type) {
        return new NativeQueryBuilder().withFields(
            this.getResponseFields()
        ).withFilter(
            (type != null && !type.isBlank()) ?
            Query.of(
                b -> b.term(
                    t -> t.field("type").value(type)
                )
            ) : Query.of(
                b -> b.bool(
                    q -> q
                )
            )
        ).withQuery(
            Query.of(
                q -> q.multiMatch(
                    this.autoCompleteQueryBuilder(term)
                )
            )
        ).withPageable(
            PageRequest.of(0, 15)
        ).build();
    }
 
}