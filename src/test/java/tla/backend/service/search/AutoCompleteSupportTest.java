package tla.backend.service.search;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

public class AutoCompleteSupportTest {

    @Test
    void testDefaultAutocompleteConfig() {
        AutoCompleteSupport ac = AutoCompleteSupport.DEFAULT;
        Query query = ac.autoCompleteQueryBuilder("thut");
        assertAll("make sure assumptions about default autocomplete config are correct",
            () -> assertEquals(4, ac.getQueryFields().size(), "match 4 fields"),
            () -> assertEquals(5, ac.getResponseFields().length, "return 5 result fields"),
            // TODO
            //() -> assertEquals(4, query.fields().size(), "multi match query matches as many fields as config")
            () -> assertTrue(query.isCombinedFields(), "autocomplete query is combined fields")
        );
    }

    @Test
    void testBoostCalculation() {
        AutoCompleteSupport ac = AutoCompleteSupport.builder()
            .queryField("name", 1F)
            .queryField("name", 1F)
            .responseFields(new String[]{"name"})
            .build();
        assertAll("test custom autocomplete support multimatch query configurations",
            () -> assertEquals(2F, ac.getQueryFields().get("name"), "extra boost on name field"),
            () -> assertEquals(
                Map.of(
                    "name._3gram", 1F,
                    "name._2gram", 1F,
                    "name", 2F,
                    "id", 1F
                ),
                ac.getQueryFields(),
                "field boost settings counted only once"
            ),
            () -> assertFalse(
                Arrays.stream(ac.getResponseFields()).distinct().count()
                < Arrays.stream(ac.getResponseFields()).count(),
                "expect no duplicate of search request source field `name`"
            ),
            () -> assertEquals(
                AutoCompleteSupport.FETCH_FIELDS.length,
                ac.getResponseFields().length,
                "custom autocomplete config contains default response fields"
            ),
            () -> assertTrue(
                Arrays.stream(ac.getResponseFields()).anyMatch(field -> field.equals("type")),
                "`type` field part of responsefields"
            )
        );
    }

    @Test
    void testQueryGeneration() {
        AutoCompleteSupport ac = AutoCompleteSupport.builder()
            .queryFields(
                Map.of("hash", 2F)
            ).responseFields(new String[]{"hash"})
            .build();
        Query query = ac.autoCompleteQueryBuilder("thut");
        assertAll("test multimatch query generation based on autocomplete config",
            // TODO
            // () -> assertEquals(
            //     AutoCompleteSupport.QUERY_FIELDS.length + 1,
            //     query.fields().size(),
            //     "1 custom query field"
            // ),
            // () -> assertEquals(
            //     ac.getQueryFields(),
            //     query.fields(),
            //     "query contains all configured fields and boost values"
            // ),
            () -> assertEquals(
                AutoCompleteSupport.FETCH_FIELDS.length + 1,
                ac.getResponseFields().length,
                "autocomplete config contains 1 extra response field"
            )
        );
    }

    @Test
    void testEmptyInput() {
        Query q = AutoCompleteSupport.DEFAULT.autoCompleteQueryBuilder("");
        assertAll("check out query with empty input string",
            () -> assertNotNull(q, "returned instance nonetheless")
            // TODO
            //() -> assertTrue(q.prefixLength() < 1, "has no requirements for prefix length")
        );
    }

    @Test
    void emptyQueryFields() {
        AutoCompleteSupport ac = AutoCompleteSupport.builder().queryField(
            "translations.*", 1F
        ).clearQueryFields()
        .responseFields(null)
        .build();
        Query q = ac.autoCompleteQueryBuilder("thut");
        assertAll("config and query built based on empty config inputs",
            () -> assertNotNull(ac, "construction successful"),
            () -> assertTrue(ac.getQueryFields().size() > 1, "default query field config intact"),
            () -> assertNotNull(ac.getResponseFields(), "successfully handled null input"),
            () -> assertTrue(ac.getResponseFields().length > 1, "default response field config intact"),
            () -> assertNotNull(q, "did build query"),
            () -> assertNotNull(q.combinedFields().fields(), "query contains match fields")
        );
    }

}