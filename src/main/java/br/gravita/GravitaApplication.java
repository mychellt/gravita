package br.gravita;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GravitaApplication {

	public static void main(final String[] args) {
		SpringApplication.run(GravitaApplication.class, args);
	}

}
