package edu.bjfu.journal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class JournalSystemApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(JournalSystemApplication.class, args);
    }
}
