package tla.backend.es.model.meta;

import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.parts.EditorInfo;
import tla.backend.es.model.parts.Token;
import tla.backend.es.model.parts.Translations;
import tla.backend.es.query.LemmaSearchQueryBuilder;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.backend.es.query.TokenSearchQueryBuilder;
import tla.backend.service.EntityService;
import tla.backend.service.component.EntityRetrieval;
import tla.domain.command.LemmaSearch;
import tla.domain.command.PassportSpec;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TextSearch;
import tla.domain.command.TranslationSpec;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.CorpusObjectDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.SentenceDto;
import tla.domain.dto.TextDto;
import tla.domain.dto.ThsEntryDto;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.SentenceToken;
import tla.domain.model.extern.AttestedTimespan;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * If a model class is to be added, it has to be annotated with {@link @BTSeClass} and {@link TLADTO}
 * and registered in here.
 */
@Slf4j
@Configuration
public class ModelConfig {

    public static class PassportSpecConverter extends AbstractConverter<PassportSpec, PassportSpec> {
        @Override
        protected PassportSpec convert(PassportSpec source) {
            return source;
        }
    }

    public static class TranslationSpecConverter extends AbstractConverter<TranslationSpec, TranslationSpec> {
        @Override
        protected TranslationSpec convert(TranslationSpec source) {
            return source;
        }
    }

    public static class TextWordCountConverter extends AbstractConverter<TextEntity.WordCount, TextDto.WordCount> {
        @Override
        protected TextDto.WordCount convert(TextEntity.WordCount source) {
            return new TextDto.WordCount(source.min(), source.max());
        }
    }

    /**
     * Container for configurations that can be attributed to an eClass specified
     * via {@link BTSeClass} annotation on top of an {@link TLAEntity} instance:
     *
     * <ul>
     * <li>The ES index into which model class instances get saved</li>
     * <li>The model class itself</li>
     * </ul>
     */
    @Getter
    @Builder
    public static class BTSeClassConfig {
        private String index;
        private Class<? extends AbstractBTSBaseClass> modelClass;
    }

    public static SimpleDateFormat DATE_FORMAT;
    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected static ModelMapper modelMapper;

    @Getter
    private static List<Class<? extends AbstractBTSBaseClass>> modelClasses = new LinkedList<>();

    @Getter
    private static Map<String, BTSeClassConfig> eclassConfigs = new HashMap<>();

    public ModelConfig() {
        initModelMapper();
    }

    /**
     * Register eClass configuration extracted from the following annotations on the given model class:
     *
     * <ul>
     * <li>{@link BTSeClass}</li>
     * <li>{@link org.springframework.data.elasticsearch.annotations.Document}</li>
     * </ul>
     *
     * @return map with the extracted eclass as its only key,
     *         or <code>null</code> if any config value could not be extracted
     */
    private static Map<String, BTSeClassConfig> mapModelClassConfigToEclass(Class<? extends AbstractBTSBaseClass> clazz) {
        String eclass = tla.domain.model.meta.Util.extractEclass(clazz);
        String index = null;
        for (Annotation annotation : clazz.getAnnotationsByType(Document.class)) {
            index = ((Document) annotation).indexName();
        }
        BTSeClassConfig config = BTSeClassConfig.builder()
            .index(index)
            .modelClass(clazz)
            .build();
        log.info(
            "register configuration for eClass {}: {}",
            eclass,
            config.toString()
        );
        if (List.of(eclass, index, clazz).stream().allMatch(
            value -> value != null
        )) {
            return Map.of(
                eclass,
                config
            );
        } else {
            log.error(
                "Could not register eclass {} with index {} and model class {}!",
                eclass,
                index,
                clazz
            );
            return null;
        }
    }

    /**
     * extract <code>eclass</code> from {@link BTSeClass} or {@link TLADTO} annotation of given model class
     */
    public static String getEclass(Class<? extends BaseEntity> modelClass) {
        return tla.domain.model.meta.Util.extractEclass(modelClass);
    }

    /**
     * Register a model class annotated with {@link BTSeClass} and <code>@Document(index="...")</code>
     * and the corresponding configuration extracted from these annotations.
     *
     * <p>Registration of a model
     * class is required in order to be able to look up TLA documents based on their ID and eClass, as it
     * allow to look up a model class's dedicated {@link EntityService} and hence the corresponding
     * Elasticsearch repository required for operations with entities of that model class.</p>
     *
     * @throws Exception if <code>@BTSeClass</code> or <code>@Document</code> are missing on given class
     * @see {@link EntityService#getService(Class)}
     * @see {@link EntityRetrieval.BulkEntityResolver#from(LinkedEntity)}
     */
    public static Map<String, BTSeClassConfig> registerModelClass(
        Class<? extends AbstractBTSBaseClass> modelClass
    ) throws Exception {
        Map<String, BTSeClassConfig> conf = mapModelClassConfigToEclass(modelClass);
        if (conf != null) {
            if (!modelClasses.contains(modelClass)) {
                modelClasses.add(modelClass);
            }
            eclassConfigs.putAll(conf);
            return conf;
        } else {
            throw new Exception(
                String.format(
                    "could not register model class %s: annotation or annotation parameter missing!",
                    modelClass.getName()
                )
            );
        }
    }

    /**
     * Look up the model class registered for a given eclass.
     */
    public static Class<? extends AbstractBTSBaseClass> getModelClass(String eclass) {
        try {
            return eclassConfigs.get(eclass).getModelClass();
        } catch (Exception e) {
            log.error(
                String.format(
                    "can't find entity model class corresponding to eclass %s. Known eclasses are: %s",
                    eclass,
                    eclassConfigs.keySet()
                )
            );
            throw e;
        }
    }

    /**
     * Tells whether the {@link ModelConfig} class has been instantiated (presumably by spring DI).
     */
    public static boolean isInitialized() {
        return getEclassConfigs() != null && modelMapper != null;
    }

    @Bean
    public ModelMapper modelMapper() {
        if (modelMapper == null) {
            initModelMapper();
        }
        return modelMapper;
    }

    protected static ModelMapper initModelMapper() {
        log.debug("initializing model mapper");
        modelMapper = new ModelMapper();
        Translations.ToMapConverter translationsToMapConverter = new Translations.ToMapConverter();
        modelMapper.createTypeMap(EditorInfo.class, tla.domain.model.EditorInfo.class)
            .addMapping(
                EditorInfo::getUpdated, tla.domain.model.EditorInfo::setDateOfLatestUpdate
            ).addMapping(
                EditorInfo::getCreated, tla.domain.model.EditorInfo::setCreationDate
            );
        modelMapper.createTypeMap(LemmaEntity.class, LemmaDto.class)
            .addMappings(
                m -> m.using(translationsToMapConverter).map(
                    LemmaEntity::getTranslations, LemmaDto::setTranslations
                )
            ).addMapping(
                LemmaEntity::getRevisionState, LemmaDto::setReviewState
            );
        modelMapper.createTypeMap(LemmaEntity.AttestedTimeSpan.class, AttestedTimespan.Period.class);
        modelMapper.createTypeMap(ThsEntryEntity.class, ThsEntryDto.class)
            .addMappings(
                m -> m.using(translationsToMapConverter).map(
                    ThsEntryEntity::getTranslations, ThsEntryDto::setTranslations
                )
            ).addMapping(
                ThsEntryEntity::getRevisionState, ThsEntryDto::setReviewState
            );
        modelMapper.createTypeMap(TextEntity.class, TextDto.class)
            .addMapping(
                TextEntity::getRevisionState, TextDto::setReviewState
            ).addMappings(
                m -> m.using(new TextWordCountConverter()).map(
                    TextEntity::getWordCount, TextDto::setWordCount
                )
            );
        modelMapper.createTypeMap(CorpusObjectEntity.class, CorpusObjectDto.class)
            .addMapping(
                CorpusObjectEntity::getRevisionState, CorpusObjectDto::setReviewState
            );
        modelMapper.createTypeMap(AnnotationEntity.class, AnnotationDto.class).addMapping(
            AnnotationEntity::getRevisionState, AnnotationDto::setReviewState
        );
        modelMapper.createTypeMap(SentenceEntity.class, SentenceDto.class).addMappings(
            m -> m.using(translationsToMapConverter).map(
                SentenceEntity::getTranslations, SentenceDto::setTranslations
            )
        );
        modelMapper.createTypeMap(Token.class, SentenceToken.class).addMappings(
            m -> m.using(translationsToMapConverter).map(
                Token::getTranslations, SentenceToken::setTranslations
            )
        );
        // patches for search-command-to-query-adapter mappings, which for the most part don't require excplicit mappings
        modelMapper.createTypeMap(PassportSpec.class, PassportSpec.class);
        modelMapper.createTypeMap(TextSearch.class, TextSearchQueryBuilder.class).addMappings(
            m -> m.using(
                new PassportSpecConverter()
            ).map(
                TextSearch::getPassport, TextSearchQueryBuilder::setPassport
            )
        );
        modelMapper.createTypeMap(SentenceSearch.class, SentenceSearchQueryBuilder.class).addMappings(
            m -> m.using(
                new PassportSpecConverter()
            ).map(
                SentenceSearch::getPassport, SentenceSearchQueryBuilder::setPassport
            )
        );
        modelMapper.createTypeMap(TranslationSpec.class, TranslationSpec.class);
        modelMapper.createTypeMap(LemmaSearch.class, LemmaSearchQueryBuilder.class).addMappings(
            m -> m.using(new TranslationSpecConverter()).map(
                LemmaSearch::getTranslation, LemmaSearchQueryBuilder::setTranslation
            )
        );
        modelMapper.getTypeMap(SentenceSearch.class, SentenceSearchQueryBuilder.class).addMappings(
            m -> m.using(new TranslationSpecConverter()).map(
                SentenceSearch::getTranslation, SentenceSearchQueryBuilder::setTranslation
            )
        );
        modelMapper.createTypeMap(SentenceSearch.TokenSpec.class, TokenSearchQueryBuilder.class).addMappings(
            m -> m.using(new TranslationSpecConverter()).map(
                SentenceSearch.TokenSpec::getTranslation, TokenSearchQueryBuilder::setTranslation
            )
        );
        return modelMapper;
    }

    /**
     * returns the ES index name specified for an entity class via the <code>indexName</code>
     * attribute of the {@link Document} annotation.
     */
    public static String getIndexName(Class<? extends Indexable> clazz) {
        for (Annotation annotation : clazz.getAnnotationsByType(Document.class)) {
            return ((Document) annotation).indexName();
        }
        throw new IllegalArgumentException(
            String.format(
                "Class %s has no index name specified",
                clazz.getName()
            )
        );
    }

    /**
     * Uses the modelmapper bean configured in {@link #modelMapper()} to convert a model class
     * instance to its corresponding DTO representation, which is specified via a {@link TLADTO}
     * annotation on top of that model class.
     *
     * @param entity An instance of an {@link Indexable} which has a {@link TLADTO} annotation.
     * @return An instance of the DTO class corresponding to the passed entity's class,
     * created using the application context's model mapper instance.
     */
    public static AbstractDto toDTO(Indexable entity) throws NullPointerException {
        if (entity != null) {
            Class<? extends AbstractDto> dtoClass = getModelClassDTO(entity.getClass());
            Object dto = modelMapper.map(
                entity,
                dtoClass
            );
            return dtoClass.cast(dto);
        }
        throw new NullPointerException("can't convert null object!");
    }

    /**
     * Extracts the corresponding DTO type of a model class from its {@link TLADTO} annotation.
     * Returns null and fails silently if no annotation is set.
     *
     * @param modelClass Any {@link Indexable} class which defines its respective DTO class with
     * a {@link TLADO} annotation.
     * @return A {@link AbstractBTSBaseClass} subclass.
     */
    public static Class<? extends AbstractDto> getModelClassDTO(Class<? extends Indexable> modelClass) {
        for (Annotation annotation : modelClass.getAnnotations()) {
            if (annotation instanceof TLADTO tladto) {
                return tladto.value();
            }
        }
        log.warn("class {} has not @TLADTO annotation", modelClass.getName());
        return null;
    }

}
