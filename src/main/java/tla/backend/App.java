package tla.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.repo.RepoPopulator;

@Slf4j
@Configuration
@ComponentScan
@EnableAutoConfiguration(exclude = { ElasticsearchDataAutoConfiguration.class })
public class App implements ApplicationRunner {

    @Autowired
    private RepoPopulator repoPopulator;

    public static void main(String[] args) {
        log.info("startup TLA backend app");
        SpringApplication.run(App.class, args);
    }

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory(8090);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("process command line args:");
        log.info(String.join(", ", args.getOptionNames()));
        if (args.containsOption("data-file")) {
            repoPopulator.ingestTarFile(
                args.getOptionValues("data-file")
            );
        }
    }
}