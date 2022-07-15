package tla.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.ThsSearchQueryBuilder;
import tla.backend.es.repo.ThesaurusRepo;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;
import tla.backend.service.component.EntityRetrieval;
import tla.domain.command.SearchCommand;
import tla.domain.dto.ThsEntryDto;
import tla.domain.model.Passport;
import tla.domain.model.meta.Resolvable;

@Service
@ModelClass(value = ThsEntryEntity.class, path = "ths")
public class ThesaurusService extends UserFriendlyEntityService<ThsEntryEntity, UserFriendlyEntityRepo<ThsEntryEntity, String>, ThsEntryDto> {

    @Autowired
    private ThesaurusRepo thsRepo;

    @Override
    public UserFriendlyEntityRepo<ThsEntryEntity, String> getRepo() {
        return thsRepo;
    }

    /**
     * If a passport references thesaurus entries at the leaf node located by the
     * specified path, these thesaurus entries get retrieved from the index.
     *
     * @param passport
     * @param path
     * @return
     */
    public List<ThsEntryEntity> extractThsEntriesFromPassport(Passport passport, String path) {
        List<Passport> leafNodes = passport.extractProperty(path);
        return EntityRetrieval.BulkEntityResolver.of(
            leafNodes.stream().filter(
                node -> node.get() instanceof Resolvable
            ).map(
                node -> (Resolvable) node.get()
            ).toList()
        ).resolve().stream().map(
            term -> (ThsEntryEntity) term
        ).toList();
    }

    /**
     * Creates a bulk entity retriever with all thesaurus entries referenced from within a document's
     * passport in its retrieval queue.
     */
    public static EntityRetrieval.BulkEntityResolver extractThsEntriesFromPassport(Indexable document) {
        if (document instanceof TLAEntity) {
            return EntityRetrieval.BulkEntityResolver.of(
                ((TLAEntity) document).getPassport() != null ?
                ((TLAEntity) document).getPassport().extractObjectReferences() : null
            );
        }
        return new EntityRetrieval.BulkEntityResolver();
    }

    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<ThsEntryDto> command) {
        return ThsSearchQueryBuilder.class;
    }

}