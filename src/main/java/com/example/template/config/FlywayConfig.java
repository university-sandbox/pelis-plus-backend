package com.example.template.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    /**
     * Forces entityManagerFactory to depend on flyway bean,
     * ensuring migrations run before Hibernate validates the schema.
     */
    @Bean
    public static BeanDefinitionRegistryPostProcessor flywayDependsOnPostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                for (String name : registry.getBeanDefinitionNames()) {
                    BeanDefinition bd = registry.getBeanDefinition(name);
                    if ("entityManagerFactory".equals(name) || "jpaSharedEM_entityManagerFactory".equals(name)) {
                        String[] existing = bd.getDependsOn();
                        String[] updated = existing == null ? new String[]{"flyway"} : append(existing, "flyway");
                        bd.setDependsOn(updated);
                    }
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {}

            private String[] append(String[] arr, String value) {
                String[] result = new String[arr.length + 1];
                System.arraycopy(arr, 0, result, 0, arr.length);
                result[arr.length] = value;
                return result;
            }
        };
    }
}
