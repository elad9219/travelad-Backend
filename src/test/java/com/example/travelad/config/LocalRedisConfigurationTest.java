package com.example.travelad.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class LocalRedisConfigurationTest {

    @Test
    void localPropertiesCreateAConnectionFactoryForTheLocalContainer() throws Exception {
        Properties properties =
                PropertiesLoaderUtils.loadAllProperties("application-local.properties");

        String[] values = properties.stringPropertyNames().stream()
                .map(name -> name + "=" + properties.getProperty(name))
                .toArray(String[]::new);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
                .withPropertyValues(values)
                .run(context -> {
                    assertNull(context.getStartupFailure());

                    LettuceConnectionFactory factory =
                            context.getBean(LettuceConnectionFactory.class);

                    assertNotNull(factory.getStandaloneConfiguration());
                    assertEquals(
                            "127.0.0.1",
                            factory.getStandaloneConfiguration().getHostName());
                    assertEquals(
                            26379,
                            factory.getStandaloneConfiguration().getPort());
                    assertEquals(
                            0,
                            factory.getStandaloneConfiguration().getDatabase());
                });
    }
}