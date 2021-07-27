package tla.backend.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.mock.mockito.MockBean;

import tla.backend.AbstractMockMvcTest;
import tla.backend.es.model.Metadata;
import tla.backend.es.model.parts.EditDate;
import tla.backend.es.repo.MetadataRepo;

public class ApiControllerTest extends AbstractMockMvcTest {

    @MockBean
    private MetadataRepo metadataRepo;

    @Autowired
    private BuildProperties buildProperties;

    @Test
    void queryEndpointList() throws Exception {
        final var expectedPaths = List.of(
            "/lemma/count",
            "/version",
            "/ths/get/{id}"
        );
        var response = mockMvc.perform(get("/")).andExpect(
            status().isOk()
        ).andReturn();
        var content = response.getResponse().getContentAsString();
        assertAll("check for expected endpoint paths",
            expectedPaths.stream().map(
                path -> () -> assertTrue(content.contains(path), path)
            )
        );
    }

    @Test
    @DisplayName("request to version endpoint with no corpus metadata should return HTTP code 202")
    void queryVersionEndpointNotPopulated() throws Exception {
        when(metadataRepo.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/version")).andExpect(
            status().isAccepted()
        ).andExpect(
            jsonPath("$.version").value(buildProperties.getVersion())
        ).andExpect(
            jsonPath("$.release").value("n/a")
        );
    }

    @Test
    @DisplayName("version endpoint response should contain corpus data dump meta data info")
    void queryVersionEndpoint() throws Exception {
        var info = new Metadata();
        info.setDOI("10.5072/zenodo.716586");
        info.setId("v1.0.210115");
        info.setDate(EditDate.of(2021, 01, 15));
        info.setModelVersion("0.1.253-dev");
        info.setEtlVersion("0.1.280");
        info.setLingglossVersion("0.0.2");
        when(metadataRepo.findAll()).thenReturn(
            List.of(info)
        );
        mockMvc.perform(get("/version")).andExpect(
            status().isOk()
        ).andExpect(
            jsonPath("$.version").value(buildProperties.getVersion())
        ).andExpect(
            jsonPath("$.release").value(info.getId())
        ).andExpect(
            jsonPath("$.date").value(EditDate.DATE_CONVERTER.format(info.getDate()))
        ).andExpect(
            jsonPath("$.etlVersion").value(info.getEtlVersion())
        );
    }

}
