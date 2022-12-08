package com.prac.kurento;

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class KurentoApplication{

	@Bean
	public KurentoClient kurentoClient() {
		return KurentoClient.create();
	}


	public static void main(String[] args) {
		SpringApplication.run(KurentoApplication.class, args);
	}

}
