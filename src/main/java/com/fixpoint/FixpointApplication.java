package com.fixpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FixpointApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(FixpointApplication.class, args);
		} catch (Throwable ex) {
			com.fixpoint.exceptions.ApplicationErrorHandler.handle(ex);
			throw ex;
		}
	}

}