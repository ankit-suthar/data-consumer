package com.data.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "com.data.consumer.repository")
@EntityScan(basePackages = "com.common.models.model")
@EnableJpaRepositories(basePackages = "com.data.consumer.repository")
public class DataConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataConsumerApplication.class, args);
	}

}
