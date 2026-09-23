package com.vehicletrace;

import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Map;

public class VehicleController {

    // POST /api/vehicles  (FR3: register a vehicle)
    public static void register(Context ctx) throws Exception {
        AuthController.requireUser(ctx);

        VehicleRequest req = ctx.bodyAsClass(VehicleRequest.class);

        String error = validate(req);
        if (error != null) {
            ctx.status(400).json(Map.of("error", error));
            return;
        }

        String plate = normalizePlate(req.numberPlate());

        String sql = "INSERT INTO vehicles (number_plate, make, model, owner_name, owner_phone) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plate);
            ps.setString(2, req.make().trim());
            ps.setString(3, req.model().trim());
            ps.setString(4, req.ownerName().trim());
            ps.setString(5, req.ownerPhone().trim());
            ps.executeUpdate();

            int vehicleId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                vehicleId = keys.getInt(1);
            }

            ctx.status(201).json(Map.of(
                    "message", "Vehicle registered successfully",
                    "vehicleId", vehicleId,
                    "numberPlate", plate,
                    "make", req.make().trim(),
                    "model", req.model().trim(),
                    "ownerName", req.ownerName().trim(),
                    "ownerPhone", req.ownerPhone().trim()
            ));

        } catch (SQLIntegrityConstraintViolationException e) {
            ctx.status(409).json(Map.of("error", "A vehicle with number plate " + plate + " is already registered"));
        }
    }

    // Makes plates consistent: "abc 1234" -> "ABC1234"
    public static String normalizePlate(String input) {
        if (input == null) return "";
        return input.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    // Returns an error message, or null if everything is valid
    private static String validate(VehicleRequest req) {
        if (req == null) return "Request body is missing";
        if (isBlank(req.numberPlate())) return "Number plate is required";
        String plate = normalizePlate(req.numberPlate());
        if (plate.length() < 2 || plate.length() > 15) return "Number plate must be 2 to 15 letters or numbers";
        if (isBlank(req.make())) return "Make is required";
        if (isBlank(req.model())) return "Model is required";
        if (isBlank(req.ownerName())) return "Owner's name is required";
        if (isBlank(req.ownerPhone())) return "Owner's phone number is required";
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}