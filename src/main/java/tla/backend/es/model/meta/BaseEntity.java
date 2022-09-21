package tla.backend.es.model.meta;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.parts.EditorInfo;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.TLADTO;

/**
 * Entity model base class. Represents an identifiable stand-alone BTS/TLA document with
 * a name, a type and subtype, editing information, and references to related documents.
 *
 * <blockquote>Document types which have a <code>passport</code> metadata tree, and which are supposed
 * to be identifiable as external resources (e.g. in the online database published by a dfferent
 * research project) can extend the specialized abstract subclass {@link TLAEntity}.</blockquote>
 *
 * <p>Because this class implements {@link Indexable}, subclasses are expected
 * to declare an Elasticsearch index they are to be stored in, and because all subclasses are
 * {@link AbstractBTSBaseClass} instances, they must specify the <code>eClass</code> of the
 * original Berlin Text System (BTS) model class they correspond with.
 * Both properties are being registered in {@link ModelConfig}.</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class BaseEntity extends LinkedEntity implements Indexable {

    @Id
    @NonNull
    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String subtype;

    @Field(type = FieldType.Keyword)
    private String revisionState;

    @Field(type = FieldType.Search_As_You_Type)
    private String name;

    /**
     * Information about what researcher authored this document, who contributed to it,
     * and the date of the latest change.
     */
    @Field(type = FieldType.Object)
    private EditorInfo editors;

    /**
     * Converts an instance to a DTO of the type specified via {@link TLADTO} annotation
     */
    public AbstractDto toDTO() {
        return ModelConfig.toDTO(this);
    }

    /**
     * Creates an {@link tla.domain.model.ObjectReference} (DTO model) object identifying this instance.
     */
    public tla.domain.model.ObjectReference toDTOReference() {
        return tla.domain.model.ObjectReference.builder()
            .id(this.getId())
            .eclass(this.getEclass())
            .type(this.getType())
            .name(this.getName())
            .build();
    }


}
