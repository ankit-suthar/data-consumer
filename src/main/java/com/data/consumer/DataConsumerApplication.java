package com.data.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "com.data.consumer.repository")
public class DataConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataConsumerApplication.class, args);
	}

}
