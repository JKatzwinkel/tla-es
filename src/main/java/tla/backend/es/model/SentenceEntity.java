package tla.backend.es.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity;
import tla.backend.es.model.parts.Token;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Translations;
import tla.domain.dto.SentenceDto;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * Represents a single sentence from a text document.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BTSeClass("BTSSentence")
@TLADTO(SentenceDto.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "sentence", createIndex = false)
@Setting(settingPath = "/elasticsearch/settings/indices/sentence.json")
public class SentenceEntity extends LinkedEntity implements Indexable {

    @Id
    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Object)
    private Context context;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Object)
    private Transcription transcription;

    @Field(type = FieldType.Nested)
    private Collection<Token> tokens;

    @Field(type = FieldType.Integer)
    private int wordCount;

    /**
     * Tells you to which text document this sentence belongs and its position
     * within the text's content.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Context {
        /**
         * ID of the text document to which this sentence belongs.
         */
        @Field(type = FieldType.Keyword)
        @JsonAlias("text")
        private String textId;
        /**
         * text document type
         */
        @Field(type = FieldType.Keyword)
        private String textType;
        /**
         * Label of whichever "new line" marker token found in the preceding sentences
         * is closest to the beginning of this sentence.
         */
        @Field(type = FieldType.Text)
        private String line;
        /**
         * Label of whichever "new paragraph" marker token found in the preceding sentences
         * is closest to the beginning of this sentence.
         */
        @JsonAlias("para")
        @Field(type = FieldType.Text)
        private String paragraph;
        /**
         * This sentence's positon in the containing text's array of sentences, starting
         * with <code>0</code>.
         */
        @JsonAlias("pos")
        @Field(type = FieldType.Integer)
        private int position;
        /**
         * How many variants of a single sentences exist (due to ambivalences).
         */
        @Field(type = FieldType.Integer)
        private int variants;
    }

}
