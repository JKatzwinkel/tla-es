package tla.backend.es.repo;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import lombok.extern.slf4j.Slf4j;
import tla.domain.model.Passport;

@Slf4j
@Configuration
@EnableElasticsearchRepositories
public class RepoConfig extends ElasticsearchConfiguration {

    @Autowired
    private Environment env;

    @Override
    public ClientConfiguration clientConfiguration() {
        var host = env.getProperty(
            "tla.es.host", "localhost"
        );
        var port = Integer.parseInt(
            env.getProperty(
                "tla.es.port", "9200"
            )
        );
        log.info("configure Elasticsearch client for connection to {}:{}", host, port);
        return ClientConfiguration.builder().connectedTo(
            InetSocketAddress.createUnresolved(host, port)
        ).build();
    }

    @Bean
    public ElasticsearchConverter elasticsearchConverter() {
        MappingElasticsearchConverter mappingElasticsearchConverter = new MappingElasticsearchConverter(
            new SimpleElasticsearchMappingContext()
        );
        mappingElasticsearchConverter.setConversions(
            elasticsearchCustomConversions()
        );
        mappingElasticsearchConverter.afterPropertiesSet();
        return mappingElasticsearchConverter;
    }

    @Bean
    public RepoPopulator repoPopulator() {
        return new RepoPopulator();
    }

    @Bean
    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
            List.of(new PassportToMap(), new MapToPassport())
        );
    }

    @WritingConverter
    @SuppressWarnings("unchecked")
    public static class PassportToMap implements Converter<Passport, Map<String, Object>> {
        private static ObjectMapper mapper = tla.domain.util.IO.getMapper();
        @Override
        public Map<String, Object> convert(Passport source) {
            try {
                Map<String, Object> res = mapper.readValue(
                    mapper.writeValueAsString(source),
                    Map.class
                );
                return res;
            } catch (Exception e) {
                log.warn(
                    String.format(
                        "passport to map conversion failed for passport %s",
                        source
                    ),
                    e
                );
            }
            return null;
        }
    }

    @ReadingConverter
    public static class MapToPassport implements Converter<Map<String, Object>, Passport> {
        @Override
        public Passport convert(Map<String, Object> source) {
            return Passport.of(source);
        }
    }
}
