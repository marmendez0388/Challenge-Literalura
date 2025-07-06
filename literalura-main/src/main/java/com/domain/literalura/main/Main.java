package com.domain.literalura.main;

import com.domain.literalura.model.*;
import com.domain.literalura.repository.*;
import com.domain.literalura.service.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Main {
    private final Scanner keyboard = new Scanner(System.in);
    private final ApiConsulter apiConsulter = new ApiConsulter();
    private final DataConverter dataConverter = new DataConverter();
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public Main(BookRepository bookRepository, AuthorRepository authorRepository) { this.bookRepository = bookRepository; this.authorRepository = authorRepository; }

    public void start() {
        var option = -1;

        while (option != 0) {
            var menu = """
                    \n
                    ======================================
                                  LiterAlura
                    ======================================
                    \n
                    --- Select an option ---
                    
                    1 - Search book by title
                    2 - List registered books
                    3 - List registered authors
                    4 - List authors alive in a given year
                    5 - List books by language
                    6 - List top 10 downloaded books
                    7 - Show books database statistics
                                        
                    0 - Exit
                    """;

            System.out.println(menu);

            if (keyboard.hasNextInt()) {
                option = keyboard.nextInt();
                keyboard.nextLine();

                switch (option) {
                    case 1:
                        searchBookByTitle();
                        break;
                    case 2:
                        listRegisteredBooks();
                        break;
                    case 3:
                        listRegisteredAuthors();
                        break;
                    case 4:
                        ListAuthorsAliveInAGivenYear();
                        break;
                    case 5:
                        listBooksByLanguage();
                        break;
                    case 6:
                        listTop10();
                        break;
                    case 7:
                        showDbStatistics();
                        break;
                    case 0:
                        System.out.println("\nClosing the app...");
                        break;
                    default:
                        System.out.println("\nInvalid option");
                }

            } else {
                System.out.println("\nInvalid input");
                keyboard.next();
            }
        }
    }

    @Transactional
    private void searchBookByTitle() {
        String BASE_URL = "https://gutendex.com/books/?search=";
        System.out.println("\nEnter the name of the book you want to search:");
        var title = keyboard.nextLine();

        if (!title.isBlank() && !isANumber(title)) {

            var json = apiConsulter.obtainData(BASE_URL + title.replace(" ", "%20"));
            var data = dataConverter.obtainData(json, Data.class);
            Optional<BookData> searchBook = data.results()
                    .stream()
                    .filter(b -> b.title().toLowerCase().contains(title.toLowerCase()))
                    .findFirst();

            if (searchBook.isPresent()) {
                BookData bookData = searchBook.get();

                if (!verifiedBookExistence(bookData)) {
                    Book book = new Book(bookData);
                    AuthorData authorData = bookData.author().get(0);
                    Optional<Author> optionalAuthor = authorRepository.findByName(authorData.name());

                    if (optionalAuthor.isPresent()) {
                        Author existingAuthor = optionalAuthor.get();
                        book.setAuthor(existingAuthor);
                        existingAuthor.getBooks().add(book);
                        authorRepository.save(existingAuthor);
                    } else {
                        Author newAuthor = new Author(authorData);
                        book.setAuthor(newAuthor);
                        newAuthor.getBooks().add(book);
                        authorRepository.save(newAuthor);
                    }

                    bookRepository.save(book);

                } else {
                    System.out.println("\nBook already added in DB");
                }

            } else {
                System.out.println("\nBook not found");
            }

        } else {
            System.out.println("\nInvalid input");
        }

    }

    private void listRegisteredBooks() {
        List<Book> books = bookRepository.findAll();

        if(!books.isEmpty()) {
            System.out.println("\n----- Registered books -----");
            books.forEach(System.out::println);
        } else {
            System.out.println("\nNothing here, yet");
        }

    }

    private void listRegisteredAuthors() {
        List<Author> authors = authorRepository.findAll();

        if(!authors.isEmpty()) {
            System.out.println("\n----- Registered authors -----");
            authors.forEach(System.out::println);
        } else {
            System.out.println("\nNothing here, yet");
        }

    }

    private boolean verifiedBookExistence(BookData bookData) {
        Book book = new Book(bookData);
        return bookRepository.verifiedBDExistence(book.getTitle());
    }

    private void ListAuthorsAliveInAGivenYear() {
        System.out.println("\nEnter the year you want to consult: ");

        if (keyboard.hasNextInt()) {
            var year = keyboard.nextInt();
            List<Author> authors = authorRepository.findAuthorsAlive(year);

            if (!authors.isEmpty()) {
                System.out.println("\n----- Registered authors alive in " + year + " -----");
                authors.forEach(System.out::println);
            } else {
                System.out.println("\nNo results, enter another year");
            }

        } else {
            System.out.println("\nInvalid input");
            keyboard.next();
        }

    }

    private void listBooksByLanguage() {
        var option = -1;
        String language = "";

        System.out.println("\nSelect the language you want to consult");
        var languagesMenu = """
               \n
               1 - English
               2 - French
               3 - German
               4 - Portuguese
               5 - Spanish
               """;

        System.out.println(languagesMenu);

        if (keyboard.hasNextInt()) {
            option = keyboard.nextInt();

            switch (option) {
                case 1:
                    language = "en";
                    break;
                case 2:
                    language = "fr";
                    break;
                case 3:
                    language = "de";
                    break;
                case 4:
                    language = "pt";
                    break;
                case 5:
                    language = "es";
                    break;
                default:
                    System.out.println("\nInvalid option");
            }

            System.out.println("\nRegistered books:");
            List<Book> books = bookRepository.findBooksByLanguage(language);

            if (!books.isEmpty()) {
                books.forEach(System.out::println);
            } else {
                System.out.println("\nNo results, select another language");
            }

        } else {
            System.out.println("\nInvalid input");
            keyboard.next();
        }

    }

    private boolean isANumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void listTop10() {
        List<Book> books = bookRepository.findTop10();

        if (!books.isEmpty()) {
            System.out.println("\n----- Top 10 downloaded books -----");
            books.forEach(b -> System.out.println(b.getTitle()));
        } else {
            System.out.println("\nNothing here, yet");
        }

    }

    private void showDbStatistics() {
        List<Book> books = bookRepository.findAll();

        if (!books.isEmpty()) {
            IntSummaryStatistics sta = books.stream()
                    .filter(b -> b.getDownloads() > 0)
                    .collect(Collectors.summarizingInt(Book::getDownloads));

            System.out.println("\n----- Database statistics -----");
            System.out.println("Average downloads: " + sta.getAverage());
            System.out.println("Max downloads: " + sta.getMax());
            System.out.println("Min downloads: " + sta.getMin());
            System.out.println("Registered book/s: " + sta.getCount());
        } else {
            System.out.println("\nNothing here, yet");
        }

    }

}
