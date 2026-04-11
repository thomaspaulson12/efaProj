package com.efa.wizzmoni.configuration.dbconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataSourcePgSqlConfig {

    @Bean(name = "efaDataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.pgsql.efa")
    public DataSource efaDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "efaJdbcTemplate")
    @Primary
    public JdbcTemplate efaJdbcTemplate(@Qualifier("efaDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}