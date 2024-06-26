package tla.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.meta.Recursable;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.ESQueryResult;
import tla.backend.es.query.LemmaSearchQueryBuilder;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.query.TextsContainingLemmaOccurrenceQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.service.component.AttestationTreeBuilder;
import tla.backend.service.component.EntityRetrieval;
import tla.backend.service.search.AutoCompleteSupport;
import tla.backend.service.search.SearchService;
import tla.domain.command.SearchCommand;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.Language;
import tla.domain.model.ObjectReference;
import tla.domain.model.extern.AttestedTimespan;
import tla.domain.model.extern.AttestedTimespan.AttestationStats;

@Service
@ModelClass(value = LemmaEntity.class, path = "lemma")
public class LemmaService extends EntityService<LemmaEntity, ElasticsearchRepository<LemmaEntity, String>, LemmaDto> {

    static final String THS_ENTITY_ECLASS = ThsEntryEntity.getTypesEclass(ThsEntryEntity.class);

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private SearchService searchService;

    private AutoCompleteSupport autoComplete;

    @Override
    public ElasticsearchRepository<LemmaEntity, String> getRepo() {
        return repo;
    }

    /**
     * Extends superclass implementation {@link EntityService#getDetails(String)} in
     * that lemma attestations are computed from occurrences and put into the
     * wrapped lemma DTO.
     *
     * @see {@link #computeAttestedTimespans(String)}
     */
    @Override
    public SingleDocumentWrapper<? extends AbstractDto> getDetails(String id) {
        LemmaEntity lemma = retrieve(id);
        if (lemma == null) {
            return null;
        }
        SingleDocumentWrapper<?> wrapper = super.getDetails(id);
        ((LemmaDto) wrapper.getDoc()).setAttestations(
            this.computeAttestedTimespans(id)
        );
        return wrapper;
    }

    /**
     * collects all thesaurus terms representing a time period and being referenced
     * in texts containing the specified lemma, and counts the number of texts and
     * total occurrences for each one.
     */
    public List<AttestedTimespan> computeAttestedTimespans(String lemmaId) {
        ESQueryResult<?> textSearchResult = searchService.register(
            new TextsContainingLemmaOccurrenceQueryBuilder(lemmaId)
        ).run(SearchService.UNPAGED);
        Map<String, Long> textCounts = textSearchResult.getAggregation(
            SentenceSearchQueryBuilder.AGG_ID_TEXT_IDS
        );
        var sentenceCount = textCounts.values().stream().collect(
            Collectors.summingInt(Long::intValue)
        );
        Map<String, Long> textCountPerDate = textSearchResult.getAggregation(
            TextSearchQueryBuilder.AGG_ID_DATE
        );
        var dates = EntityRetrieval.BulkEntityResolver.of(
            textCountPerDate.keySet().stream().map(
                id -> ObjectReference.builder().id(id).eclass(THS_ENTITY_ECLASS).build()
            )
        ).stream();
        var attestations = AttestationTreeBuilder.of(
            dates.map(entity -> (Recursable) entity)
        ).counts(textCountPerDate).build();
        if (!attestations.isEmpty()) {
            attestations.get(0).setAttestations(
                AttestationStats.builder().sentences(sentenceCount).build()
            );
            attestations.get(0).setPeriod(
                AttestedTimespan.Period.builder().begin(0).end(0).build()
            );
        }
        return attestations;
    }

    // public Map<String, Long> getMostFrequent(int limit) {
    //     SearchResponse response = this.searchService.query(SentenceEntity.class, matchAllQuery(),
    //             AggregationBuilders.nested("aggs", "tokens").subAggregation(AggregationBuilders.terms("lemmata")
    //                     .field("tokens.lemma.id").order(BucketOrder.count(false)).size(limit)));
    //     Nested aggs = response.getAggregations().get("aggs");
    //     Terms terms = aggs.getAggregations().get("lemmata");
    //     return terms.getBuckets().stream()
    //             .collect(Collectors.toMap(Terms.Bucket::getKeyAsString, Terms.Bucket::getDocCount));
    // }

    @Override
    public AutoCompleteSupport getAutoCompleteSupport() {
        if (this.autoComplete == null) {
            this.autoComplete = new AutoCompleteSupport(
                Arrays.stream(Language.values()).collect(
                    Collectors.toMap(
                        lang -> String.format("translations.%s", lang),
                        lang -> .5f
                    )
                ),
                new String[]{"translations"}
            );
        }
        return this.autoComplete;
    }

    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<LemmaDto> command) {
        return LemmaSearchQueryBuilder.class;
    }

}