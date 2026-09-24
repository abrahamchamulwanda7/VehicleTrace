package com.vehicletrace;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserController {

    // POST /api/users  (FR2: admin adds a staff login for their own garage)
    public static void create(Context ctx) throws Exception {
        Sessions.SessionUser admin = requireAdmin(ctx);

        CreateUserRequest req = ctx.bodyAsClass(CreateUserRequest.class);

        String error = validate(req);
        if (error != null) {
            ctx.status(400).json(Map.of("error", error));
            return;
        }

        String role = (req.role() == null || req.role().isBlank())
                ? "STAFF"
                : req.role().trim().toUpperCase();

        String passwordHash = BCrypt.hashpw(req.password(), BCrypt.gensalt());

        String sql = "INSERT INTO users (garage_id, full_name, username, password_hash, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, admin.garageId());
            ps.setString(2, req.fullName().trim());
            ps.setString(3, req.username().trim());
            ps.setString(4, passwordHash);
            ps.setString(5, role);
            ps.executeUpdate();

            int userId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                userId = keys.getInt(1);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Staff account created successfully");
            result.put("userId", userId);
            result.put("fullName", req.fullName().trim());
            result.put("username", req.username().trim());
            result.put("role", role);
            result.put("garageName", admin.garageName());
            ctx.status(201).json(result);

        } catch (SQLIntegrityConstraintViolationException e) {
            ctx.status(409).json(Map.of("error", "Username is already taken"));
        }
    }

    // GET /api/users  (admin sees all staff in their own garage)
    public static void list(Context ctx) throws Exception {
        Sessions.SessionUser admin = requireAdmin(ctx);

        String sql = """
                SELECT user_id, full_name, username, role, created_at
                FROM users
                WHERE garage_id = ?
                ORDER BY role, full_name
                """;

        List<Map<String, Object>> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, admin.garageId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> user = new LinkedHashMap<>();
                    user.put("userId", rs.getInt("user_id"));
                    user.put("fullName", rs.getString("full_name"));
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role"));
                    user.put("createdAt", rs.getString("created_at"));
                    users.add(user);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("garageName", admin.garageName());
        result.put("totalUsers", users.size());
        result.put("users", users);
        ctx.json(result);
    }

    // Stops the request unless the user is logged in AND is an ADMIN
    private static Sessions.SessionUser requireAdmin(Context ctx) {
        Sessions.SessionUser user = AuthController.requireUser(ctx);
        if (!"ADMIN".equals(user.role())) {
            throw new ForbiddenResponse("Only the garage admin can manage staff accounts");
        }
        return user;
    }

    // Returns an error message, or null if everything is valid
    private static String validate(CreateUserRequest req) {
        if (req == null) return "Request body is missing";
        if (isBlank(req.fullName())) return "Full name is required";
        if (isBlank(req.username())) return "Username is required";
        if (req.username().trim().length() < 3) return "Username must be at least 3 characters";
        if (isBlank(req.password())) return "Password is required";
        if (req.password().length() < 6) return "Password must be at least 6 characters";
        if (req.role() != null && !req.role().isBlank()) {
            String role = req.role().trim().toUpperCase();
            if (!role.equals("ADMIN") && !role.equals("STAFF")) return "Role must be ADMIN or STAFF";
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}