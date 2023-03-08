package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.constant.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jndi.JndiTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Objects;

@Configuration
@Profile(Profiles.JBOSS_ORACLE)
public class CoreDataSourceConfiguration {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(Environment env) throws NamingException {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(productDataSource(env));
        em.setPackagesToScan("it.pagopa.pm.gateway.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(Environment env) throws NamingException {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory(env).getObject());

        return transactionManager;
    }

    @Bean
    public DataSource productDataSource(Environment env) throws NamingException {
        return (DataSource) new JndiTemplate().lookup(Objects.requireNonNull(
                env.getProperty("pagopa.datasource.jndi.name")));
    }

}
