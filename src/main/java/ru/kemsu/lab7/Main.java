package ru.kemsu.lab7;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class Main {

    // H2 в памяти. Для хранения в файле: "jdbc:h2:~/lab7db"
    private static final String URL = "jdbc:h2:mem:lab7";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // Любимая композиция для задания № 3 (можно заменить на свою).
    private static final String MY_SONG = "Гимн РОССИИ";

    // Данные о себе для задания № 7 (замените на свои).
    private static final VisitorDto ME = new VisitorDto(
            "Александр", "Карманов", "+7-900-000-00-00", true,
            List.of(
                    new BookDto("Clean Code", "Robert C. Martin", 2008, "9780132350884", "Prentice Hall"),
                    new BookDto("The Pragmatic Programmer", "Andrew Hunt, David Thomas", 2019,
                            "9780135957059", "Addison-Wesley")));

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            MusicRepository music = new MusicRepository(conn);
            LibraryRepository library = new LibraryRepository(conn);

            music.prepare();

            title("№ 1. Все композиции");
            music.printAll();

            title("№ 2. Композиции без букв m и t");
            music.printWithoutMAndT();

            title("№ 3. Добавление композиции");
            music.add(MY_SONG);
            music.printAll();

            title("№ 4. Загрузка books.json");
            library.createTables();
            List<VisitorDto> visitors = readVisitors();
            library.load(visitors);
            library.printCounts();
            // Повторная загрузка не должна создать дубликаты.
            library.load(visitors);
            library.printCounts();

            title("№ 5. Книги по году издания");
            library.printBooksOrderedByYear();

            title("№ 6. Книги до 2000 года");
            library.printBooksBefore2000();

            title("№ 7. Добавление себя и своих книг");
            library.load(List.of(ME));
            library.printVisitorWithBooks(ME);

            title("№ 8. Удаление таблиц");
            library.dropTables();
        } catch (SQLException | IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<VisitorDto> readVisitors() throws IOException {
        try (InputStream in = Main.class.getResourceAsStream("/books.json")) {
            if (in == null) {
                throw new IOException("Не найден ресурс books.json");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, new TypeToken<List<VisitorDto>>() { }.getType());
            }
        }
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
