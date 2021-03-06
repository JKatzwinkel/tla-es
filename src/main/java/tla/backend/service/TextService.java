package tla.backend.service;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.TextEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.backend.es.repo.TextRepo;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.TextDto;

@Service
@ModelClass(value = TextEntity.class, path = "text")
public class TextService extends UserFriendlyEntityService<TextEntity, UserFriendlyEntityRepo<TextEntity, String>, TextDto> {

    @Autowired
    private TextRepo textRepo;

    @Autowired
    private ThesaurusService thsService;

    @Override
    public UserFriendlyEntityRepo<TextEntity, String> getRepo() {
        return textRepo;
    }

    /** 
     * Returns first and last year of the time span a text has been attributed to. 
     *
    */
    public int[] getTimespan(String textId) {
        TextEntity text = textRepo.findById(textId).get();
        SortedSet<Integer> years = new TreeSet<>();
        thsService.extractThsEntriesFromPassport(
            text.getPassport(),
            "date.date.date"
        ).stream().forEach(
            term -> {
                years.addAll(
                    term.extractTimespan()
                );
            }
        );
        return new int[] {
            years.first(),
            years.last()
        };
    }

    @Override
    public ESQueryBuilder getSearchCommandAdapter(SearchCommand<TextDto> command) {
        return this.getModelMapper().map(command, TextSearchQueryBuilder.class);
    }

}