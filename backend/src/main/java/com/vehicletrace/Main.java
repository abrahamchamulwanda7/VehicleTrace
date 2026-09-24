package com.vehicletrace;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

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

                // FR2: Admin manages staff accounts for their garage (admin only)
                .post("/api/users", UserController::create)
                .get("/api/users", UserController::list)
                .delete("/api/users/{id}", UserController::remove)

                // FR3: Register a vehicle (login required)
                .post("/api/vehicles", VehicleController::register)

                // FR4 + FR5: Search by number plate and view repair history (login required)
                .get("/api/vehicles/{plate}", VehicleController::search)

                // FR6 + FR7 + FR8: Record a repair with parts and repeat-problem check (login required)
                .post("/api/repairs", RepairController::record)

                // Login (401) and permission (403) errors in the same JSON format
                .exception(HttpResponseException.class, (e, ctx) -> {
                    ctx.status(e.getStatus());
                    ctx.json(Map.of("error", e.getMessage()));
                })

                // Friendly error: request data is not valid JSON or has wrong types
                .exception(JsonProcessingException.class, (e, ctx) -> {
                    ctx.status(400);
                    ctx.json(Map.of("error", "Invalid request data. Please check the fields and try again."));
                })

                // Friendly error: anything else
                .exception(Exception.class, (e, ctx) -> {
                    if (e.getCause() instanceof JsonProcessingException) {
                        ctx.status(400);
                        ctx.json(Map.of("error", "Invalid request data. Please check the fields and try again."));
                        return;
                    }
                    e.printStackTrace();
                    ctx.status(500);
                    ctx.json(Map.of("error", "Something went wrong on the server. Please try again."));
                })

                .start(7070);
    }
}