package tla.backend.service.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.service.EntityService;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.Resolvable;

public class EntityRetrieval {

    public static class BulkEntityResolver {

        /**
         * object reference target IDs grouped by eclass
         */
        protected Map<String, Set<String>> refs;

        public BulkEntityResolver() {
            this.refs = new HashMap<>();
        }

        /**
         * creates a new bulk entity resolver instance and initialize it
         * with given collection of object reference items to be resolved to actual
         * entities.
         */
        public static BulkEntityResolver of(Collection<? extends Resolvable> references) {
            return new BulkEntityResolver().addAll(references);
        }

        /**
         * creates a new bulk entity resolver instance and initialize it
         * with given stream of object reference items to be resolved to actual
         * entities.
         */
        public static BulkEntityResolver of(Stream<? extends Resolvable> references) {
            return BulkEntityResolver.of(references.toList());
        }

        /**
         * Take all objectreferences in an entity's <code>relations</code> map and feeds them into
         * a new {@link BulkEntityResolver} instance.
         */
        public static BulkEntityResolver from(LinkedEntity entity) {
            var bulk = new BulkEntityResolver();
            if (entity.getRelations() != null) {
                entity.getRelations().entrySet().forEach(
                    e -> bulk.addAll(e.getValue())
                );
            }
            return bulk;
        }

        /**
         * creates a bulk entity resolver and initialize it with the object references
         * from all given entities' relations.
         */
        public static BulkEntityResolver from(Collection<LinkedEntity> entities) {
            return BulkEntityResolver.from(entities.stream());
        }

        /**
         * creates a bulk entity resolver and initialize it with all object references
         * found inside a stream of entities' relations.
         */
        public static BulkEntityResolver from(Stream<LinkedEntity> entities) {
            var bulk = new BulkEntityResolver();
            entities.forEach(
                entity -> {
                    entity.getRelations().entrySet().stream().forEach(
                        entry -> bulk.addAll(entry.getValue())
                    );
                }
            );
            return bulk;
        }

        /**
         * Add object references to bulk retrieval queue.
         */
        public BulkEntityResolver addAll(Collection<? extends Resolvable> references) {
            if (references != null) {
                references.forEach(
                    ref -> this.add(ref)
                );
            }
            return this;
        }

        /**
         * Merge another bulk retriever's object references queue into this bulk retriever.
         */
        public BulkEntityResolver merge(BulkEntityResolver bulkResolver) {
            bulkResolver.getQueue().entrySet().forEach(
                e -> e.getValue().forEach(
                    v -> this.add(
                        ObjectReference.builder().id(v).eclass(e.getKey()).build()
                    )
                )
            );
            return this;
        }

        /**
         * Get object reference queue, which is a map with <code>eClass</code> values as keys,
         * associated with sets of entity IDs.
         */
        public Map<String, Set<String>> getQueue() {
            return this.refs;
        }

        /**
         * Add a single object reference to bulk retrieval queue.
         */
        protected void add(Resolvable ref) {
            this.refs.merge(
                ref.getEclass(),
                new HashSet<>(List.of(ref.getId())),
                (cur, id) -> {
                    cur.addAll(id);
                    return cur;
                }
            );
        }

        /**
         * Retrieve referenced object from respective ES indices.
         */
        public Collection<Indexable> resolve() {
            return this.stream().toList();
        }

        /**
         * Resolve as stream, i.e. retrieve all queued objects from respective ES indices.
         *
         * @see #resolve()
         */
        public Stream<Indexable> stream() {
            return this.refs.entrySet().stream().flatMap(
                e -> this.resolve(e.getKey(), e.getValue())
            );
        }

        /**
         * Retrieve object references to entities of specified type from respective ES repository.
         */
        protected Stream<? extends Indexable> resolve(String eclass, Collection<String> ids) {
            EntityService<?,?,?> service = EntityService.getService(
                ModelConfig.getModelClass(eclass)
            );
            return StreamSupport.stream(
                service.getRepo().findAllById(ids).spliterator(),
                false
            );
        }

    }

}
