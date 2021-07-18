package tla.backend.es.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.App;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.service.EntityService;
import tla.backend.service.ModelClass;
import tla.domain.command.SearchCommand;
import tla.domain.dto.meta.DocumentDto;
import tla.domain.model.meta.BTSeClass;

@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(classes = {App.class})
public class RepoPopulatorTest {

    @Autowired
    private RepoPopulator repoPopulator;

    @Autowired
    private TestRepo repo;

    @Autowired
    private ElasticsearchOperations operations;

    private TestService testService;

    @BTSeClass("TestEntity")
    @Document(indexName = "test", createIndex = false)
    static class TestEntity extends TLAEntity {}

    @Service
    @ModelClass(value = TestEntity.class, path = "test")
    public class TestService extends EntityService<TestEntity, ElasticsearchRepository<TestEntity, String>, DocumentDto> {
        @Override
        public ElasticsearchRepository<TestEntity, String> getRepo() {
            return repo;
        }
        @Override
        public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<DocumentDto> command) {
            return null;
        }
    }

    @BeforeAll
    void init() {
        testService = new TestService();
    }

    @AfterEach
    void deleteIndex() {
        operations.indexOps(TestEntity.class).delete();
    }

    @Test
    @DisplayName("test domain entity class should be registered with model config")
    void testDomainModelRegistry() {
        List<String> classes = EntityService.getRegisteredModelClasses().stream().map(
            modelClass -> modelClass.getSimpleName()
        ).collect(Collectors.toList());
        assertTrue(classes.contains("TestEntity"));
    }

    @Test
    @DisplayName("repo populator should ingest test entity instance to Elasticsearch index")
    void testRepoPopulator() throws Exception {
        var ingestor = repoPopulator.init().selectBatchIngestor("test");
        ingestor.add("{\"eclass\": \"TestEntity\", \"id\": \"1\"}");
        repoPopulator.flushIngestors();
        var service = repoPopulator.getService("test");
        assertEquals(testService, service);
        var entity = service.getRepo().findById("1");
        assertNotNull(entity);
    }

    @Test
    @DisplayName("repo populator should ingest test entity instance from .tar.gz file")
    void testIngestTarArchive() throws Exception {
        repoPopulator.init().ingestTarFile(
            List.of("src/test/resources/test.tar.gz")
        );
        var entity = testService.getRepo().findById("1");
        assertNotNull(entity);
    }

}