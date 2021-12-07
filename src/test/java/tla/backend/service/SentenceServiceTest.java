package tla.backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

import tla.backend.App;
import tla.backend.Util;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.AnnotationRepo;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.es.repo.TextRepo;
import tla.backend.es.repo.ThesaurusRepo;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TranslationSpec;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.model.Language;

@SpringBootTest(classes = {App.class})
public class SentenceServiceTest {

    @MockBean
    private SentenceRepo sentenceRepo;

    @MockBean
    private TextRepo textRepo;

    @MockBean
    private AnnotationRepo annoRepo;

    @MockBean
    private ThesaurusRepo thsRepo;

    @Autowired
    private SentenceService sentenceService;

    @MockBean
    private ElasticsearchOperations operations;

    @Test
    void getSentenceSearchResultsWrapper() throws Exception {
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        var text = Util.loadSampleFile(TextEntity.class, textId);
        when(
            textRepo.findAllById(anyCollection())
        ).thenReturn(
            List.of(
                text
            )
        );
        String annoId = "IBUBd0kXx8hvzU9vuxAKWNHnf6s";
        var rubrum = Util.loadSampleFile(AnnotationEntity.class, annoId);
        when(
            annoRepo.findAllById(anyCollection())
        ).thenReturn(
            List.of(rubrum)
        );
        SentenceEntity s = Util.loadSampleFile(SentenceEntity.class, "IBUBd3QvPWhrgk50h3u3Wv5PmdA");
        SearchHit<SentenceEntity> hit = new SearchHit<>(
            null, null, null, 1f, null, null, null, null, null, null, s
        );
        SearchHits<SentenceEntity> hits = new SearchHitsImpl<>(1, TotalHitsRelation.EQUAL_TO, 1f, null, List.of(hit), null, null);
        SentenceSearch cmd = new SentenceSearch();
        TranslationSpec translation = new TranslationSpec();
        translation.setText("Blut der ersten Geburt der Kobra");
        translation.setLang(new Language[]{Language.DE});
        cmd.setTranslation(translation);
        var query = sentenceService.getSearchCommandAdapter(cmd);
        assertNotNull(query);
        when(
            operations.search(
                any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(SentenceEntity.class),
                any()
            )
        ).thenReturn(
            hits
        );
        SearchResultsWrapper<?> dto = sentenceService.runSearchCommand(cmd, PageRequest.of(1, 20)).orElseThrow();
        assertAll("test search results in container",
            () -> assertNotNull(dto.getResults(), "contains results"),
            () -> assertEquals(1, dto.getResults().size(), "exactly 1 sentence")
        );
        assertAll("test sentence search results container",
            () -> assertNotNull(dto.getRelated(), "related objects"),
            () -> assertEquals(Set.of("BTSText", "BTSAnnotation"), dto.getRelated().keySet(), "both partOf and contains objects injected"),
            () -> assertEquals(Set.of(textId), dto.getRelated().get("BTSText").keySet(), "1 related text"),
            () -> assertNull(dto.getRelated().get("BTSComment"), "no comments"),
            () -> assertEquals(text.toDTO().toJson(), dto.getRelated().get("BTSText").get(textId).toJson(), "text as expected"),
            () -> assertEquals(rubrum.toDTO().toJson(), dto.getRelated().get("BTSAnnotation").get(annoId).toJson(), "rubrum as expected")
        );
    }

    @Test
    void getSentenceDetailsWrapper() throws Exception {
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        String sentenceId = "IBUBd3QvPWhrgk50h3u3Wv5PmdA";
        String annoId = "IBUBd0kXx8hvzU9vuxAKWNHnf6s";
        SentenceEntity s = Util.loadSampleFile(SentenceEntity.class, sentenceId);
        TextEntity t = Util.loadSampleFile(TextEntity.class, textId);
        AnnotationEntity rubrum = Util.loadSampleFile(AnnotationEntity.class, annoId);
        ThsEntryEntity tm = Util.loadSampleFile(ThsEntryEntity.class, "LJHMV4523JB7NBGAJRDTS3H27Y");
        when(
            thsRepo.findAllById(anyCollection())
        ).thenReturn(List.of(tm));
        when(
            sentenceRepo.findById(sentenceId)
        ).thenReturn(
            Optional.of(s)
        );
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(t)
        );
        when(
            textRepo.findAllById(anyCollection())
        ).thenReturn(
            List.of(t)
        );
        when(
            annoRepo.findById(annoId)
        ).thenReturn(
            Optional.of(rubrum)
        );
        SingleDocumentWrapper<?> result = sentenceService.getDetails(sentenceId);
        assertAll("result ok",
            () -> assertNotNull(result, "not null"),
            () -> assertNotNull(result.getDoc(), "payload found"),
            () -> assertNotNull(result.getRelated(), "related objects list"),
            () -> assertFalse(result.getRelated().isEmpty(), "contains related objects"),
            () -> assertTrue(result.getRelated().containsKey("BTSThsEntry"), "related objects contain thesaurus terms")
        );
    }

}
