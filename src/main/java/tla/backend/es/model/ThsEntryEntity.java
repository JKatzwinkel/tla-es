package tla.backend.es.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Recursable;
import tla.backend.es.model.meta.UserFriendlyEntity;
import tla.backend.es.model.parts.ObjectPath;
import tla.backend.es.model.parts.Translations;
import tla.domain.dto.ThsEntryDto;
import tla.domain.model.Passport;
import tla.domain.model.extern.AttestedTimespan;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@Slf4j
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSThsEntry")
@TLADTO(ThsEntryDto.class)
@Document(indexName = "ths")
@EqualsAndHashCode(callSuper = true)
@Setting(settingPath = "/elasticsearch/settings/indices/ths.json")
public class ThsEntryEntity extends UserFriendlyEntity implements Recursable {

    private static ObjectMapper objectMapper = tla.domain.util.IO.getMapper();

    private static final String SYNONYMS_PASSPORT_PATH = "synonyms.synonym_group";
    private static final String SYNONYM_VALUE_PATH = "synonym";
    private static final String SYNONYM_LANG_PATH = "language";
    private static final String DATE_START_PASSPORT_PATH = "thesaurus_date.main_group.beginning";
    private static final String DATE_END_PASSPORT_PATH = "thesaurus_date.main_group.end";

    private static final List<String> TIMESPAN_DATES_PASSPORT_PATHS = List.of(
        DATE_START_PASSPORT_PATH,
        DATE_END_PASSPORT_PATH
    );

    @Field(type = FieldType.Keyword)
    @JsonAlias({"sortkey", "sort_key", "sort_string", "sortString"})
    private String sortKey;

    @Field(type = FieldType.Search_As_You_Type, name = "hash")
    private String SUID;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Object)
    private ObjectPath[] paths;

    /**
     * Returns translations of a thesaurus entry's label. If no explicit translations exist, this method
     * attempts to extract translations from the <code>synonym_group</code> field of the passport.
     */
    public Translations getTranslations() {
        if (this.translations != null) {
            return this.translations;
        } else {
            return this.extractTranslationsFromPassport();
        }
    }

    /**
     * Convert multilingual synonyms extracted from passport to {@link Translations} object.
     *
     * @return {@link Translations} instance or <code>null</code> if no synonyms are in passport
     */
    private Translations extractTranslationsFromPassport() {
        Translations res = null;
        if (this.getPassport() != null) {
            List<Passport> nodes = this.getPassport().extractProperty(
                SYNONYMS_PASSPORT_PATH
            );
            Map<String, List<String>> synonyms = new HashMap<>();
            nodes.stream().filter(
                n -> n.containsKey(SYNONYM_LANG_PATH) && n.containsKey(SYNONYM_VALUE_PATH)
            ).forEach(
                n -> {
                    List<String> translations = n.extractProperty(SYNONYM_VALUE_PATH).stream().map(
                        leafNode -> leafNode.getLeafNodeValue()
                    ).collect(
                        Collectors.toList()
                    );
                    n.extractProperty(SYNONYM_LANG_PATH).forEach(
                        langValueNode -> {
                            String lang = langValueNode.getLeafNodeValue();
                            if (synonyms.containsKey(lang)) {
                                synonyms.get(lang).addAll(translations);
                            } else {
                                synonyms.put(lang, new ArrayList<String>(translations));
                            }
                        }
                    );
                }
            );
            try {
                res = objectMapper.readValue(
                    objectMapper.writeValueAsString(synonyms),
                    Translations.class
                );
            } catch (Exception e) {
                log.error("something went wrong during synonum extraction", e);
            }
        }
        return res;
    }

    /**
     * Returns the timespan represented by a thesaurus entry in the form of a list
     * of size 2 containing first and last year.
     */
    public List<Integer> extractTimespan() {
        List<Integer> years = new ArrayList<>();
        TIMESPAN_DATES_PASSPORT_PATHS.stream().forEach(
            path -> {
                this.getPassport().extractProperty(path).stream().forEach(
                    node -> {
                        if (node.get() instanceof String) {
                            years.add(Integer.valueOf((String) node.get()));
                        }
                    }
                );
            }
        );
        Collections.sort(years);
        return years;
    }

    /**
    * creates a DTO object representing the timespan covered by a thesaurus term.
    */
    public AttestedTimespan.Period toAttestedPeriod() {
        List<Integer> years = this.extractTimespan();
        return AttestedTimespan.Period.builder()
            .begin(years.get(0))
            .end(years.get(1))
            .ref(this.toDTOReference())
            .build();
    }
}
