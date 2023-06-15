package com.bpm.activiti.modeler;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

//HibernateJpaAutoConfiguration.class,
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
//@ComponentScan(value = "org.activiti")
@EntityScan(basePackages = {"com.bpm.example.modeler"})
public class ModelerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModelerApplication.class, args);
	}

}
