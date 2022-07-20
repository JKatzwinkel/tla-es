package tla.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.LemmaEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.service.EntityService;
import tla.backend.service.LemmaService;
import tla.domain.dto.LemmaDto;


@RestController
@RequestMapping("/lemma")
public class LemmaController extends EntityController<LemmaEntity, LemmaDto> {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private LemmaService lemmaService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public EntityService<LemmaEntity, ?, LemmaDto> getService() {
        return lemmaService;
    }

    // @RequestMapping(method = RequestMethod.GET, value = "/frequencies")
    // public ResponseEntity<Map<String, Long>> getFrequencies() {
    //     Map<String, Long> freq = lemmaService.getMostFrequent(2000);
    //     return new ResponseEntity<Map<String, Long>>(
    //         freq,
    //         HttpStatus.OK
    //     );
    // }

    @RequestMapping(method = RequestMethod.GET, value = "/get")
    public ResponseEntity<Iterable<LemmaDto>> getLemmataById(@RequestParam List<String> ids) {
        List<LemmaDto> results = new ArrayList<>();
        repo.findAllById(ids).forEach(
            entity -> {
                results.add(
                    modelMapper.map(
                        entity,
                        LemmaDto.class
                    )
                );
            }
        );
        return new ResponseEntity<Iterable<LemmaDto>>(
            results,
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.GET, value = "/all")
    public Iterable<LemmaEntity> getAll() {
        return repo.findAll(Sort.by("sortKey"));
    }

}