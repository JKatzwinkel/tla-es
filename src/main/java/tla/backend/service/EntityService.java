package tla.backend.service;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.ESQueryResult;
import tla.backend.service.component.EntityRetrieval;
import tla.backend.service.search.AutoCompleteSupport;
import tla.backend.service.search.SearchService;
import tla.domain.command.SearchCommand;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.dto.meta.DocumentDto;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;
import tla.error.ObjectNotFoundException;

/**
 * Implementing subclasses must be annotated with {@link ModelClass} and be instantiated
 * using the no-args default constructor.
 * They should also be annotated with {@link Service} for component scanning / dependency injection.
 *
 * For handling of an incoming search command DTO, use {@link #runSearchCommand(SearchCommand, Pageable)}.
 *
 * @see SearchService
 */
@Slf4j
public abstract class EntityService<T extends Indexable, R extends ElasticsearchRepository<T, String>, D extends AbstractDto> {

    @Autowired
    protected SearchService searchService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ModelMapper modelMapper;

    private Class<T> modelClass = null;
    private Class<D> dtoClass = null;
    protected static Map<Class<? extends Indexable>, EntityService<? extends Indexable, ? extends ElasticsearchRepository<?,?>, ? extends AbstractDto>> modelClassServices = new HashMap<>();
    // TODO: either remove or actually utilize:
    protected static Map<Class<? extends Indexable>, AbstractDto> modelClassDtos = new HashMap<>();

    /**
     * Default constructor registering services under the eclass specified via a {@link BTSeClass}
     * annotation.
     */
    @SuppressWarnings("unchecked")
    protected EntityService() {
        for (Annotation a : this.getClass().getAnnotationsByType(ModelClass.class)) {
            this.modelClass = (Class<T>) ((ModelClass) a).value();
            modelClassServices.put(
                this.modelClass,
                this
            );
            if (AbstractBTSBaseClass.class.isAssignableFrom(this.modelClass)) {
                try {
                    ModelConfig.registerModelClass(
                        this.modelClass.asSubclass(AbstractBTSBaseClass.class)
                    );
                } catch (Exception e) {
                    log.error(
                        String.format("failed to register model class %s", this.modelClass),
                        e
                    );
                }
            }
        }
    }

    @PostConstruct
    void init() {
        createIndex();
    }

    public ModelMapper getModelMapper() {
        return this.modelMapper;
    }

    /**
     * Returns the domain class a service is taking care of (extracted from its
     * {@link ModelClass} annotation.
     */
    public Class<T> getModelClass() {
        return this.modelClass;
    }

    /**
     * Create the Elasticsearch index specified in the {@code @Document} annotation
     * of this service's model class, and configure its settings and mappings according
     * to {@code @Setting} and {@code @Mapping} annotations of the model class, and
     * spring data elasticsearch object mapping annotations of the model class's attributes.
     */
    public boolean createIndex() {
        IndexOperations index = elasticsearchOperations.indexOps(modelClass);
        var name = index.getIndexCoordinates().getIndexName();
        if (index.exists()) {
            log.info("index {} already exists.", name);
            return false;
        }
        try {
            index.createWithMapping();
            log.info("created index {} for model class {}.", name, modelClass.getSimpleName());
            return true;
        } catch (RestStatusException e) {
            log.warn("index {} already exists!", name);
        }
        return false;
    }


    /**
     * Returns the entity service registered for a given model class.
     *
     * Registration takes place at construction time of any service with a {@link ModelClass} annotation.
     * This is required for the entity retrieval service to work.
     */
    public static EntityService<? extends Indexable, ? extends ElasticsearchRepository<?, ?>, ? extends AbstractDto> getService(
        Class<? extends AbstractBTSBaseClass> modelClass
    ) {
        return modelClassServices.get(modelClass);
    }

    /**
     * Return the class specified via {@link TLADTO} annotation on top
     * of the model class a service is for.
     *
     * @see #getModelClass()
     */
    @SuppressWarnings("unchecked")
    public Class<D> getDtoClass() {
        if (this.dtoClass != null) {
            return this.dtoClass;
        } else {
            for (Annotation a : getModelClass().getAnnotationsByType(TLADTO.class)) {
                this.dtoClass = (Class<D>) ((TLADTO) a).value();
                return this.dtoClass;
            }
        }
        return null;
    }

    /**
     * Returns all model classes for which there are services registered.
     *
     * @see {@link ModelClass} annotation
     */
    public static Collection<Class<? extends Indexable>> getRegisteredModelClasses() {
        return modelClassServices.keySet();
    }

    /**
     * Subclasses must implement this method and return their respective entity repository
     * (which they presumably got autowired directly into them via spring dependency injection).
     *
     * @return The {@link ElasticsearchRepository} instance used by this service to manage the
     * Elasticsearch index where documents of the model class with which the service has been
     * typed are stored in.
     */
    public abstract R getRepo();

    /**
     * get a service's {@link AutoCompleteSupport} instance for performing
     * quick lookup searches against its entity type. Can and should be overridden by
     * subclasses so that autocompletion for specific entity types can be customized.
     */
    public AutoCompleteSupport getAutoCompleteSupport() {
        return AutoCompleteSupport.DEFAULT;
    }

    /**
     * Retrieve a single document from the Elasticsearch index managed by the service's
     * {@link ElasticsearchRepository}, or return null if none can be found for the given ID.
     *
     * @param id Document ID
     * @return Either an instance of the model class with which the service has been typed,
     * or null if no such document with the given ID could be found.
     */
    public T retrieve(String id) {
        Optional<T> result = getRepo().findById(id);
        return result.isPresent() ? result.get() : null;
    }

    /**
     * Retrieves the document specified by its ID from the Elasticsearch index, converts it into
     * an instance of its class's corresponding DTO type, wraps a {@link SingleDocumentWrapper}
     * around it, and then attempts to instantiate all documents that are referenced by it,
     * be it via <code>passport</code> or <code>relations</code>.
     *
     * @param id Identifier of the reqeusted document.
     * @return A {@link SingleDocumentWrapper} container object wrapping the DTO-turned document,
     * and inshallah all documents referenced by it.
     * @see {@link ModelConfig#toDTO}
     */
    public SingleDocumentWrapper<? extends AbstractDto> getDetails(String id) {
        T document = this.retrieve(id);
        final SingleDocumentWrapper<?> container;
        if (document == null) {
            return null;
        }
        container = new SingleDocumentWrapper<>(
            ModelConfig.toDTO(document)
        );
        var bulk = this.retrieveRelatedDocs(document);
        if (document instanceof TLAEntity entity) {
            bulk.addAll(
                entity.getPassport() != null ? entity.getPassport().extractObjectReferences() : null
            );
        }
        try {
            bulk.resolve().forEach(
                relatedObject -> {
                    container.addRelated(
                        (DocumentDto) ModelConfig.toDTO(relatedObject)
                    );
                }
            );
        } catch (Exception e) {
            log.error("something went wrong during conversion of related objects to DTO");
        }
        return container;
    }

    /**
     * If a document is an instance of {@link LinkedEntity}, go through its
     * {@link LinkedEntity#getRelations()} data structure and
     * try to look up all objects referenced in their respective Elasticsearch indices based on
     * their <code>eclass</code> value.
     *
     * @param document An instance of the model class this service is for
     * @return A list of the document instances referenced. Returns an empty list if the document
     * passed is not a {@link LinkedEntity} instance.
     */
    protected EntityRetrieval.BulkEntityResolver retrieveRelatedDocs(T document) {
        return (document instanceof LinkedEntity entity)
            ? EntityRetrieval.BulkEntityResolver.from(entity)
            : new EntityRetrieval.BulkEntityResolver();
    }

    /**
     * If a document is of type {@link TLAEntity}, then extract all document references
     * from its passport and try to retrieve them from the respective Elasticsearch indices
     * where those can be expected. Usually, the documents referenced from within a passport
     * should be thesaurus terms, and thus be located in the Elasticsearch index corresponding
     * to the eclass <code>"BTSThsEntry"</code>.
     */
    protected EntityRetrieval.BulkEntityResolver retrieveReferencedThesaurusEntries(Indexable document) {
        return ThesaurusService.extractThsEntriesFromPassport(document);
    }

    /**
     * Tries to find the ES document identified by eclass and ID.
     */
    public Indexable retrieveSingleBTSDoc(String eclass, String id) {
        Class<? extends AbstractBTSBaseClass> modelClass = ModelConfig.getModelClass(eclass);
        EntityService<?, ?, ?> service = EntityService.getService(modelClass);
        ElasticsearchRepository<? extends Indexable, String> repo = service.getRepo();
        Optional<? extends Indexable> result = repo.findById(id);
        return result.orElseThrow(
            () -> {
                log.error(
                    String.format(
                        "Could not retrieve %s document with ID %s",
                        eclass,
                        id
                    )
                );
                return new ObjectNotFoundException(id, eclass);
            }
        );
    }

    /**
     * Maps a bunch of model entities to DTOs.
     */
    public Collection<? extends AbstractDto> toDTO(Collection<T> entities) {
        return entities.stream().map(
            ModelConfig::toDTO
        ).toList();
    }

    /**
     * Extract DTO objects out of a list of searchresults of an entity type.
     */
    public List<D> hitsToDTO(SearchHits<?> hits, Class<D> dtoClass) {
        return hits.getSearchHits().stream().map(
            hit -> modelMapper.map(
                hit.getContent(),
                dtoClass
            )
        ).toList();
    }

    /**
     * Returns a list of matches for a simple multi field prefix query which
     * can be used for input field content assist and such. Can optionally filter
     * by object type.
     */
    public List<D> autoComplete(String type, String term) {
        if (term.length() < 3) {
            return Collections.emptyList();
        }
        return hitsToDTO(
            searchService.executeSearchQuery(
                this.getAutoCompleteSupport().autoCompleteQuery(term, type),
                this.getModelClass()
            ).getHits(),
            getDtoClass()
        );
    }

    /**
     * map incoming search command DTO to an elasticsearch query builder adapter.
     */
    public ESQueryBuilder getSearchCommandAdapter(SearchCommand<D> command) {
        return this.getModelMapper().map(
            command, this.getSearchCommandAdapterClass(command)
        );
    }

    /**
     * subclasses should implement this and return
     * the class to be used to convert command into native query builder/adapter.
     */
    public abstract Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<D> command);

    /**
     * TODO: doc
     */
    public Optional<SearchResultsWrapper<? extends D>> runSearchCommand(SearchCommand<D> command, Pageable page) {
        Assert.notNull(page, "pageable must be specified");
        log.info("page: {}", page);
        var queryAdapter = this.getSearchCommandAdapter(command);
        ESQueryResult<?> result = searchService.register(queryAdapter).run(page);
        try {
            result.getAggregations().remove(ESQueryResult.AGGS_ID_IDS);
            var wrapper = new SearchResultsWrapper<D>(
                hitsToDTO(result.getHits(), this.getDtoClass()),
                command,
                result.getPageInfo(),
                result.getAggregations()
            );
            retrieveRelatedDocs(result).resolve().forEach(
                relatedObject -> wrapper.addRelated(
                    (DocumentDto) ModelConfig.toDTO(relatedObject)
                )
            );
            return Optional.of(wrapper);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * initializes a bulk entity resolver with all object references (inside relations element) found in the
     * entities from a given search result.
     */
    protected EntityRetrieval.BulkEntityResolver retrieveRelatedDocs(ESQueryResult<?> searchResult) {
        return EntityRetrieval.BulkEntityResolver.from(
            searchResult.getHits().getSearchHits().stream().<LinkedEntity>map(
                hit -> (LinkedEntity) hit.getContent()
            )
        );
    }

}
