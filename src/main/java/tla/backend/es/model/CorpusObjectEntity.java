package tla.backend.es.model;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.UserFriendlyEntity;
import tla.backend.es.model.parts.ObjectPath;
import tla.domain.dto.CorpusObjectDto;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSTCObject")
@TLADTO(CorpusObjectDto.class)
@Document(indexName = "object", createIndex = false)
public class CorpusObjectEntity extends UserFriendlyEntity {

    @Field(type = FieldType.Keyword, name = "hash")
    private String SUID;

    @Field(type = FieldType.Keyword)
    private String corpus;

    @Field(type = FieldType.Object)
    private ObjectPath[] paths;

}
