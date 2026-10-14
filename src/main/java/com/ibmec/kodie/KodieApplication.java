package com.ibmec.kodie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // backup automatico diario (BackupService)
public class KodieApplication {

	public static void main(String[] args) {
		SpringApplication.run(KodieApplication.class, args);
	}

}
