package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The thread pool every email is actually sent on.
 *
 * Sizing assumes the bottleneck is SMTP, not CPU: the threads spend their time
 * blocked on a socket, so a handful of them is plenty for a project of this size.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    public static final String EMAIL_EXECUTOR = "emailTaskExecutor";

    @Bean(name = EMAIL_EXECUTOR)
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");

        // If 100 messages are already waiting, the HTTP thread sends the next
        // one itself. That slows the caller down, which is the point: the
        // alternative policies either DROP the email or throw, and an order
        // confirmation that silently vanishes is the worst outcome here.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Let an in-flight send finish when the service is shutting down.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * An @Async method that returns void throws into nothing — without this the
     * stack trace would never be printed and a failed send would look like a
     * success. EmailService already catches its own failures; this is the net
     * underneath that net.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // The arguments are deliberately NOT logged: they are order payloads
        // carrying a real customer's name, email and address, and a stack
        // trace is no place for those. EmailDispatcher has already logged the
        // orderNo and a masked recipient by the time this runs.
        return (Throwable ex, Method method, Object... params) ->
                log.error("Unhandled failure in async method {}.{}",
                        method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }
}
