package tla.backend.es.query;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.jayway.jsonpath.Configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.modelmapper.ModelMapper;

import tla.backend.es.model.meta.MappingTest;
import tla.domain.command.LemmaSearch;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TranslationSpec;
import tla.domain.command.TypeSpec;
import tla.domain.model.Language;
import tla.domain.model.Script;

@TestInstance(Lifecycle.PER_CLASS)
public class SearchCommandQueryBuilderTest {

    ModelMapper modelMapper;

    @BeforeAll
    void initModelMapper() {
        modelMapper = new MappingTest().getModelMapper();
    }

    static Object toJsonObject(TLAQueryBuilder query) {
        return Configuration.defaultConfiguration().jsonProvider().parse(
            query.toJson()
        );
    }

    @Test
    void queryJsonSerializationTest() throws Exception {
        LemmaSearch cmd = new LemmaSearch();
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        var json = query.toJson();
        assertEquals('{', json.charAt(0), "JSON serialization begins with curly bracket");
        assertEquals('}', json.charAt(json.length()-1), "JSON serialization ends with curly bracket");
        assertTrue(json.contains("\"bool\":"), "JSON serialization contains root query kind");
        assertDoesNotThrow(
            () -> read(toJsonObject(query), "$.bool.must"),
            "query serialization can be parsed and queried"
        );
    }

    @Test
    void lemmaSearchQueryTest() throws Exception {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setWordClass(new TypeSpec("type", "subtype"));
        cmd.setScript(new Script[]{Script.DEMOTIC});
        cmd.setTranslation(new TranslationSpec());
        cmd.getTranslation().setText("pferd");
        cmd.getTranslation().setLang(new Language[]{Language.DE});
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        var json = toJsonObject(query);
        assertAll("lemma search ES query",
            //() -> assertEquals("", query.toJson()),
            () -> assertEquals(List.of("type"), read(json, "$.bool.must[*].term.type.value"), "type term query"),
            () -> assertEquals(List.of("d"), read(json, "$.bool.filter[*].prefix.id.value"), "prefix for demotic IDs")
        );
    }

    @Test
    @SuppressWarnings("rawtypes")
    void sentenceSearchQueryTest() throws Exception {
        SentenceSearch cmd = new SentenceSearch();
        cmd.setTranslation(new TranslationSpec());
        cmd.getTranslation().setText("horse");
        cmd.getTranslation().setLang(new Language[]{Language.DE, Language.EN});
        SentenceSearch.TokenSpec t = new SentenceSearch.TokenSpec();
        t.setTranslation(new TranslationSpec());
        t.getTranslation().setText("pferd");
        t.getTranslation().setLang(new Language[]{Language.DE});
        cmd.setTokens(List.of(t));
        var query = modelMapper.map(cmd, SentenceSearchQueryBuilder.class);
        var json = toJsonObject(query);
        assertAll("sentence search ES query",
            //() -> assertEquals("", query.toJson()),
            () -> assertEquals(
                2, ((List)read(json, "$.bool.filter[*].bool.should[*].match.keys()")).size(),
                "2 query clauses for sentence translation"
            ),
            () -> assertEquals(
                List.of("pferd"),
                read(
                    json,
                    "$.bool.filter[*].bool.must[*].nested.query.bool.filter[*].bool.should[*].match['tokens.translations.de'].query"
                ),
                "nested token translation filter query clause"
            )
        );
    }

}
