package com.example.bookstore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DemoUserInitializer {
    @Bean
    CommandLineRunner initialUsers(AppUserRepository users, PasswordEncoder encoder) {
        return arguments -> {
            addIfAbsent(users, encoder, "user", "User123!", "user@example.test", "USER");
            addIfAbsent(users, encoder, "admin", "Admin123!", "admin@example.test", "ADMIN");
        };
    }

    private void addIfAbsent(AppUserRepository users, PasswordEncoder encoder,
            String username, String plainPassword, String email, String role) {
        if (users.findByUsername(username).isEmpty()) {
            users.save(new AppUser(username, encoder.encode(plainPassword), email, role));
        }
    }
}
