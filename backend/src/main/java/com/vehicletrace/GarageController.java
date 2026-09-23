package com.vehicletrace;

import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class GarageController {

    private static final List<String> PROVINCES = List.of(
            "Central", "Copperbelt", "Eastern", "Luapula", "Lusaka",
            "Muchinga", "Northern", "North-Western", "Southern", "Western"
    );

    // POST /api/garages/register
    public static void register(Context ctx) throws Exception {
        RegisterGarageRequest req = ctx.bodyAsClass(RegisterGarageRequest.class);

        // 1. Check the input
        String error = validate(req);
        if (error != null) {
            ctx.status(400).json(Map.of("error", error));
            return;
        }
        String province = matchProvince(req.province());

        // 2. Hash the password (never store plain text)
        String passwordHash = BCrypt.hashpw(req.password(), BCrypt.gensalt());

        // 3. Save garage + admin user together (transaction)
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int garageId;
                String garageSql = "INSERT INTO garages (garage_name, province, address, phone) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(garageSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, req.garageName().trim());
                    ps.setString(2, province);
                    ps.setString(3, req.address().trim());
                    ps.setString(4, req.phone().trim());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        garageId = keys.getInt(1);
                    }
                }

                int userId;
                String userSql = "INSERT INTO users (garage_id, full_name, username, password_hash, role) VALUES (?, ?, ?, ?, 'ADMIN')";
                try (PreparedStatement ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, garageId);
                    ps.setString(2, req.fullName().trim());
                    ps.setString(3, req.username().trim());
                    ps.setString(4, passwordHash);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getInt(1);
                    }
                }

                conn.commit();

                ctx.status(201).json(Map.of(
                        "message", "Garage registered successfully",
                        "garageId", garageId,
                        "userId", userId,
                        "username", req.username().trim()
                ));

            } catch (SQLIntegrityConstraintViolationException e) {
                conn.rollback();
                String message = e.getMessage().contains("'username'")
                        ? "Username is already taken"
                        : "A garage with this name is already registered in this province";
                ctx.status(409).json(Map.of("error", message));
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Returns an error message, or null if everything is valid
    private static String validate(RegisterGarageRequest req) {
        if (req == null) return "Request body is missing";
        if (isBlank(req.garageName())) return "Garage name is required";
        if (isBlank(req.province())) return "Province is required";
        if (matchProvince(req.province()) == null) return "Province must be one of: " + String.join(", ", PROVINCES);
        if (isBlank(req.address())) return "Address is required";
        if (isBlank(req.phone())) return "Phone number is required";
        if (isBlank(req.fullName())) return "Full name is required";
        if (isBlank(req.username())) return "Username is required";
        if (req.username().trim().length() < 3) return "Username must be at least 3 characters";
        if (isBlank(req.password())) return "Password is required";
        if (req.password().length() < 6) return "Password must be at least 6 characters";
        return null;
    }

    // Accepts "lusaka", "LUSAKA" etc. and returns the correct spelling
    private static String matchProvince(String input) {
        if (input == null) return null;
        for (String p : PROVINCES) {
            if (p.equalsIgnoreCase(input.trim())) return p;
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}