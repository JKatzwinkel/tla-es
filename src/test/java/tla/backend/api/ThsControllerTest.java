package tla.backend.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tla.backend.AbstractMockMvcTest;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.parts.ObjectReference;
import tla.backend.es.repo.ThesaurusRepo;
import tla.backend.service.ThesaurusService;


class ThsControllerTest extends AbstractMockMvcTest {

    @Autowired
    private ThesaurusService service;

    @MockitoBean
    private ThesaurusRepo repo;

    private ThsEntryEntity thsEntry;

    @BeforeEach
    void init() {
        this.thsEntry = ThsEntryEntity.builder()
        .id("1")
        .eclass("BTSThsEntry")
        .name("wadi")
        .type("findSpot")
        .relation("partOf", BaseEntity.Relations.of(
            ObjectReference.builder().id("2").name("region1").type("findSpot").eclass("BTSThsEntry").build(),
            ObjectReference.builder().id("3").name("region2").type("findSpot").eclass("BTSThsEntry").build()
        ))
        .build();
    }

    @Test
    void serviceInjected() {
        assertNotNull(service);
    }

    @Test
    void countRelations() {
        assertEquals(2, this.thsEntry.getRelations().get("partOf").size(), "predicate 'partOf' should have 2 values");
    }

    @Test
    void getRequestSingleEntry() throws Exception {
        when(repo.findById(anyString()))
            .thenReturn(
                Optional.of(thsEntry)
            );
        mockMvc.perform(
            get("/ths/get/xxx")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(
                status().isOk()
            )
            .andExpect(
                content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(jsonPath("$.doc.relations.partOf[0].id").value("2"));
    }

}
