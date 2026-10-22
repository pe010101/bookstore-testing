package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityWorkflowTest {
    @Autowired private MockMvc mvc;
    @Autowired private BookRepository books;
    @Autowired private CategoryRepository categories;

    @Test
    void anonymousUsersCannotOpenAnyApplicationPageOrApi() throws Exception {
        for (String path : new String[]{"/", "/booklist", "/add", "/books", "/api/books"}) {
            mvc.perform(get(path).accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Test
    void loginAndStaticCssRemainPublicAndFormContainsCsrfToken() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/css/bootstrap.min.css")).andExpect(status().isOk());
    }

    @Test
    void actualUserPasswordAuthenticatesAndThePageShowsUsernameWithoutDelete() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("user").password("User123!"))
                .andExpect(authenticated().withUsername("user").withRoles("USER"))
                .andExpect(redirectedUrl("/booklist")).andReturn();
        mvc.perform(get("/booklist").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">user</strong>")))
                .andExpect(content().string(not(containsString(">Delete</button>"))))
                .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    @Test
    void actualAdminPasswordAuthenticatesAndThePageShowsDelete() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("admin").password("Admin123!"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN")).andReturn();
        mvc.perform(get("/booklist").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">admin</strong>")))
                .andExpect(content().string(containsString(">Delete</button>")));
    }

    @Test
    void wrongPasswordReturnsLoginError() throws Exception {
        mvc.perform(formLogin().user("user").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutWithCsrfInvalidatesTheAuthenticatedSession() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("user").password("User123!")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(post("/logout").session(session).accept("text/html").with(csrf()))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void userCannotDeleteAHtmlBookEvenWithAValidCsrfToken() throws Exception {
        Book book = newBook();
        mvc.perform(post("/delete/{id}", book.getId()).with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void adminCanDeleteAHtmlBookWithAValidCsrfToken() throws Exception {
        Book book = newBook();
        mvc.perform(post("/delete/{id}", book.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/booklist"));
        assertThat(books.existsById(book.getId())).isFalse();
    }

    @Test
    void adminCannotDeleteWithoutCsrfAndGetNeverDeletes() throws Exception {
        Book book = newBook();
        mvc.perform(post("/delete/{id}", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/delete/{id}", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void userCannotDeleteThroughDataRestOrAssociationRoutesEvenWithCsrf() throws Exception {
        Book book = newBook();
        mvc.perform(delete("/api/books/{id}", book.getId()).with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/books/{id}/category", book.getId())
                        .with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void adminCanDeleteThroughDataRestWithCsrf() throws Exception {
        Book book = newBook();
        mvc.perform(delete("/api/books/{id}", book.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(books.existsById(book.getId())).isFalse();
    }

    @Test
    void apiDeletionAlsoRequiresCsrfForAdmin() throws Exception {
        Book book = newBook();
        mvc.perform(delete("/api/books/{id}", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    private Book newBook() {
        Category category = categories.findAll().iterator().next();
        Book book = new Book(null, "Security test book", "Test author", "security-test", 2026);
        book.setCategory(category);
        return books.save(book);
    }
}
