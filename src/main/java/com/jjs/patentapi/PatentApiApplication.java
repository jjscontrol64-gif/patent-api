package com.jjs.patentapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatentApiApplication {

	public static void main(String[] args) {
		loadLocalDotenv();
		SpringApplication.run(PatentApiApplication.class, args);
	}

	private static void loadLocalDotenv() {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			if (System.getenv(key) == null && System.getProperty(key) == null) {
				System.setProperty(key, entry.getValue());
			}
		});
	}
}
