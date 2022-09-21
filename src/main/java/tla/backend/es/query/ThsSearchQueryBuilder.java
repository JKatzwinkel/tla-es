package tla.backend.es.query;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.service.ModelClass;

@Slf4j
@Getter
@ModelClass(ThsEntryEntity.class)
public class ThsSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder, ExpansionQueryBuilder {

    private boolean expansion;
    private List<String> rootIds;

    @Override
    public void setExpansion(boolean expansion) {
        log.info("ths query: add IDs aggregation");
        ExpansionQueryBuilder.super.setExpansion(expansion);
        this.expansion = expansion;
    }

}