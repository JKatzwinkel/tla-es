package tla.backend.es.repo.custom;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.meta.Indexable;

public interface EntityRepo<T extends Indexable, ID> extends ElasticsearchRepository<T, ID> {}
