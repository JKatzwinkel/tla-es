package tla.backend.es.model.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.modelmapper.ModelMapper;

import tla.backend.Util;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.parts.Token;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.domain.command.PassportSpec;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TextSearch;
import tla.domain.dto.SentenceDto;
import tla.domain.dto.ThsEntryDto;

@TestInstance(Lifecycle.PER_CLASS)
public class MappingTest {

    private ModelMapper modelMapper;

    public ModelMapper getModelMapper() {
        if (modelMapper == null) {
            modelMapper = ModelConfig.initModelMapper();
        }
        return modelMapper;
    }

    public static Token getSampleToken() throws Exception {
        return tla.domain.util.IO.getMapper().readValue(
            """
                {
                    "annoTypes": [
                        "rubrum"
                    ],
                    "flexion": {
                        "btsGloss": "n/a",
                        "lingGloss": "N.f:pl",
                        "numeric": 3
                    },
                    "glyphs": {
                        "mdc": "X1:V31"
                    },
                    "id": "IBUBd3bnVNl3VyEPm0EUdogMKw8",
                    "label": "k,t",
                    "lemma": {
                        "POS": {
                            "subtype": "substantive_fem",
                            "type": "substantive"                                                                                                                                                                                                                   },                                                                                                                                                                                                                                          "id": "162830"
                    },                                                                                                                                                                                                                                          "transcription": {                                                                                                                                                                                                                              "mdc": "k,t",
                        "unicode": "k,t"
                    },
                    "translations": {
                        "de": [
                            "etwas anderes"
                        ]
                    },
                    "type": "word"
                }
            """, Token.class
        );
    }

    @BeforeAll
    void init() {
        getModelMapper();
    }

    @Test
    @DisplayName("test mapping sentence entity to DTO")
    void testSentenceEntityMapping() throws Exception {
        var sentence = new SentenceEntity();
        sentence.setTokens(
            List.of(getSampleToken())
        );
        sentence.setId("𓃱");
        var dto = modelMapper.map(sentence, SentenceDto.class);
        assertEquals("substantive_fem", dto.getTokens().get(0).getLemma().getPartOfSpeech().getSubtype());
    }

    @Test
    void passportSearchCommandMapping() {
        PassportSpec pp = new PassportSpec();
        pp.put("date", PassportSpec.ThsRefPassportValue.of(List.of("XX"), true));
        var ppp = modelMapper.map(pp, PassportSpec.class);
        assertNotNull(ppp);
        TextSearch command = new TextSearch();
        command.setPassport(pp);
        var ttt = modelMapper.map(command, TextSearchQueryBuilder.class);
        assertNotNull(ttt.getPassport());
        assertEquals(1, ttt.getPassport().size());
    }

    @Test
    void sentenceSearchCommandMapping() {
        SentenceSearch cmd = new SentenceSearch();
        var qb = modelMapper.map(cmd, SentenceSearchQueryBuilder.class);
        assertNotNull(qb);
    }

    @Test
    void objectReferenceEquality() throws Exception {
        ThsEntryEntity ths = Util.loadSampleFile(ThsEntryEntity.class, "E7YEQAEKZVEJ5PX7WKOXY2QEEM");
        ThsEntryDto dto = modelMapper.map(ths, ThsEntryDto.class);
        tla.domain.model.ObjectReference dtoRef = tla.domain.model.ObjectReference.from(dto);
        tla.domain.model.ObjectReference refDto = ths.toDTOReference();
        assertEquals(refDto, dtoRef, "DTO-style object reference extracted from entity should equal object reference extracted from DTO");
    }

}