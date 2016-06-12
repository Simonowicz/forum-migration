package org.szepietowski;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
public class ForumMigrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForumMigrationApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setCorePoolSize(4);
		pool.setMaxPoolSize(8);
		pool.setWaitForTasksToCompleteOnShutdown(true);
		return pool;
	}

	@Bean public ConversionService conversionService() {
		return new DefaultConversionService();
	}
}
