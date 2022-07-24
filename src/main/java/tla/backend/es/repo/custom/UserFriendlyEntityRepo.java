package tla.backend.es.repo.custom;

import java.util.Optional;

import org.springframework.data.repository.NoRepositoryBean;

import tla.backend.es.model.meta.UserFriendlyEntity;

@NoRepositoryBean
public interface UserFriendlyEntityRepo<T extends UserFriendlyEntity, ID> extends EntityRepo<T, ID> {

    public Optional<T> findBySUID(String suid);

}