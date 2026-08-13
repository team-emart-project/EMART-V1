package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves the whole context still wires up: the API-key filter, the async
 * executor, the Thymeleaf engine and the JavaMailSender.
 *
 * dry-run keeps it honest — nothing here should ever reach a real mailbox.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "emart.email.dry-run=true",
        "emart.email.api-key="
})
class BackendEmailMicroserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
