package com.test.hackathon_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class HackathonBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HackathonBackendApplication.class, args);
	}

}
