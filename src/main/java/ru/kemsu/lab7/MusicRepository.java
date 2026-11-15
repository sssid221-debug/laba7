package ru.kemsu.lab7;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Задания № 1-3: работа с таблицей study.music. */
public class MusicRepository {

    private final Connection conn;

    public MusicRepository(Connection conn) {
        this.conn = conn;
    }

    /** Создаёт схему study и выполняет скрипт music-create.sql из ресурсов. */
    public void prepare() throws SQLException, IOException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS study");
        }
        String script;
        try (InputStream in = MusicRepository.class.getResourceAsStream("/music-create.sql")) {
            if (in == null) {
                throw new IOException("Не найден ресурс music-create.sql");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Скрипт состоит из нескольких команд, разделённых точкой с запятой.
        for (String sql : script.split(";")) {
            if (sql.isBlank()) {
                continue;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    /** Задание № 1: все композиции. */
    public void printAll() throws SQLException {
        String sql = "SELECT id, name FROM study.music ORDER BY id";
        printQuery(sql);
    }

    /** Задание № 2: названия, в которых нет ни буквы m, ни буквы t (без учёта регистра). */
    public void printWithoutMAndT() throws SQLException {
        String sql = "SELECT id, name FROM study.music "
                + "WHERE LOWER(name) NOT LIKE '%m%' AND LOWER(name) NOT LIKE '%t%' "
                + "ORDER BY id";
        printQuery(sql);
    }

    /** Задание № 3: добавление композиции. Новый id = максимальный + 1. */
    public void add(String title) throws SQLException {
        String sql = "INSERT INTO study.music (id, name) "
                + "VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM study.music), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            int rows = ps.executeUpdate();
            System.out.println("Добавлено композиций: " + rows + " (" + title + ")");
        }
    }

    private void printQuery(String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("%2d | %s%n", rs.getInt("id"), rs.getString("name"));
            }
        }
    }
}
