package it.pagopa.pm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan({"it.pagopa.pm.gateway.entity"})
@ComponentScan(basePackages = "it.pagopa.pm.gateway")
@EnableJpaRepositories(basePackages = "it.pagopa.pm.gateway.repository")
public class Application extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
