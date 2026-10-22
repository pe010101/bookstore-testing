package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DatabaseUsersTest {
    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UserDetailsService detailsService;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private DemoUserInitializer initializer;

    @Test
    void demoUsersContainBcryptHashesAndSeparateRoles() {
        AppUser user = users.findByUsername("user").orElseThrow();
        AppUser admin = users.findByUsername("admin").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo("User123!");
        assertThat(admin.getPassword()).startsWith("$2").isNotEqualTo("Admin123!");
        assertThat(encoder.matches("User123!", user.getPassword())).isTrue();
        assertThat(encoder.matches("Admin123!", admin.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(detailsService).isInstanceOf(DatabaseUserDetailsService.class)
                .isNotInstanceOf(InMemoryUserDetailsManager.class);
    }

    @Test
    void aNewDatabaseUserCanLogInThroughTheRealFormFilter() throws Exception {
        users.saveAndFlush(new AppUser("database-only", encoder.encode("Database123!"),
                "database@example.test", "USER"));
        mvc.perform(formLogin().user("database-only").password("Database123!"))
                .andExpect(authenticated().withUsername("database-only").withRoles("USER"))
                .andExpect(redirectedUrl("/booklist"));
    }

    @Test
    void absentDatabaseUserCannotLogIn() throws Exception {
        mvc.perform(formLogin().user("no-such-user").password("anything"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        assertThatThrownBy(() -> detailsService.loadUserByUsername("no-such-user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void usernameIsUniqueInTheDatabase() {
        users.saveAndFlush(new AppUser("unique-demo", encoder.encode("First123!"),
                "first@example.test", "USER"));
        assertThatThrownBy(() -> users.saveAndFlush(new AppUser("unique-demo",
                encoder.encode("Second123!"), "second@example.test", "USER")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void initializerIsIdempotentAndNeverRehashesAnExistingHash() throws Exception {
        long beforeCount = users.count();
        String beforeHash = users.findByUsername("user").orElseThrow().getPassword();
        CommandLineRunner runner = initializer.initialUsers(users, encoder);
        runner.run();
        runner.run();
        assertThat(users.count()).isEqualTo(beforeCount);
        assertThat(users.findByUsername("user").orElseThrow().getPassword()).isEqualTo(beforeHash);
    }

    @Test
    void authenticationRepositoryHasNoGeneratedRestEndpointOrRootLink() throws Exception {
        mvc.perform(get("/api").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.appUsers").doesNotExist())
                .andExpect(jsonPath("$._links.users").doesNotExist());
        mvc.perform(get("/api/appUsers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordIsNeverSerializedToJson() throws Exception {
        String json = mapper.writeValueAsString(users.findByUsername("user").orElseThrow());
        assertThat(json).doesNotContain("password", "$2", "User123!");
        assertThat(json).contains("\"username\":\"user\"");
    }
}
