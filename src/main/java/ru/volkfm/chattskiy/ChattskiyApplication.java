package ru.volkfm.chattskiy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

@SpringBootApplication
@EnableReactiveCassandraRepositories
@ConfigurationPropertiesScan
public class ChattskiyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChattskiyApplication.class, args);
	}
}
