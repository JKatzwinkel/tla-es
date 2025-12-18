package tla.backend.es.model.parts;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Token {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String type = "word";

    @Field(type = FieldType.Text)
    @JsonAlias("name")
    private String label;

    @Field(type = FieldType.Object)
    private Lemmatization lemma;

    @Field(type = FieldType.Object)
    private Flexion flexion;

    @Field(type = FieldType.Object)
    private Glyphs glyphs;

    @Field(type = FieldType.Object)
    private Transcription transcription;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Keyword)
    private List<String> annoTypes;

    public static Token of(String glyphs, Transcription transcription) {
        Token result = new Token();
        result.glyphs = Glyphs.of(glyphs);
        result.transcription = transcription;
        return result;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flexion {

        /**
         * BTS glossing
         */
        @JsonAlias({"bGloss", "bgloss"})
        @Field(type = FieldType.Text)
        private String btsGloss;

        /**
         * Leipzig glossing
         */
        @JsonAlias({"lGloss", "lgloss"})
        @Field(type = FieldType.Text)
        private String lingGloss;

        /**
         * BTS flexcode
         */
        @Field(type = FieldType.Long)
        private Long numeric;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Lemmatization {
        /**
         * lemma part of speech
         */
        @JsonAlias({"POS"})
        @Field(name = "POS", type = FieldType.Object)
        private PartOfSpeech partOfSpeech;
        /**
         * lemma ID
         */
        @Field(type = FieldType.Keyword)
        private String id;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Glyphs {

        @Field(type = FieldType.Text, analyzer = "hieroglyph_analyzer", searchAnalyzer = "hieroglyph_analyzer")
        private String mdc;

        @Field(type = FieldType.Text)
        private String unicode;

        @Field(type = FieldType.Text)
        private String orig;

        @Field(type = FieldType.Long)
        private List<Long> order;

        public static Glyphs of(String mdc) {
            var glyphs = new Glyphs();
            glyphs.setMdc(mdc);
            return glyphs;
        }
    }

}
