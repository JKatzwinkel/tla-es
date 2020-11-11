package tla.backend.api;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import tla.backend.AbstractMockMvcTest;
import tla.backend.Util;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.AnnotationRepo;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.es.repo.TextRepo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


public class SentenceControllerTest extends AbstractMockMvcTest {

    @MockBean
    private SentenceRepo sentenceRepo;

    @MockBean
    private TextRepo textRepo;

    @MockBean
    private AnnotationRepo annoRepo;

    @ParameterizedTest
    @ValueSource(strings = {"CDWYGHBII5C37IBETSSI6RCIDQ"})
    void deserializeTextEntity(String textId) throws Exception {
        TextEntity t = Util.loadSampleFile(TextEntity.class, textId);
        assertAll("text deserialized",
            () -> assertNotNull(t, "not null"),
            () -> assertNotNull(t.getPaths(), "object paths instantiated")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"IBUBd3QvPWhrgk50h3u3Wv5PmdA"})
    void deserializeSentenceFromFile(String sentenceId) throws Exception {
        SentenceEntity s = Util.loadSampleFile(SentenceEntity.class, sentenceId);
        assertAll("check deserialized sentence entity",
            () -> assertNotNull(s, "not null"),
            () -> assertNotNull(s.getTokens(), "has tokens")
        );
    }

    @Test
    void getDetails() throws Exception {
        String sentenceId = "IBUBd3QvPWhrgk50h3u3Wv5PmdA";
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        String annoId = "IBUBd0kXx8hvzU9vuxAKWNHnf6s";
        when(
            sentenceRepo.findById(sentenceId)
        ).thenReturn(
            Optional.of(
                Util.loadSampleFile(SentenceEntity.class, sentenceId)
            )
        );
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(
                Util.loadSampleFile(TextEntity.class, textId)
            )
        );
        when(
            annoRepo.findById(annoId)
        ).thenReturn(
            Optional.of(
                Util.loadSampleFile(AnnotationEntity.class, annoId)
            )
        );
        mockMvc.perform(
            get(String.format("/sentence/get/%s", sentenceId)).contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            status().isOk()
        ).andExpect(
            content().contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            jsonPath(String.format("$.related.BTSText.%s.id", textId)).value(textId)
        ).andExpect(
            jsonPath("$.doc.tokens[0].flexion.lingGloss").value("N.f:sg")
        );
    }

}