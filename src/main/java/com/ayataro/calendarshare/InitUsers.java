package com.ayataro.calendarshare;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitUsers {

    @Bean
    CommandLineRunner init(UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            String email = "test@example.com";
            if (userRepo.findByEmail(email).isEmpty()) {
                User u = new User();
                u.setEmail(email);
                u.setName("Test");
                u.setPassword(encoder.encode("password"));
                userRepo.save(u);
            }
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}