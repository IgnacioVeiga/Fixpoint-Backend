package com.fixpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FixpointApplication {

	private static final String DEFAULT_APP_TIMEZONE = "America/Argentina/Buenos_Aires";

	public static void main(String[] args) {
		try {
			initializeJvmTimeZone();
			SpringApplication.run(FixpointApplication.class, args);
		} catch (Throwable ex) {
			com.fixpoint.exceptions.ApplicationErrorHandler.handle(ex);
			throw ex;
		}
	}

	private static void initializeJvmTimeZone() {
		String configuredTimeZone = System.getenv("APP_TIMEZONE");
		if (configuredTimeZone == null || configuredTimeZone.isBlank()) {
			configuredTimeZone = DEFAULT_APP_TIMEZONE;
		}
		String normalizedTimeZone = configuredTimeZone.trim();
		System.setProperty("user.timezone", normalizedTimeZone);
		TimeZone.setDefault(TimeZone.getTimeZone(normalizedTimeZone));
	}

}
