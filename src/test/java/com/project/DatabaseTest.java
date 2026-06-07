package com.project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseTest {
    public static void main(String[] args) {
        // For this clean run, we'll verify direct connection parameters
        String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String password = "npg_Cy0XQEZk2KSG"; 

        System.out.println("Testing live connection using Java 26 compiler setup...");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            if (connection != null) {
                System.out.println("\n=========================================");
                System.out.println("SUCCESS: Connected to Neon PostgreSQL cloud!");
                System.out.println("=========================================\n");
            }
        } catch (SQLException e) {
            System.out.println("\nConnection run encountered an issue.");
            e.printStackTrace();
        }
    }
}