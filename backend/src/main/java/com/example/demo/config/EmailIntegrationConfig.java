package com.example.demo.config;

import com.example.demo.client.EmailServiceClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Everything this backend needs to talk to backend-email-microservice.
 *
 * @EnableAsync is the important line: without it the @Async on
 * OrderPlacedEmailListener is silently ignored and the HTTP call to the email
 * service happens on the checkout thread — which is exactly the coupling this
 * whole arrangement exists to avoid.
 */
@Configuration
@EnableConfigurationProperties(EmailServiceProperties.class)
@EnableAsync
public class EmailIntegrationConfig {

    /**
     * A RestClient dedicated to the email service: it carries the base URL and
     * the shared secret, so {@link EmailServiceClient} cannot forget either.
     *
     * TIMEOUTS ARE NOT OPTIONAL. The default is to wait forever, and a mail
     * service that has hung would then park an executor thread per order until
     * the pool is gone. Both values are short because the endpoint answers 202
     * without doing any real work.
     */
    @Bean
    public RestClient emailServiceRestClient(EmailServiceProperties properties) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        // RestClient.builder(), not the auto-configured RestClient.Builder bean.
        // Spring Boot 4 moved that auto-configuration into its own module, and
        // this project's starter set does not bring it in - injecting it fails
        // the whole context at startup. The static factory needs nothing but
        // spring-web, which is already here.
        RestClient.Builder configured = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory);

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            configured = configured.defaultHeader("X-API-Key", properties.getApiKey());
        }

        return configured.build();
    }

    /**
     * A SMALL, SEPARATE pool for notifications.
     *
     * Deliberately not the shared default executor: if email is slow, the
     * blocked threads should be these and nothing else. AbortPolicy over
     * CallerRuns for the same reason — when the queue is full, drop the
     * notification rather than let it run on the caller's thread, which after
     * @TransactionalEventListener is the thread that just finished a checkout.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
