package com.quantlab.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class QuantlabApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuantlabApiApplication.class, args);
	}

}
