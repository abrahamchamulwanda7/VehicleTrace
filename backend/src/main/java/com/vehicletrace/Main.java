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

                // FR2: Staff login
                .post("/api/auth/login", AuthController::login)
                .get("/api/auth/me", AuthController::me)
                .post("/api/auth/logout", AuthController::logout)

                .start(7070);
    }
}