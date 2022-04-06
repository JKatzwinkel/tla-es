package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilders;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import tla.backend.es.query.dependencies.ThsRefExpansionDependency;
import tla.domain.command.PassportSpec;
import tla.domain.command.PassportSpec.PassportSpecValue;

@Slf4j
@Getter
public class PassportIncludingQueryBuilder extends ESQueryBuilder {

    private PassportSpec passport = new PassportSpec();

    /**
     * Register a new {@link ThsRefExpansionDependency} query dependency for thesaurus entry ID
     * query expansion if the passport property search specification's expansion flag is set,
     * or register an ES terms query filter matching a (potentially already expanded) list of
     * thesaurus entry IDs.
     */
    private void processThsReferences(String key, PassportSpec.ThsRefPassportValue values) {
        if (values.isExpand()) {
            this.dependsOn(
                ThsRefExpansionDependency.of(
                    this, key, values.getValues()
                )
            );
            this.getPassport().put(key, values);
        } else {
            this.filter(
                QueryBuilders.termsQuery(
                    String.format("passport.%s.id.keyword", key),
                    this.getPassport().put(key, values).getValues()
                )
            );
        }
    }

    /**
     * Register a conjunct ES match query if an individual passport property search specification
     * contains literal values, or consider it for query expansion otherwise (i.e. if it
     * specified thesaurus entries).
     *
     * @see #processThsReferences(String, tla.domain.command.PassportSpec.ThsRefPassportValue)
     */
    private void processPassportProperty(String key, PassportSpecValue value) {
        if (!value.getValues().isEmpty()) {
            if (value instanceof PassportSpec.ThsRefPassportValue) {
                processThsReferences(key, (PassportSpec.ThsRefPassportValue) value);
            } else {
                this.must(
                    QueryBuilders.matchQuery(
                        String.format("passport.%s", key),
                        String.join(" ", value.getValues())
                    )
                );
            }
        }
    }

    /**
     * Add passport search specifications to this query builder, possibly queuing additional
     * query dependencies for thesaurus reference query expansion.
     */
    public void setPassport(PassportSpec spec) {
        if (spec != null && !spec.isEmpty()) {
            log.info("passport specs: {}", tla.domain.util.IO.json(spec));
            spec.forEach(
                this::processPassportProperty
            );
        }
    }

}