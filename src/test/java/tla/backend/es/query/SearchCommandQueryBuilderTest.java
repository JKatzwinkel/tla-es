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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import tla.backend.es.model.meta.MappingTest;
import tla.domain.command.LemmaSearch;
import tla.domain.command.PassportSpec;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TextSearch;
import tla.domain.command.TranslationSpec;
import tla.domain.command.TypeSpec;
import tla.domain.command.PassportSpec.PassportSpecValue;
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
        cmd.setRoot("ḫzꜣ");
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        var json = toJsonObject(query);
        assertAll("lemma search ES query",
            //() -> assertEquals("", query.toJson()),
            () -> assertEquals(List.of("type"), read(json, "$.bool.must[*].term.type.value"), "type term query"),
            () -> assertEquals(List.of("d"), read(json, "$.bool.filter[*].prefix.id.value"), "prefix for demotic IDs"),
            () -> assertTrue(query.toJson().contains("ḫzꜣ"))
        );
    }

    @Test
    void lemmaSearchQueryTest_script() {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setScript(new Script[]{Script.HIERATIC});
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        assertAll("lemma ES search query excludes demotic lemma IDs",
            //() -> assertEquals("", query.toJson()),
            () -> assertTrue(query.toJson().contains("must_not\":[{\"prefix\":{\"id\":{\"value\":\"d\"}"))
        );
    }

    @Test
    void lemmaSearchQueryTest_transcription() {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setTranscription("nfr");
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        assertAll("lemma ES search query specifies transcription",
            () -> assertTrue(query.toJson().contains("nfr"))
        );
    }

    @Test
    void lemmaSearchQueryTest_annoType() {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setAnno(new TypeSpec("annoType", null));
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        assertAll("lemma ES search query specifies anno type",
            () -> assertTrue(query.toJson().contains("BTSAnnotation"))
        );
    }

    @Test
    void lemmaSearchQueryTest_bibliography() {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setBibliography("Wb 1, 130.1-5");
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        assertAll("lemma ES search query specifies bibliographic reference",
            () -> assertTrue(query.toJson().contains("Wb 1, 130.1-5"))
        );
    }

    @Test
    void lemmaSearchQuerySortOrderTest() {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setSort("root_desc");
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        var nativeQuery = query.buildNativeQuery(Pageable.unpaged());
        assertAll("lemma search query sort order should be in order",
            () -> assertEquals(
                "relations.root.name",
                nativeQuery.getSort().get().toList().get(0).getProperty()
            ),
            () -> assertEquals(
                Sort.Direction.DESC,
                nativeQuery.getSort().get().toList().get(0).getDirection()
            )
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

    @Test
    void textSearchQueryTest_thesaurusReferences() {
        TextSearch cmd = new TextSearch();
        var pps = new PassportSpec();
        var thsIds = List.of("LKQP7D6XGBGV5K52MIEFX2NWNI", "TCNODPO4NFBSRI7WQYIR23ALJI");
        pps.put(
            "object.description_of_object.type",
            PassportSpecValue.of(thsIds, false)
        );
        cmd.setPassport(pps);
        var query = modelMapper.map(cmd, TextSearchQueryBuilder.class);
        assertAll("text ES query specifies thesaurus references",
            thsIds.stream().map(
                thsId -> () -> {
                    assertTrue(query.toJson().contains(thsId), thsId);
                }
            )
        );
    }

    @Test
    void textSearchQueryTest_passportValues() {
        TextSearch cmd = new TextSearch();
        var pps = new PassportSpec();
        pps.put(
            "present_location.location.inventory_number",
            PassportSpecValue.of(List.of("E 3209"), null)
        );
        cmd.setPassport(pps);
        var query = modelMapper.map(cmd, TextSearchQueryBuilder.class);
        assertAll("text ES query specifies passport values",
            //() -> assertEquals("", query.toJson()),
            () -> assertTrue(query.toJson().contains(
                "passport.present_location.location.inventory_number\":{\"query\":\"E 3209\"}"
            ))
        );
    }

}
