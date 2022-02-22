package it.pagopa.pm.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.pagopa.boot.repository.core",
        entityManagerFactoryRef = "productEntityManager",
        transactionManagerRef = "productTransactionManager"
)
public class CoreDataSourceConfiguration {

    @Autowired
    private Environment env;

    @Bean
    public DataSource productDataSource() throws NamingException {
        return (DataSource) new JndiTemplate().lookup(Objects.requireNonNull(
                env.getProperty("pagopa.datasource.jdni.name")));
    }

}
