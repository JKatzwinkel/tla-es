package tla.backend.es.repo;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumingThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import tla.backend.App;
import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.parts.EditDate;
import tla.backend.service.EntityService;

@SpringBootTest(classes = {App.class})
public class RepoTest {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private RepoPopulator repoPopulator;

    @Autowired
    private ElasticsearchOperations operations;

    @Test
    void repoDoesExist() {
        assertTrue(repo != null, "repo should not be null");
    }

    @Test
    void esRestTemplateAvailable() {
        assertTrue(operations != null, "elasticsearch operations should not be null");
    }

    @Test
    void repoPopulatorHasRegistry() {
        final var expectedKeys = List.of("lemma", "ths", "text", "comment", "annotation", "object", "sentence", "meta", "test");
        assertTrue(ModelConfig.isInitialized(), "entity model registered");
        repoPopulator.init();
        assertAll(
            () -> assertFalse(repoPopulator.repoIngestors.isEmpty(), "repo populator has registry"),
            () -> assertEquals(
                expectedKeys.size(),
                EntityService.getRegisteredModelClasses().size(),
                "all model classes have been registered by service"
            ),
            () -> assertEquals(
                expectedKeys.size(),
                repoPopulator.repoIngestors.size(),
                "all model classes have been registered by repo ingestor"
            ),
            () -> assertAll(
                EntityService.getRegisteredModelClasses().stream().map(
                    modelClass -> () -> {
                        assertTrue(
                            Indexable.class.isAssignableFrom(modelClass),
                            modelClass.getName() + " is an Indexable"
                        );
                        assumingThat(
                            Indexable.class.isAssignableFrom(modelClass),
                            () -> {
                                var key = ModelConfig.getIndexName(modelClass.asSubclass(Indexable.class));
                                assertTrue(
                                    expectedKeys.contains(key),
                                    key + " among expected keys"
                                );
                            }
                        );
                    }
                )
            ),
            () -> assertAll(
                expectedKeys.stream().map(
                    key -> () -> assertNotNull(repoPopulator.selectBatchIngestor(key), key + " batch ingestor registered")
                )
            ),
            () -> assertNull(repoPopulator.selectBatchIngestor("xxx"), "bogus batch ingestor not registered")
        );
    }

    @Test
    void indexLemmaSetup() {
        IndexOperations index = operations.indexOps(
            IndexCoordinates.of("lemma")
        );
        Settings lemmaIndexSettings = index.getSettings().flatten();
        assertAll("index lemma should exist and be configured correctly",
            () -> assertTrue(index.exists(), "index lemma should exist"),
            () -> assertTrue(!lemmaIndexSettings.isEmpty(), "lemma index should contain settings"),
            () -> assertTrue(lemmaIndexSettings.containsKey("index.max_result_window"), "settings should contain key `max_result_window`"),
            () -> assertEquals(100000, lemmaIndexSettings.get("index.max_result_window"))
        );
    }

    @Test
    void esDateConverterShouldParseLocalDate() {
        ElasticsearchDateConverter converter = ElasticsearchDateConverter.of("yyyy-MM-dd");
        EditDate localDate = converter.parse("2019-12-18", EditDate.class);
        assertAll("",
            () -> assertNotNull(localDate),
            () -> assertEquals("2019-12-18", localDate.toString())
        );
        assertDoesNotThrow(
            () -> converter.parse("2019-12-18", LocalDate.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testObjectsInCollectionsMappings() {
        Map<String, Object> corpusobjectmap = operations.indexOps(CorpusObjectEntity.class).getMapping();
        assertAll("corpus object class ES mapping",
            () -> assertNotNull(corpusobjectmap, "corpus obj mapping"),
            () -> assertTrue(corpusobjectmap.get("properties") instanceof Map),
            () -> assertTrue(((Map<?,?>) corpusobjectmap.get("properties")).get("paths") instanceof Map)
        );
        Map<String, Object> pathsMap = (Map<String, Object>) ((Map<?,?>) corpusobjectmap.get("properties")).get("paths");
        assertFalse(pathsMap.isEmpty());

        /* // relations, paths, externalreferences index mappings still need fixing

        Map<String, Object> expectMap = Map.of("properties", Map.of());
        assertAll("paths mapping (objectreference)",
            () -> assertTrue(pathsMap.size() > 1, "contains more than just `type`"),
            () -> assertFalse(!pathsMap.containsKey("type"), "mapping has type"),
            () -> assertNotEquals(expectMap, pathsMap)
        ); */
    }
}
