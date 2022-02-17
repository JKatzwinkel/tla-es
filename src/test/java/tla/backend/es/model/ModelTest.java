package tla.backend.es.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.annotations.Document;

import tla.backend.App;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity.Relations;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.model.parts.EditDate;
import tla.backend.es.model.parts.EditorInfo;
import tla.backend.es.model.parts.ObjectReference;
import tla.backend.es.model.parts.PartOfSpeech;
import tla.backend.es.model.parts.Token;
import tla.backend.es.model.parts.Token.Flexion;
import tla.backend.es.model.parts.Token.Lemmatization;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Translations;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.CorpusObjectDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.SentenceDto;
import tla.domain.dto.TextDto;
import tla.domain.dto.ThsEntryDto;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.dto.meta.DocumentDto;
import tla.domain.model.Language;
import tla.domain.model.Passport;
import tla.domain.model.SentenceToken;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.util.IO;

@SpringBootTest(classes = {App.class})
public class ModelTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void modelConfigInitialized() {
        List<Class<? extends AbstractBTSBaseClass>> modelClasses = ModelConfig.getModelClasses();
        assertAll("make sure model config class has been initialized",
            () -> assertTrue(ModelConfig.isInitialized(), "flag should be set"),
            () -> assertNotNull(modelClasses, "model class list should not be null"),
            () -> assertNotNull(ModelConfig.getEclassConfigs(), "model configurations registry expected"),
            () -> assertEquals(
                modelClasses.size(),
                ModelConfig.getEclassConfigs().size(),
                String.format(
                    "number of model class configurations registered should be the same as model classes known to ModelConfig (%s)",
                    String.join(
                        ", ",
                        modelClasses.stream().map(Class::getName).toList()
                    )
                )
            )
        );
    }

    @BTSeClass("BTSMadeUpModel")
    @Document(indexName = "made_up_index_name")
    private static class CorrectlyAnnotatedDummyModelClass extends TLAEntity {}

    @Document(indexName = "made_up_index_name")
    private static class IncorrectlyAnnotatedDummyModelClass extends TLAEntity {}

    @Test
    void registerModelClass() throws Exception {
        int numberOfRegisteredModels = ModelConfig.getModelClasses().size();
        int numberOfRegisteredModelConfigs = ModelConfig.getEclassConfigs().size();
        try {
            Map<String, ModelConfig.BTSeClassConfig> conf = ModelConfig.registerModelClass(
                CorrectlyAnnotatedDummyModelClass.class
            );
            assertAll("model class config should be extracted and registered",
                () -> assertNotNull(conf, "configuration expected"),
                () -> assertTrue(conf.containsKey("BTSMadeUpModel"), "expect eclass"),
                () -> assertEquals("made_up_index_name", conf.get("BTSMadeUpModel").getIndex(), "expect index name"),
                () -> assertEquals(CorrectlyAnnotatedDummyModelClass.class, conf.get("BTSMadeUpModel").getModelClass(), "expect class"),
                () -> assertEquals(numberOfRegisteredModels + 1, ModelConfig.getModelClasses().size(), "expect one more known model class"),
                () -> assertEquals(numberOfRegisteredModelConfigs + 1, ModelConfig.getEclassConfigs().size(), "expect one more registered config"),
                () -> assertEquals(conf.get("BTSMadeUpModel").getIndex(), ModelConfig.getIndexName(CorrectlyAnnotatedDummyModelClass.class)),
                () -> assertEquals(CorrectlyAnnotatedDummyModelClass.class, ModelConfig.getModelClass("BTSMadeUpModel"))
            );
        } catch (Exception e) {
            //log.warn("model class registration failed for {}", CorrectlyAnnotatedDummyModelClass.class.getName());
            throw e;
        }
    }

    @Test
    void registerInvalidModelClass() {
        assertThrows(
            Exception.class,
            () -> ModelConfig.registerModelClass(
                IncorrectlyAnnotatedDummyModelClass.class
            )
        );
    }

    @Test
    void lookupModelClassForUnknownEclass() {
        assertThrows(
            NullPointerException.class,
            () -> ModelConfig.getModelClass("nonexistentEclass")
        );
    }

    @Test
    void entitySuperClass_equality() throws Exception {
        Indexable lemma = LemmaEntity.builder().id("ID").build();
        Indexable term = ThsEntryEntity.builder().id("ID").build();
        assertAll("entities of different subclass with same ID should not be equal",
            () -> assertNotEquals(lemma, term, "lemma 'ID' should not equal ths term 'ID'")
        );
    }

    @Test
    void translationsEqual() throws Exception {
        assertTrue(mapper != null, "entitymapper should not be null");
        Translations t1 = Translations.builder().de(List.of("übersetzung")).en(List.of("translation", "meaning")).build();
        Translations t2 = Translations.builder().de(Arrays.asList("übersetzung")).en(Arrays.asList("translation", "meaning")).build();
        Translations t3 = mapper.readValue("{\"de\": [\"übersetzung\"], \"en\": [\"translation\", \"meaning\"]}", Translations.class);
        assertAll("translations objects should be equal",
            () -> assertEquals(t2, t1, "translation instances should be equal regardless of build method parameter type"),
            () -> assertEquals(t3, t1, "deserialized instance should be equal to builder-instantiated"),
            () -> assertNull(t1.getFr(), "builder-built french translations array should be null"),
            () -> assertNull(t3.getFr(), "deserialized french translations array should be null"),
            () -> assertEquals(t3.hashCode(), t1.hashCode(), "hashcodes equal"),
            () -> assertNull(t3.get(null), "undefined language causes null")
        );
    }

    @Test
    void editInfoEqual() throws Exception {
        EditorInfo e1 = mapper.readValue(
            "{\"author\":\"author\", \"updated\":\"2019-12-18\"}",
            EditorInfo.class
        );
        EditorInfo e2 = mapper.readValue(
            mapper.writeValueAsString(e1),
            EditorInfo.class
        );
        assertAll("test EditorInfo equality",
            () -> assertEquals(e1, e2, "instances as a whole should be equal"),
            () -> assertEquals(e1.getUpdated(), e2.getUpdated(), "edit dates should be equals")
        );
    }

    @Test
    void thesaurusEntriesEqual() throws Exception {
        ThsEntryEntity t_built = ThsEntryEntity.builder()
            .id("1")
            .eclass("BTSThsEntry")
            .sortKey("1")
            .SUID("gd7")
            .editors(EditorInfo.builder().author("author").updated(EditDate.of(2015, 12, 31)).build())
            .build();
        ThsEntryEntity t_read = mapper.readValue(
            "{\"id\":\"ID\",\"hash\":\"gd7\",\"eclass\":\"BTSThsEntry\",\"sort_string\":\"1\",\"editors\":{\"author\":\"author\",\"updated\":\"2015-12-31\"}}",
            ThsEntryEntity.class
        );
        ThsEntryEntity t_round = mapper.readValue(mapper.writeValueAsString(t_built), ThsEntryEntity.class);
        assertAll("thesaurus entry instances should be equal regardless of creation method",
            () -> assertNotEquals(t_built, t_read, "deserialized instance should not be the same as built instance"),
            () -> assertEquals(t_built, t_round, "built instance should remain the same after serialization and deserialization via ES entity mapper"),
            () -> assertEquals(t_built.getEditors(), t_read.getEditors(), "edit infos should be equal"),
            () -> assertEquals(t_built.hashCode(), t_round.hashCode(), "hashcode same after deserializing serialization")
        );
    }

    @Test
    void thesrausEntryDeserialize_mapping() throws Exception {
        ThsEntryEntity t = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/ths/E7YEQAEKZVEJ5PX7WKOXY2QEEM.json",
            ThsEntryEntity.class
        );
        assertAll("synonyms should be extracted from passport and converted into translations",
            () -> assertNotNull(t.getSUID(), "short ID"),
            () -> assertNotNull(t.getTranslations()),
            () -> assertNotNull(t.getTranslations().getFr()),
            () -> assertEquals(1, t.getTranslations().getFr().size()),
            () -> assertEquals("21e dynastie", t.getTranslations().getFr().get(0)),
            () -> assertNotNull(t.getPaths(), "object paths"),
            () -> assertFalse(t.getPaths().length < 1, "path not empty")
        );
        AbstractDto dto = ModelConfig.toDTO(t);
        assertAll("test thesaurus entity to DTO mapping",
            () -> assertTrue(dto instanceof ThsEntryDto),
            () -> assertNotNull(((ThsEntryDto) dto).getReviewState(), "review status must not be null"),
            () -> assertNotNull(((ThsEntryDto) dto).getTranslations(), "translations must not be null"),
            () -> assertTrue(!((ThsEntryDto) dto).getTranslations().isEmpty(), "translations not empty"),
            () -> assertNotNull(((ThsEntryDto) dto).getPaths(), "object paths"),
            () -> assertTrue(!((ThsEntryDto) dto).getPaths().isEmpty(), "paths not empty")
        );
    }

    @Test
    void lemmaRegistered() throws Exception {
        assertAll("has lemma model class registered with modelconfig?",
            () -> assertEquals(LemmaEntity.class, ModelConfig.getModelClass("BTSLemmaEntry"), "check registered model class"),
            () -> assertEquals("BTSLemmaEntry", ModelConfig.getEclass(LemmaEntity.class), "check registered eclass"),
            () -> assertEquals("lemma", ModelConfig.getIndexName(LemmaEntity.class), "cehck lemma index name")
        );
    }

    @Test
    void lemmaEntriesEqual() throws Exception {
        LemmaEntity l_built = LemmaEntity.builder()
            .id("1")
            .passport(new Passport())
            .build();
        LemmaEntity l_read = mapper.readValue(
            "{\"id\":\"1\",\"passport\":{}}",
            LemmaEntity.class
        );
        LemmaEntity l_round = mapper.readValue(
            mapper.writeValueAsString(l_built),
            LemmaEntity.class
        );
        assertAll("lemma entry instances should be equal regardless of creation method",
            () -> assertEquals("BTSLemmaEntry", l_built.getEclass(), "superclass getEclass() method should return registered eClass value"),
            () -> assertEquals(IO.json(l_built), IO.json(l_round)),
            () -> assertEquals(l_built, l_read, "deserialized lemma instance should be equal to built instance with the same properties"),
            () -> assertEquals(l_built, l_round, "lemma instance serialized and then deserialized should equal itself"),
            () -> assertEquals(l_built.hashCode(), l_round.hashCode(), "hashcode stays the same after deserializing serialization")
        );
    }

    @Test
    void deserializeLemma() throws Exception {
        LemmaEntity l = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/lemma/31610.json",
            LemmaEntity.class
        );
        assertAll("deserialize lemma entity from JSON file",
            () -> assertNotNull(l),
            () -> assertNotNull(l.getPassport()),
            () -> assertNotNull(l.getWords()),
            () -> assertNotNull(l.getWords().get(0).getGlyphs().getMdc()),
            () -> assertNotNull(l.getWords().get(0).getFlexion().getBtsGloss(), "lemma word flexion bts glossing")
        );
    }

    @Test
    void lemmaModelMapping() {
        Passport p = new Passport();
        Passport n = new Passport("leaf value");
        p.add("key", n);
        LemmaEntity l = LemmaEntity.builder()
            .id("Id")
            .eclass("BTSLemmaEntry")
            .name("nfr")
            .type("subst")
            .revisionState("published")
            .sortKey("Id")
            .timeSpan(new LemmaEntity.AttestedTimeSpan(-2375, -30))
            .passport(p)
            .translations(Translations.builder().de(List.of("übersetzung")).build())
            .word(
                new Token(
                    "N35:G47",
                    new Transcription("nfr", "nfr")
                )
            )
            .build();
        DocumentDto dto = (DocumentDto) l.toDTO();
        assertTrue(dto instanceof LemmaDto);
        LemmaDto d = (LemmaDto) dto;
        assertAll("lemma entity should be mapped to DTO correctly",
            () -> assertEquals(l.getRevisionState(), d.getReviewState(), "review status should be present"),
            () -> assertEquals(l.getSortKey(), d.getSortKey(), "sort key should be copied"),
            () -> assertNotNull(d.getTimeSpan(), "time span"),
            () -> assertEquals(-2375, d.getTimeSpan().getBegin(), "first year"),
            () -> assertEquals(-30, d.getTimeSpan().getEnd(), "last year"),
            () -> assertTrue(!d.getTranslations().isEmpty(), "translations should not be empty"),
            () -> assertEquals(1, l.getWords().size(), "expect 1 word"),
            () -> assertEquals("N35:G47", l.getWords().get(0).getGlyphs().getMdc(), "hieroglyphs MdC mapped"),
            () -> assertNotNull(d.getWords().get(0).getTranscription(), "word should have transcription"),
            () -> assertTrue(!d.getPassport().isEmpty(), "passport not empty"),
            () -> assertEquals(List.of("key"), d.getPassport().getFields(), "passport key set")
        );
    }

    @Test
    void annotationClassRegistered() {
        assertAll("annotation model class registered?",
            () -> assertEquals("BTSAnnotation", ModelConfig.getEclass(AnnotationEntity.class)),
            () -> assertEquals(AnnotationEntity.class, ModelConfig.getModelClass("BTSAnnotation")),
            () -> assertEquals("annotation", ModelConfig.getIndexName(AnnotationEntity.class))
        );
    }

    @Test
    void annotationModelMapping() throws Exception {
        AnnotationEntity a = AnnotationEntity.builder()
            .id("ID")
            .eclass("BTSAnnotation")
            .name("nfr")
            .revisionState("published")
            .passport(Passport.of(Map.of("lemma", Collections.emptyMap())))
            .build();
        AnnotationDto d = AnnotationDto.class.cast(a.toDTO());
        assertAll("test annotation entity to DTO conversion",
            () -> assertEquals(a.getEclass(), d.getEclass()),
            () -> assertEquals(a.getName(), d.getName()),
            () -> assertEquals(a.getPassport(), d.getPassport()),
            () -> assertEquals(a.getRevisionState(), d.getReviewState()),
            () -> assertTrue(d instanceof AnnotationDto)
        );
    }

    @Test
    void annotationDeserializeFromFile() throws Exception {
        AnnotationEntity a = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/annotation/2Y6NIZZWUJG7XAT3Y63A6WICA4.json",
            AnnotationEntity.class
        );
        assertAll("test annotation deserialization from JSON file",
            () -> assertNotNull(a),
            () -> assertEquals("Annotation zu $jzr$", a.getName()),
            () -> assertEquals(1, a.getPassport().extractProperty("annotation.lemma").size()),
            () -> assertTrue(a.getPassport().extractProperty("annotation.lemma").get(0).getLeafNodeValue().length() > 100)
        );
        AnnotationEntity b = mapper.readValue(
            mapper.writeValueAsString(a),
            AnnotationEntity.class
        );
        assertAll("after serialization and deserialization, annotation object should be still the same",
            () -> assertEquals(a, b, "instance"),
            () -> assertEquals(a.hashCode(), b.hashCode(), "hashcode"),
            () -> assertNotNull(b.getBody(), "body"),
            () -> assertEquals(a.getBody(), b.getBody(), "body"),
            () -> assertEquals(a.toString(), b.toString(), "tostring"),
            () -> assertEquals(
                tla.domain.util.IO.json(a),
                tla.domain.util.IO.json(b),
                "serialization"
            )
        );
    }

    @Test
    void textDeserializeFromFile() throws Exception {
        TextEntity t = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/text/2A5EGGJVHVFVVL42QSWVLJORYE.json",
            TextEntity.class
        );
        assertAll("test text deserialization from JSON file",
            () -> assertNotNull(t),
            () -> assertNotNull(t.getPaths()),
            () -> assertEquals("bbawarchive", t.getCorpus()),
            () -> assertEquals(t.getEditors().getUpdated(), t.getEditors().getCreated(), "creation & edit date"),
            () -> assertEquals(5, t.getEditors().getUpdated().getLong(ChronoField.DAY_OF_WEEK), "date day of week")
        );
    }

    @Test
    void textMapping() throws Exception {
        TextEntity t = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/text/2A5EGGJVHVFVVL42QSWVLJORYE.json",
            TextEntity.class
        );
        DocumentDto d = (DocumentDto) t.toDTO();
        assertAll("test text to DTO mapping",
            () -> assertNotNull(d),
            () -> assertTrue(d instanceof TextDto),
            () -> assertEquals(t.getRevisionState(), d.getReviewState()),
            () -> assertNotNull(((TextDto) d).getPaths(), "object paths"),
            () -> assertTrue(((TextDto) d).getPaths().size() > 0, "indeed paths"),
            () -> assertFalse(
                d.getEditors().getUpdated().after(d.getEditors().getCreated()),
                "update same day as creation"
            ),
            () -> assertEquals(14, ((TextDto) d).getWordCount().getMax(), "word count")
        );
        TextDto dto = (TextDto) d;
        assertAll("test mapped text DTO properties",
            () -> assertNotNull(dto.getPaths())
        );
    }

    @Test
    void corpusobjectFromFileMapping() throws Exception {
        CorpusObjectEntity o = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/object/2ABHCXHF5BCG3PDW7VSPA77K7U.json",
            CorpusObjectEntity.class
        );
        assertAll("test corpus object deserialization from JSON file",
            () -> assertNotNull(o),
            () -> assertNotNull(o.getEditors(), "editor info not null"),
            () -> assertNotNull(o.getEditors().getUpdated(), "edit date no null"),
            () -> assertNotNull(o.getPaths(), "corpus paths"),
            () -> assertTrue(o.getPaths().length > 0, "paths deserialized"),
            () -> assertTrue(!o.getPaths()[0].isEmpty(), "corpus object path contains elements"),
            () -> assertEquals("bbawarchive", o.getCorpus())
        );
        DocumentDto d = (DocumentDto) o.toDTO();
        assertAll("test corpus object to DTO mapping",
            () -> assertNotNull(d),
            () -> assertTrue(d instanceof CorpusObjectDto),
            () -> assertNotNull(((CorpusObjectDto) d).getPaths()),
            () -> assertEquals(o.getRevisionState(), d.getReviewState()),
            () -> assertNotNull(d.getEditors(), "DTO edit info not null"),
            () -> assertNotNull(d.getEditors().getUpdated(), "DTO edit date not null")
        );
    }

    @Test
    void commentDeserializeSimple() throws Exception {
        CommentEntity c = mapper.readValue(
            "{\"eclass\":\"BTSComment\",\"id\":\"ID\",\"body\":\"comment\",\"relations\":{\"partOf\":[{\"id\":\"1\",\"eclass\":\"BTSText\",\"ranges\":[{\"from\":\"a\",\"to\":\"b\"}]}]}}",
            CommentEntity.class
        );
        assertAll("comment deserialization ok",
            () -> assertNotNull(c, "instance not null"),
            () -> assertNotNull(c.getBody(), "body"),
            () -> assertEquals("comment", c.getBody(), "content"),
            () -> assertNotNull(c.getRelations(), "relations"),
            () -> assertEquals(1, c.getRelations().size(), "relation predicate count"),
            () -> assertNotNull(c.getRelations().get("partOf"), "predicate partof"),
            () -> assertEquals(1, c.getRelations().get("partOf").size(), "1 relation"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0), "first relation"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0).getRanges(), "text range"),
            () -> assertTrue(!c.getRelations().get("partOf").get(0).getRanges().isEmpty(), "text range"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0).getRanges().get(0).getStart(), "text range boundary left"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0).getRanges().get(0).getEnd(), "text range boundary right")
        );
        CommentEntity c2 = CommentEntity.builder()
            .id(c.getId())
            .eclass(c.getEclass())
            .body(c.getBody())
            .relations(c.getRelations())
            .build();
        assertAll("equality",
            () -> assertEquals(c, c2, "instances"),
            () -> assertEquals(c.toString(), c2.toString(), "str repr"),
            () -> assertEquals(c.hashCode(), c2.hashCode(), "hashcode")
        );
    }

    @Test
    void sentenceDeserialize() throws Exception {
        SentenceEntity s = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/sentence/IBYCcRHLQNYWZE3htMe7qAXwMmY.json",
            SentenceEntity.class
        );
        assertAll("deserialize sentence",
            () -> assertNotNull(s, "sentence instance"),
            () -> assertNotNull(s.getTokens(), "contains words"),
            () -> assertTrue(!s.getTokens().isEmpty(), "tokens not empty"),
            () -> assertNull(s.getType(), "type")
        );
        var c = s.getContext();
        assertAll("sentence context should deserialize correctly",
            () -> assertEquals(151, c.getPosition(), "sentence position within text"),
            () -> assertEquals(1, c.getVariants(), "variant count"),
            () -> assertEquals("Text", c.getTextType(), "text type"),
            () -> assertEquals("[59,1]", c.getLine(), "line count info"),
            () -> assertEquals("Eb 365", c.getParagraph(), "paragraph info")
        );
        SentenceEntity s2 = SentenceEntity.builder()
            .relations(
                Map.of(
                    "partOf", Relations.of(
                        ObjectReference.builder().id(
                            "REED47N2PNGD5HULYWF4VHCJPA"
                        ).eclass("BTSText").name(
                            "55,20-64,5 = Eb 336-431: \"Sammelhandschrift f\u00fcr die Augen\" (Das Augenbuch)"
                        ).type("Text").build()
                    ),
                    "contains", Relations.of(
                        ObjectReference.builder().id(
                            "PCMAZFOU7BCZ7EZ6XKPESL56ZI"
                        ).eclass("BTSText").name(
                            "Paginierung: Seite 59"
                        ).type("subtext").build()
                    )
                )
            ).id("IBYCcRHLQNYWZE3htMe7qAXwMmY").context(s.getContext())
            .tokens(s.getTokens()).translations(s.getTranslations()).transcription(s.getTranscription())
            .build();
        assertAll("sentence component equality",
            () -> {
                List.of("contains", "partOf").forEach(
                    key -> assertEquals(s2.getRelations().get(key), s.getRelations().get(key), key)
                );
            },
            () -> assertEquals(s.getContext(), s2.getContext(), "context"),
            () -> assertEquals(s.toString(), s2.toString(), "tostring")
        );
    }

    @Test
    void mapSentenceToDTO() throws Exception {
        Lemmatization l = new Lemmatization();
        SentenceEntity.Context c = SentenceEntity.Context.builder()
            .textId("textId").line("[1]").position(90).build();
        l.setPartOfSpeech(new PartOfSpeech("substantive", "masc"));
        Flexion f = new Flexion();
        f.setNumeric(3L);
        f.setBtsGloss("n/a");
        Token t = new Token();
        t.setFlexion(f);
        t.setLemma(l);
        t.setTranslations(Translations.builder().de(List.of("bedeutung")).build());
        t.setAnnoTypes(List.of("rubrum"));
        SentenceEntity s = SentenceEntity.builder()
            .id("ID")
            .relation(
                "partOf",
                BaseEntity.Relations.of(
                    ObjectReference.builder().id("textid").eclass("BTSText").type("Text").name("papyrus").build()
                )
            ).context(c)
            .transcription(new Transcription("nfr", "nfr"))
            .translations(Translations.builder().de(List.of("uebersetzung")).build())
            .tokens(List.of(t))
            .build();
        s.setType("HS");
        SentenceDto dto = (SentenceDto) ModelConfig.toDTO(s);
        assertAll("test sentence entity to DTO mapping",
            () -> assertNotNull(dto, "instance"),
            () -> assertNotNull(dto.getTranscription(), "transcription"),
            () -> assertEquals("nfr", dto.getTranscription().getUnicode(), "transcription unicode"),
            () -> assertNotNull(dto.getTranslations(), "translations"),
            () -> assertTrue(dto.getTranslations().containsKey(Language.DE), "german translation"),
            () -> assertEquals(List.of("uebersetzung"), dto.getTranslations().get(Language.DE), "translation value"),
            () -> assertNotNull(dto.getTokens(), "tokens"),
            () -> assertEquals(1, dto.getTokens().size(), "1 token"),
            () -> assertNotNull(dto.getContext(), "sentence context in DTO"),
            () -> assertEquals(s.getContext().getLine(), dto.getContext().getLine(), "lc"),
            () -> assertEquals(s.getContext().getPosition(), dto.getContext().getPosition(), "sentence position"),
            () -> assertEquals("HS", dto.getType(), "type")
        );
        SentenceToken tdto = dto.getTokens().get(0);
        assertAll("test sentence token to DTO mapping",
            () -> assertNotNull(tdto, "token"),
            () -> assertNotNull(tdto.getFlexion(), "flexion"),
            () -> assertEquals(3L, tdto.getFlexion().getNumeric(), "flexcode"),
            () -> assertEquals("n/a", tdto.getFlexion().getBtsGloss(), "bts glossing"),
            () -> assertNotNull(tdto.getLemma(), "lemmatization"),
            () -> assertNotNull(tdto.getLemma().getPartOfSpeech(), "lemma POS"),
            () -> assertNotNull(tdto.getLemma().getPartOfSpeech().getType(), "lemma POS type"),
            () -> assertTrue(tdto.getAnnoTypes().contains("rubrum"), "token is rubrum")
        );
    }

}