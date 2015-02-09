package com.hp.example;

import com.hp.example.entities.Person;
import com.hp.example.repositories.PersonRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class) // make JUnit use Spring's test runner
// tell the Spring runner to use the Java Configuration to define the context. The configuration is defined in a static class below
@ContextConfiguration
public class PersonRepositoryTest {
    @Autowired
    private PersonRepository personRepository;  // this is the real repository bean created by spring data JPA

    @Test
    public void findByFirstNameLike() {
        // the in-memory database is pre-populated with data from the import.sql
        List<Person> persons = personRepository.findByFirstNameLike("Mo%");

        verifyPersonExistsInList(persons, "Moshe", "Cohen");
    }

    @Test
    public void findByLastNameLike() {
        List<Person> persons = personRepository.findByLastNameLike("%oh%");

        verifyPersonExistsInList(persons, "Moshe", "Cohen");
    }

    private void verifyPersonExistsInList(List<Person> persons, String firstName, String lastName) {
        Person p = new Person();
        p.setFirstName(firstName);
        p.setLastName(lastName);

        assertThat(firstName + " " + lastName + " is not in the list", p, isIn(persons));
    }

    @Configuration                                       // the Spring Java Configuration class
    @EnableJpaRepositories(
            basePackages = "com.hp.example.repositories")// enable Spring data JPA repositories using scanning
    @EnableTransactionManagement                         // enable transaction management - required for Spring data JPA
    public static class PersonRepositoryTestConfiguration {
        @Bean
        public DataSource dataSource() {
            // define the data source with Spring JDBC embedded database builder
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        }

        // this bean is named because the @EnableJpaRepository expects a bean with this name
        @Bean(name = "entityManagerFactory")
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
            // define the EMF with standard Spring local container EMF factory bean
            LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
            factoryBean.setDataSource(dataSource());
            factoryBean.setPackagesToScan("com.hp.example");

            JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter() {
                {
                    setShowSql(true);
                    setGenerateDdl(true);
                }
            };

            factoryBean.setJpaVendorAdapter(vendorAdapter);
            factoryBean.setJpaProperties(additionalProperties());  // notice the use of additional properties

            return factoryBean;
        }

        // the additional properties for JPA - specific for Hibernate
        private Properties additionalProperties() {
            Properties props = new Properties();

            props.setProperty("hibernate.format_sql", "true");
            props.setProperty("hibernate.cache.use_query_cache", "false");
            props.setProperty("hibernate.generate_statistics", "false");
            props.setProperty("hibernate.cache.use_second_level_cache", "false");
            props.setProperty("hibernate.jdbc.fetch_size", "100");
            props.setProperty("hibernate.jdbc.batch_size", "500");
            props.setProperty("hibernate.order_updates", "true");
            props.setProperty("hibernate.order_inserts", "true");
            props.setProperty("hibernate.default_batch_fetch_size", "20");
            props.setProperty("hibernate.hbm2ddl.auto", "create-drop");

            return props;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            // create a JPA transaction manager
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactoryBean().getObject());
            return transactionManager;
        }
    }
}
