package com.vehicletrace;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Sessions {

    // Information about a logged-in staff member
    public record SessionUser(
            int userId,
            int garageId,
            String garageName,
            String fullName,
            String username,
            String role
    ) {
    }

    private static final Map<String, SessionUser> ACTIVE = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    // Creates a new token for a user who has just logged in
    public static String create(SessionUser user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ACTIVE.put(token, user);
        return token;
    }

    // Finds the user for a token, or null if the token is not valid
    public static SessionUser get(String token) {
        if (token == null) return null;
        return ACTIVE.get(token);
    }

    // Logs a user out
    public static void remove(String token) {
        if (token != null) ACTIVE.remove(token);
    }
}