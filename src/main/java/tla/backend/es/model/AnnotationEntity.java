package tla.backend.es.model;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import tla.backend.es.model.meta.BaseEntity;
import tla.domain.dto.AnnotationDto;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSAnnotation")
@TLADTO(AnnotationDto.class)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "annotation", createIndex = false)
public class AnnotationEntity extends BaseEntity {

    /**
     * Locator for lemma annotation text content within passports.
     */
    public static final String LEMMA_ANNOTATION_PASSPORT_PATH = "annotation.lemma";

    @Field(type = FieldType.Object)
    private Passport passport;

    @Getter(AccessLevel.NONE)
    @Field(type = FieldType.Text, analyzer = "german")
    private Collection<String> body;

    public void setTitle(String title) {
        this.setName(title);
    }

    /**
     * Text content copied from annotation {@link Passport} in case the annotation
     * belongs to a lemma entry.
     */
    public Collection<String> getBody() {
        if (this.body != null) {
            return this.body;
        } else {
            return this.extractBodyFromPassport();
        }
    }

    /**
     * Annotations referring to lemma entries have text content stored inside
     * their {@link Passport} metadata trees.
     * This method attempts to extract such text content from the passport.
     * If the annotation does not belong to a lemma entry, then an empty list
     * is being returned.
     *
     * @return {@link List} instance in all cases.
     */
    private Collection<String> extractBodyFromPassport() {
        if (this.getPassport() != null) {
            Collection<Passport> leafNodes = this.getPassport().extractProperty(
                LEMMA_ANNOTATION_PASSPORT_PATH
            );
            if (leafNodes != null) {
                return leafNodes.stream().filter(
                    node -> {
                        return !node.isEmpty() && !node.getLeafNodeValue().isBlank();
                    }
                ).map(
                    Passport::getLeafNodeValue
                ).map(
                    String::trim
                ).collect(
                    Collectors.toList()
                );
            }
        }
        return Collections.emptyList();
    }

}
