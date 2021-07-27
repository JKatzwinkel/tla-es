package tla.backend.api;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import tla.backend.es.model.Metadata;
import tla.backend.service.MetadataService;

@RestController
@RequestMapping("/")
public class ApiController {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private BuildProperties buildProperties;

    @Autowired
    private MetadataService metadataService;

    /**
     * list all registered endpoints
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<String> listEndpoints() {
        return new ResponseEntity<>(
            String.join(
                "\n",
                handlerMapping.getHandlerMethods().keySet().stream().flatMap(
                    mapping -> mapping.getPatternValues().stream()
                ).sorted().collect(
                    Collectors.toList()
                )
            ),
            HttpStatus.OK
        );
    }

    /**
     * Returns version info.
     */
    @RequestMapping(value = "/version", method = RequestMethod.GET)
    public ResponseEntity<?> getVersionInfo() throws IOException {
        Metadata info = this.metadataService.getInfo();
        HttpStatus statusCode = HttpStatus.OK;
        if (info == null) {
            info = Metadata.EMPTY;
            statusCode = HttpStatus.ACCEPTED;
        }
        return new ResponseEntity<>(
            Map.of(
                "version", buildProperties.getVersion(),
                "DOI", info.getDOI(),
                "date", info.getDate(),
                "release", info.getId(),
                "modelVersion", info.getModelVersion(),
                "etlVersion", info.getEtlVersion(),
                "lingglossVersion", info.getLingglossVersion(),
                "esVersion", this.metadataService.getESVersionNumber()
            ),
            statusCode
        );
    }

}
