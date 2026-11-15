package ru.kemsu.lab7;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Задания № 4-8: посетители и книги. */
public class LibraryRepository {

    private final Connection conn;

    public LibraryRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Создаёт таблицы, если их ещё нет.
     * Книга уникальна по ISBN, посетитель - по набору (имя, фамилия, телефон):
     * в books.json встречаются разные люди с одинаковым телефоном, поэтому один телефон не годится.
     * Связь «посетитель - любимые книги» хранится в таблице visitor_books (многие ко многим).
     */
    public void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS visitors ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "surname VARCHAR(100) NOT NULL, "
                    + "phone VARCHAR(30) NOT NULL, "
                    + "subscribed BOOLEAN NOT NULL, "
                    + "CONSTRAINT uq_visitor UNIQUE (name, surname, phone))");
            stmt.execute("CREATE TABLE IF NOT EXISTS books ("
                    + "isbn VARCHAR(20) PRIMARY KEY, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "author VARCHAR(255) NOT NULL, "
                    + "publishing_year INT NOT NULL, "
                    + "publisher VARCHAR(255) NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS visitor_books ("
                    + "visitor_id INT NOT NULL, "
                    + "isbn VARCHAR(20) NOT NULL, "
                    + "PRIMARY KEY (visitor_id, isbn), "
                    + "FOREIGN KEY (visitor_id) REFERENCES visitors (id), "
                    + "FOREIGN KEY (isbn) REFERENCES books (isbn))");
        }
    }

    /** Задание № 4 и 7: добавляет посетителей и книги, дубликаты не создаются. */
    public void load(List<VisitorDto> visitors) throws SQLException {
        for (VisitorDto visitor : visitors) {
            int visitorId = findOrInsertVisitor(visitor);
            for (BookDto book : visitor.favoriteBooks()) {
                upsertBook(book);
                linkVisitorBook(visitorId, book.isbn());
            }
        }
    }

    private int findOrInsertVisitor(VisitorDto v) throws SQLException {
        String select = "SELECT id FROM visitors WHERE name = ? AND surname = ? AND phone = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, v.name());
            ps.setString(2, v.surname());
            ps.setString(3, v.phone());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        String insert = "INSERT INTO visitors (name, surname, phone, subscribed) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.name());
            ps.setString(2, v.surname());
            ps.setString(3, v.phone());
            ps.setBoolean(4, v.subscribed());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void upsertBook(BookDto b) throws SQLException {
        String sql = "MERGE INTO books (isbn, name, author, publishing_year, publisher) "
                + "KEY (isbn) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.isbn());
            ps.setString(2, b.name());
            ps.setString(3, b.author());
            ps.setInt(4, b.publishingYear());
            ps.setString(5, b.publisher());
            ps.executeUpdate();
        }
    }

    private void linkVisitorBook(int visitorId, String isbn) throws SQLException {
        String sql = "MERGE INTO visitor_books (visitor_id, isbn) KEY (visitor_id, isbn) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitorId);
            ps.setString(2, isbn);
            ps.executeUpdate();
        }
    }

    public void printCounts() throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT (SELECT COUNT(*) FROM visitors) AS v, (SELECT COUNT(*) FROM books) AS b")) {
            rs.next();
            System.out.println("Посетителей в базе: " + rs.getInt("v") + ", книг в базе: " + rs.getInt("b"));
        }
    }

    /** Задание № 5: книги, отсортированные по году издания. */
    public void printBooksOrderedByYear() throws SQLException {
        printBooks("SELECT isbn, name, author, publishing_year FROM books "
                + "ORDER BY publishing_year, name");
    }

    /** Задание № 6: книги, изданные до 2000 года. */
    public void printBooksBefore2000() throws SQLException {
        printBooks("SELECT isbn, name, author, publishing_year FROM books "
                + "WHERE publishing_year < 2000 ORDER BY publishing_year, name");
    }

    /** Задание № 7: читает из базы данные посетителя и его любимые книги. */
    public void printVisitorWithBooks(VisitorDto v) throws SQLException {
        String sql = "SELECT v.name AS vname, v.surname, v.phone, b.name AS bname, b.author, b.publishing_year "
                + "FROM visitors v "
                + "JOIN visitor_books vb ON vb.visitor_id = v.id "
                + "JOIN books b ON b.isbn = vb.isbn "
                + "WHERE v.name = ? AND v.surname = ? AND v.phone = ? "
                + "ORDER BY b.name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.name());
            ps.setString(2, v.surname());
            ps.setString(3, v.phone());
            try (ResultSet rs = ps.executeQuery()) {
                boolean header = false;
                while (rs.next()) {
                    if (!header) {
                        System.out.println("Посетитель: " + rs.getString("vname") + " "
                                + rs.getString("surname") + ", тел. " + rs.getString("phone"));
                        System.out.println("Любимые книги:");
                        header = true;
                    }
                    System.out.printf("  - %s, %s (%d)%n", rs.getString("bname"),
                            rs.getString("author"), rs.getInt("publishing_year"));
                }
            }
        }
    }

    /** Задание № 8: удаляет созданные таблицы (сначала связующую, затем остальные). */
    public void dropTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE visitor_books");
            stmt.execute("DROP TABLE visitors");
            stmt.execute("DROP TABLE books");
        }
        System.out.println("Таблицы visitor_books, visitors и books удалены.");
    }

    private void printBooks(String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("%d | %-42s | %-20s | %s%n",
                        rs.getInt("publishing_year"), rs.getString("name"),
                        rs.getString("author"), rs.getString("isbn"));
            }
        }
    }
}
