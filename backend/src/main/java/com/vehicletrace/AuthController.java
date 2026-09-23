package com.vehicletrace;

import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class AuthController {

    // POST /api/auth/login
    public static void login(Context ctx) throws Exception {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);

        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            ctx.status(400).json(Map.of("error", "Username and password are required"));
            return;
        }

        String sql = """
                SELECT u.user_id, u.garage_id, g.garage_name, u.full_name,
                       u.username, u.password_hash, u.role
                FROM users u
                JOIN garages g ON g.garage_id = u.garage_id
                WHERE u.username = ?
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.username().trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !BCrypt.checkpw(req.password(), rs.getString("password_hash"))) {
                    ctx.status(401).json(Map.of("error", "Invalid username or password"));
                    return;
                }

                Sessions.SessionUser user = new Sessions.SessionUser(
                        rs.getInt("user_id"),
                        rs.getInt("garage_id"),
                        rs.getString("garage_name"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("role")
                );

                String token = Sessions.create(user);

                ctx.json(Map.of(
                        "message", "Login successful",
                        "token", token,
                        "user", user
                ));
            }
        }
    }

    // GET /api/auth/me
    public static void me(Context ctx) {
        Sessions.SessionUser user = Sessions.get(getToken(ctx));
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Not logged in"));
            return;
        }
        ctx.json(user);
    }

    // POST /api/auth/logout
    public static void logout(Context ctx) {
        Sessions.remove(getToken(ctx));
        ctx.json(Map.of("message", "Logged out"));
    }

    // Reads the token from the "Authorization: Bearer <token>" header
    public static String getToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}