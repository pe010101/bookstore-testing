package com.example.bookstore;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BookController {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public BookController(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/booklist";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/booklist")
    public String bookList(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "booklist";
    }

    @GetMapping("/add")
    public String addBookForm(Model model) {
        addFormModel(model, new Book());
        return "addbook";
    }

    @PostMapping("/save")
    public String addBook(@ModelAttribute BookForm form, @RequestParam Long categoryId) {
        return saveBook(new Book(), form, categoryId);
    }

    @PostMapping("/save/{id}")
    public String updateBook(@PathVariable Long id, @ModelAttribute BookForm form, @RequestParam Long categoryId) {
        return saveBook(requiredBook(id), form, categoryId);
    }

    private String saveBook(Book book, BookForm form, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());
        book.setPublicationYear(form.getPublicationYear());
        book.setCategory(category);
        bookRepository.save(book);
        return "redirect:/booklist";
    }

    @GetMapping("/edit/{id}")
    public String editBookForm(@PathVariable Long id, Model model) {
        addFormModel(model, requiredBook(id));
        return "addbook";
    }

    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookRepository.delete(requiredBook(id));
        return "redirect:/booklist";
    }

    private Book requiredBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private void addFormModel(Model model, Book book) {
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryRepository.findAll());
    }
}
