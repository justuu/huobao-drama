package com.huobao.drama.migration;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * One-time SQLite → MySQL data migration.
 * Run: mvn exec:java -Dexec.mainClass="com.huobao.drama.migration.SqliteToMysqlMigration"
 *      -Dexec.args="path/to/huobao_drama.db jdbc:mysql://localhost:3306/huobao_drama root password"
 */
public class SqliteToMysqlMigration {

    // Tables in dependency order (parents before children)
    private static final List<String> TABLES = List.of(
            "dramas",
            "episodes",
            "characters",
            "scenes",
            "props",
            "episode_characters",
            "episode_scenes",
            "storyboards",
            "storyboard_characters",
            "ai_service_configs",
            "ai_service_providers",
            "ai_voices",
            "agent_configs",
            "image_generations",
            "video_generations",
            "video_merges",
            "assets"
    );

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: <sqlite-path> <mysql-url> <mysql-user> <mysql-password>");
            System.out.println("Example: ../data/huobao_drama.db jdbc:mysql://localhost:3306/huobao_drama root root");
            return;
        }

        String sqlitePath = args[0];
        String mysqlUrl = args[1];
        String mysqlUser = args[2];
        String mysqlPass = args[3];

        try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
             Connection mysql = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {

            mysql.setAutoCommit(false);

            for (String table : TABLES) {
                migrateTable(sqlite, mysql, table);
            }

            mysql.commit();
            System.out.println("Migration completed successfully!");

            // Reset AUTO_INCREMENT for all tables
            for (String table : TABLES) {
                resetAutoIncrement(mysql, table);
            }
        }
    }

    private static void migrateTable(Connection sqlite, Connection mysql, String table) throws SQLException {
        System.out.printf("Migrating table: %s ... ", table);

        try (Statement stmt = sqlite.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // Build INSERT statement
            StringBuilder cols = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) { cols.append(", "); placeholders.append(", "); }
                cols.append("`").append(meta.getColumnName(i)).append("`");
                placeholders.append("?");
            }

            String insertSql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", table, cols, placeholders);

            int count = 0;
            try (PreparedStatement ps = mysql.prepareStatement(insertSql)) {
                while (rs.next()) {
                    for (int i = 1; i <= colCount; i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof String && isDateColumn(meta.getColumnName(i))) {
                            String strVal = (String) value;
                            ps.setObject(i, parseDateTime(strVal));
                        } else {
                            ps.setObject(i, value);
                        }
                    }
                    ps.addBatch();
                    count++;
                    if (count % 500 == 0) ps.executeBatch();
                }
                if (count % 500 != 0) ps.executeBatch();
            }

            System.out.printf("%d rows%n", count);
        }
    }

    private static boolean isDateColumn(String name) {
        return name.endsWith("_at") || name.equals("created_at") || name.equals("updated_at")
                || name.equals("deleted_at") || name.equals("completed_at");
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(value, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        // Fallback: try as-is
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            System.err.printf("  WARNING: Could not parse date '%s', storing as null%n", value);
            return null;
        }
    }

    private static void resetAutoIncrement(Connection mysql, String table) throws SQLException {
        try (Statement stmt = mysql.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM `" + table + "`");
            if (rs.next()) {
                long maxId = rs.getLong(1);
                if (maxId > 0) {
                    stmt.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT = " + (maxId + 1));
                }
            }
        } catch (SQLException e) {
            // storyboard_characters has composite PK, no auto_increment — skip
        }
    }
}
