package tla.backend.es.model.parts;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_EMPTY)
public class EditorInfo {

    @Field(type = FieldType.Text)
    private String author;

    @Field(type = FieldType.Date, format = DateFormat.year_month_day)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private EditDate updated;

    @Field(type = FieldType.Date, format = DateFormat.year_month_day)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private EditDate created;

    @Field(type = FieldType.Keyword)
    private String type;

    @Nullable
    @Field(type = FieldType.Text)
    private List<String> contributors;

}