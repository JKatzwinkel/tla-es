package tla.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
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

    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        log.info("startup TLA backend app");
        SpringApplication.run(App.class, args);
    }

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return new JettyServletWebServerFactory(8090);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("process command line args:");
        log.info(String.join(", ", args.getOptionNames()));
        if (args.containsOption("data-file")) {
            repoPopulator.init().ingestTarFile(
                args.getOptionValues("data-file")
            );
        }
        if (args.containsOption("shutdown")) {
            shutdown();
        }
    }

    public void shutdown() {
        System.exit(
            SpringApplication.exit(
                applicationContext,
                () -> 0
            )
        );
    }
}
