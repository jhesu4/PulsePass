package com.pulse.pass;

import org.springframework.boot.SpringApplication;

public class TestPulsePassApplication {

	public static void main(String[] args) {
		SpringApplication.from(PulsePassApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
