package tla.backend.es.model;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tla.backend.es.model.meta.Recursable;
import tla.backend.es.model.meta.UserFriendlyEntity;
import tla.backend.es.model.parts.ObjectPath;
import tla.domain.dto.TextDto;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * Text and Subtext model
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@BTSeClass("BTSText")
@TLADTO(TextDto.class)
@Document(indexName = "text", createIndex = false)
public class TextEntity extends UserFriendlyEntity implements Recursable {

    @Field(type = FieldType.Search_As_You_Type, name = "hash")
    private String SUID;

    @Field(type = FieldType.Keyword)
    private String corpus;

    @Field(type = FieldType.Object)
    private ObjectPath[] paths;

    @Field(type = FieldType.Object)
    private WordCount wordCount;

    public record WordCount(
        @Field(type = FieldType.Integer) int min,
        @Field(type = FieldType.Integer) int max
    ) {
        public WordCount(int count) {
            this(count, count);
        }
    }

}
