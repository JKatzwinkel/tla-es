package tla.backend.es.model.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.parts.ObjectReference;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.Relatable;
import tla.domain.model.meta.Resolvable;

@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(callSuper = true, exclude = {"relations"})
public abstract class LinkedEntity extends AbstractBTSBaseClass
        implements Relatable<LinkedEntity.Relations> {

    /**
     * A collection of references to other entity objects.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonDeserialize(contentAs = ObjectReference.class)
    public static class Relations extends ArrayList<Resolvable> {

        private static final long serialVersionUID = -3638905986166571667L;

        public Relations(Collection<Resolvable> refs) {
            this.addAll(refs);
        }

        public static Relations of(Resolvable... sources) {
            return new Relations(
                Arrays.asList(sources)
            );
        }

        @JsonCreator
        public static Relations from(
            Collection<tla.domain.model.ObjectReference> dtos
        ) {
            return new Relations(
                dtos.stream().map(
                    dto -> ObjectReference.builder().eclass(
                        dto.getEclass()
                    ).id(
                        dto.getId()
                    ).type(
                        dto.getType()
                    ).name(
                        dto.getName()
                    ).ranges(
                        dto.getRanges()
                    ).build()
                ).map(
                    Resolvable.class::cast
                ).toList()
            );
        }
    }

    /**
     * References to related objects grouped by relationship name (<code>partOf</code>,
     * <code>predecessor</code>, ...).
     */
    @Singular
    @Field(type = FieldType.Object)
    private Map<String, Relations> relations;

    /**
     * Default constructor initializing the relations map as an empty object.
     */
    public LinkedEntity() {
        this.relations = Collections.emptyMap();
    }

}
