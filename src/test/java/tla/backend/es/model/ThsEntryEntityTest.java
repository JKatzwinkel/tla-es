package tla.backend.es.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tla.backend.es.model.parts.Translations;
import tla.domain.model.Language;
import tla.domain.model.Passport;

public class ThsEntryEntityTest {

    private static ObjectMapper objectMapper = tla.domain.util.IO.getMapper();

    @Test
    @DisplayName("thesaurus entry synonyms extracted from passport should add up if language specifier occurs multiple times")
    void extractSynonymsMultiplePerLanguage() throws Exception {
        String passportJson = """
            {"synonyms": [
                {
                    "synonym_group": [
                        {"language": ["en"], "synonym": ["synonym1"]},
                        {"language": ["en"], "synonym": ["synonym2"]},
                        {"language": ["fr"]}
                    ]
                }
            ]}""";
        Passport pp = objectMapper.readValue(passportJson, Passport.class);
        ThsEntryEntity entity = ThsEntryEntity.builder().id("1").passport(pp).build();
        Translations synonyms = entity.extractTranslationsFromPassport();
        assertEquals(List.of("synonym1", "synonym2"), synonyms.get(Language.EN));
    }
}
