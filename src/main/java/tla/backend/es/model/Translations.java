package tla.backend.es.model;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Translations {
	
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html
	
	@Field(type = FieldType.Text, analyzer = "german")
	private List<String> de;

	@Field(type = FieldType.Text, analyzer = "english")
	private List<String> en;

	@Field(type = FieldType.Text, analyzer = "french")
	private List<String> fr;
	
	@Field(type = FieldType.Text, analyzer = "arabic")
	private List<String> ar;
	
	@Field(type = FieldType.Text, analyzer = "italian")
	private List<String> it;

}