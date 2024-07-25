package tla.backend.api;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


class SentenceControllerTest extends AbstractMockMvcTest {

    @MockitoBean
    private SentenceRepo sentenceRepo;

    @MockitoBean
    private TextRepo textRepo;

    @MockitoBean
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
        var text = Util.loadSampleFile(TextEntity.class, textId);
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(
                text
            )
        );
        when(
            textRepo.findAllById(anyCollection())
        ).thenReturn(
            List.of(text)
        );
        when(
            annoRepo.findAllById(anyCollection())
        ).thenReturn(
            List.of(
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
            jsonPath(String.format("$.related.BTSAnnotation.%s.id", annoId)).value(annoId)
        ).andExpect(
            jsonPath("$.doc.tokens[0].flexion.lingGloss").value("N.f:sg")
        );
    }

}