package tla.backend.es.model.parts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transcription {

    @Field(type = FieldType.Text, analyzer = "transcription_analyzer", searchAnalyzer = "transcription_analyzer")
    private String unicode;

    @Field(type = FieldType.Text, analyzer = "transcription_analyzer", searchAnalyzer = "transcription_analyzer")
    private String mdc;

}
