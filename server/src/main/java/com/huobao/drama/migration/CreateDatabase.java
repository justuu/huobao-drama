package com.huobao.drama.migration;

import java.sql.*;

/**
 * Quick utility to create the huobao_drama database on MySQL.
 * Run: mvn compile exec:java -Dexec.mainClass="com.huobao.drama.migration.CreateDatabase"
 *      -Dexec.args="jdbc:mysql://host:3306 user password"
 */
public class CreateDatabase {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: <mysql-base-url> <user> <password>");
            return;
        }
        String url = args[0] + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, args[1], args[2]);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS huobao_drama CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Database huobao_drama created (or already exists).");
        }
    }
}
