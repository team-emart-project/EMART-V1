package com.example.demo;

import com.example.demo.config.EmailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * e-MART notification service.
 *
 * ONE JOB: turn an order payload into an email and send it. It owns no data,
 * talks to no database, and knows nothing about carts, pricing or payments —
 * whichever backend is running (Java or .NET) posts the finished order to
 * /api/send-order-email and this service does the rest.
 *
 * @EnableAsync is what makes that endpoint return in milliseconds: the HTTP
 * thread validates the payload and hands the send to a background executor, so
 * a slow or unreachable SMTP server can never stall a customer's checkout.
 */
@SpringBootApplication
@EnableConfigurationProperties(EmailProperties.class)
@EnableAsync
public class BackendEmailMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendEmailMicroserviceApplication.class, args);
	}

}
