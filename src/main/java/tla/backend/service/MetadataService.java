package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.Metadata;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.repo.MetadataRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.meta.DocumentDto;

@Service
@ModelClass(value = Metadata.class, path = "meta")
public class MetadataService extends EntityService<Metadata, ElasticsearchRepository<Metadata, String>, DocumentDto> {

    @Autowired
    private MetadataRepo repo;

    private Metadata metadata;

    public Metadata getInfo() {
        if (metadata == null) {
            this.repo.findAll().forEach(
                m -> {
                    metadata = m;
                }
            );
        }
        return metadata;
    }

    @Override
    public ElasticsearchRepository<Metadata, String> getRepo() {
        return repo;
    }

    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<DocumentDto> command) {
        return null;
    }
}