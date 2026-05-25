package dev.taskflow;

import dev.taskflow.infrastructure.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableAsync
public class TaskFlowApplication {

    protected TaskFlowApplication() {}

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }
}
