package tla.backend.es.repo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.service.EntityService;
import tla.backend.service.ModelClass;
import tla.domain.model.meta.AbstractBTSBaseClass;

/**
 * Repository populator batch-indexing documents read from <code>*.tar.gz</code> file.
 * In order to be able to batch-ingest a document type, a {@link EntityService} implementation
 * must exist and be typed for the model class to which the document is to be mapped.
 * This entity service subclass must be annotated with a {@link ModelClass} annotation specifying
 * the model class <i>and</i> the directory within the TAR archive in which files of the
 * document type in question are located. The entity service subclass must also return
 * the correct {@link ElasticsearchRepository} instance necessary to batch-index a bunch of
 * documents using its {@link ElasticsearchRepository#saveAll(Iterable)} method.
 *
 * I.e. the service class should look like this:
 *
 * <pre>
 * @Service
 * @ModelClass(value = LemmaEntity.class, path = "lemma")
 * public class LemmaService extends QueryService<LemmaEntity> {
 *
 *     @Autowired
 *     private LemmaRepo repo;
 *
 * </pre>
 *
 * An instance of this class is being injected into the main application and automatically run
 * if the application is being executed using the <code>--data-file=</code> argument. If you want
 * it to shut down after populating the database, pass the <code>--shutdown</code> flag.
 *
 * <pre>
 * bootRun --args="--date-file=sample.tar.gz --shutdown"
 * </pre>
 *
 */
@Slf4j
public class RepoPopulator {

    /**
     * Batch indexer capable of deserializing JSON strings into instances of the
     * model class it typed for.
     */
    protected class RepoBatchIngestor<S extends Indexable> {

        final static int MAX_BATCH_SIZE = 750;

        private List<S> batch;
        @Getter private EntityService<S,?,?> service;
        private Class<S> modelClass;
        private String path;
        private int count;

        private ObjectMapper jsonMapper = new ObjectMapper();

        public RepoBatchIngestor(EntityService<S,?,?> service) {
            this.service = service;
            this.modelClass = service.getModelClass();
            this.path = getModelClassServicePath(service);
            this.batch = new ArrayList<>();
            this.count = 0;
            log.info("set up batch ingestor for model class {}", modelClass.getName());
        }

        public void add(S doc) {
            this.batch.add(doc);
            if (this.batch.size() >= MAX_BATCH_SIZE) {
                this.ingest();
            }
        }

        /**
         * Deserialize a domain model entity from a JSON string and either cache it for later, or batch-index it along all
         * other cached entities in case their number exceeds the threshold defined in {@link #MAX_BATCH_SIZE}.
         *
         * @see #ingest()
         */
        public void add(String json) {
            try {
                S doc = jsonMapper.readValue(
                    json,
                    this.modelClass
                );
                this.add(doc);
            } catch (IOException e) {
                log.warn(
                    String.format(
                        "ingestor could not instantiate %s class from %s",
                        this.modelClass.getName(),
                        json
                    ),
                    e
                );
            }
        }

        /**
         * Get the {@link EntityService} instance of which the {@link ModelClass} annotation specifies the
         * same model class as this batch indexer is typed for, and uses that service's {@link ElasticsearchRepository}
         * to batch-index all entities currently in the cache.
         */
        public void ingest() {
            try {
                (
                    (ElasticsearchRepository<S, String>) service.getRepo()
                ).saveAll(this.batch);
                this.count += this.batch.size();
                this.batch.clear();
            } catch (Exception e) {
                log.warn(
                    String.format(
                        "%s ingestor could not save %d docs (%s)! Trying again...",
                        this.modelClass.getSimpleName(),
                        this.batch.size(),
                        e.getClass().getSimpleName()
                    )
                );
            }
        }
    }

    /**
     * batch indexer registry
     */
    protected Map<String, RepoBatchIngestor<? extends Indexable>> repoIngestors = new HashMap<>();

    /**
     * currently active batch indexer
     */
    private RepoBatchIngestor<? extends Indexable> batchIngestor = null;

    /**
     * Return the <code>path</code> value of a entity service's {@link ModelClass} annotation,
     * which might be used to specify e.g. from which subdirectory data for this specific model
     * class can be imported.
     */
    private static String getModelClassServicePath(
        EntityService<? extends Indexable, ? extends ElasticsearchRepository<?, ?>, ?> service
    ) {
        for (Annotation a : service.getClass().getAnnotations()) {
            if (a instanceof ModelClass) {
                return ((ModelClass) a).path();
            }
        }
        return null;
    }

    /**
     * Find the entity service whose {@link ModelClass} annotation's <code>path</code> value equals
     * the given <code>modelPath</code> parameter.
     */
    public EntityService<?,?,?> getService(String modelPath) {
        return this.selectBatchIngestor(modelPath).getService();
    }

    /**
     * Registers {@link RepoBatchIngestor} instances for each model class for which an {@link EntityService}
     * subclass has been registered with the {@link ModelClass} annotation.
     * @return populator instance
     * @see #ingestTarFile(List)
     */
    public RepoPopulator init() {
        for (Class<? extends Indexable> modelClass : EntityService.getRegisteredModelClasses()) {
            var service = EntityService.getService(
                modelClass.asSubclass(AbstractBTSBaseClass.class)
            );
            if (AbstractBTSBaseClass.class.isAssignableFrom(modelClass)) {
                String modelPath = getModelClassServicePath(service);
                if (modelPath != null && !modelPath.isEmpty()) {
                    this.repoIngestors.put(
                        modelPath,
                        new RepoBatchIngestor<>(service)
                    );
                }
            }
        }
        return this;
    }

    /**
     * Indexes all documents inside a <code>*.tar.gz</code> file at the specified location.
     * @param filenames List of length 1
     * @throws IOException
     * @see {@link #init()}
     */
    public void ingestTarFile(List<String> filenames) throws Exception {
        log.info("process tar file {}", String.join(", ", filenames));
        if (filenames.size() == 1) {
            String filename = filenames.get(0);
            try (
                TarArchiveInputStream input = new TarArchiveInputStream(
                    new GzipCompressorInputStream(
                        new FileInputStream(filename)
                    )
                )
            ) {
                processTarArchive(input);
            } catch (FileNotFoundException e) {
                log.error(
                    String.format("file not found: %s", filename),
                    e
                );
                throw e;
            } catch (IOException e) {
                log.error(
                    String.format("error during processing tar archive %s", filename),
                    e
                );
            }
        }
        repoIngestors.clear();
    }

    /**
     * Tries to get a document type identifier out of an archived item's path,
     * which is the hindmost path segment for directories, and the second path segment from the end
     * for file entries.
     *
     * @return path segment thought to represent a doctype
     */
    private String extractDocTypeFromPath(TarArchiveEntry archiveEntry) {
        String[] segments = archiveEntry.getName().split("/");
        int segmentOffset = archiveEntry.isDirectory() ? 1 : 2;
        return segments[
            segments.length - segmentOffset
        ];
    }

    /**
     * switches to the appropriate batch indexer for a given path.
     *
     * @return might be null
     */
    protected RepoBatchIngestor<?> selectBatchIngestor(String modelPath) {
        if (this.batchIngestor == null || !this.batchIngestor.path.equals(modelPath)) {
            if (this.repoIngestors.containsKey(modelPath)) {
                this.batchIngestor = this.repoIngestors.get(modelPath);
                log.info(
                    "use {} model batch ingestor for directory {}",
                    this.batchIngestor.modelClass.getSimpleName(),
                    modelPath
                );
            } else {
                this.batchIngestor = null;
            }
        }
        return this.batchIngestor;
    }

    /**
     * Add single TLA entity deserialized from a JSON document to buffer of currently active
     * {@link RepoBatchIngestor} for later batch ingestion into Elasticsearch index.
     */
    protected void addToBatch(String json) {
        this.batchIngestor.add(json);
    }

    /**
     * Go through a TAR archive and uses the appropriate {@link ElasticsearchRepository> to index
     * JSON files.
     */
    private void processTarArchive(TarArchiveInputStream input) throws IOException {
        TarArchiveEntry archiveEntry;
        long filecount = 0;
        while ((archiveEntry = input.getNextTarEntry()) != null) {
            String typeId = this.extractDocTypeFromPath(archiveEntry);
            if (!archiveEntry.isDirectory()) {
                if (input.canReadEntryData(archiveEntry)) {
                    filecount++;
                    if (this.selectBatchIngestor(typeId) != null) {
                        this.addToBatch(
                            new String(input.readAllBytes())
                        );
                    }
                } else {
                    log.warn("archived file {} not readable", archiveEntry.getName());
                }
            }
        }
        log.info("JSON documents extracted from archive: {}", filecount);
        flushIngestors();
    }

    protected void flushIngestors() {
        this.batchIngestor = null;
        for (RepoBatchIngestor<? extends Indexable> batchIngestor : this.repoIngestors.values()) {
            batchIngestor.ingest();
            log.info(
                "ingested {} documents of type {}",
                batchIngestor.count,
                batchIngestor.modelClass.getSimpleName()
            );
        }
    }

}
