package com.vehicletrace;

import io.javalin.Javalin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create()
                .get("/", ctx -> ctx.result("VehicleTrace backend is running"))

                .get("/api/db-test", ctx -> {
                    try (Connection conn = Database.getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
                        rs.next();
                        ctx.result("Connected to database: " + rs.getString(1));
                    }
                })

                // FR1: Garage registration
                .post("/api/garages/register", GarageController::register)

                .start(7070);
    }
}