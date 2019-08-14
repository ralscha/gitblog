package ch.rasc.gitblog;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.samskivert.mustache.Mustache;

@SpringBootApplication
@EnableScheduling
public class Application {

	public static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public Mustache.Compiler mustacheCompiler() {
		return Mustache.compiler();
	}
}
