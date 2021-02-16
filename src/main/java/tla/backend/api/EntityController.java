package tla.backend.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.service.EntityService;
import tla.domain.command.SearchCommand;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.error.ObjectNotFoundException;

/**
 * Generic TLA entity REST controller.
 *
 * Subclasses should be annotated with a path mapping, e.g. <code>@RequestMapping("/ths")</code>.
 * They also need to be annotated with {@link RestController}.
 */
@Slf4j
@RestController
public abstract class EntityController<T extends Indexable, D extends AbstractDto> {

    /**
     * Must return a presumably autowired entity service of appropriate type.
     */
    public abstract EntityService<T, ? extends ElasticsearchRepository<?, ?>, D> getService();

    /**
     * Returns a document wrapper containing a single document and all documents it references.
     */
    @RequestMapping(
        value = "/get/{id}",
        method = RequestMethod.GET,
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiResponse(responseCode = "200", description = "object found")
    public ResponseEntity<SingleDocumentWrapper<? extends AbstractDto>> get(
        @Parameter(description = "TLA object ID", required = true) @PathVariable String id
    ) throws ObjectNotFoundException {
        SingleDocumentWrapper<?> result = getService().getDetails(id);
        if (result != null) {
            return new ResponseEntity<SingleDocumentWrapper<?>>(
                result,
                HttpStatus.OK
            );
        }
        log.error("could not find entity {}", id);
        throw new ObjectNotFoundException(id, this.getService().getModelClass().getSimpleName());
    }


    @CrossOrigin
    @RequestMapping(
        value = "/complete",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<? extends AbstractDto>> getCompletions(@RequestParam(required = false) String type, @RequestParam String q) throws Exception {
        try {
            return new ResponseEntity<List<? extends AbstractDto>>(
                getService().autoComplete(type, q),
                HttpStatus.OK
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "search failed",
                e
            );
        }
    }

    /**
     * Counts documents in index.
     */
    @RequestMapping(
        value = "/count",
        method = RequestMethod.GET,
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Long> count() {
        return new ResponseEntity<Long>(
            getService().getRepo().count(),
            HttpStatus.OK
        );
    }

    @RequestMapping(
        value = "/search",
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiResponse(responseCode = "200", description = "search successful")
    public ResponseEntity<SearchResultsWrapper<?>> search(
        @RequestBody(required = true) SearchCommand<D> command,
        @Parameter(required = false) Pageable page
    ) {
        log.info("page: {}", tla.domain.util.IO.json(page));
        log.info("command: {}", tla.domain.util.IO.json(command));
        var result = this.getService().runSearchCommand(command, page);
        return new ResponseEntity<SearchResultsWrapper<?>>(
            result.orElseThrow(
                () -> new ObjectNotFoundException(getService().getModelClass().getName())
            ),
            HttpStatus.OK
        );
    }

}