package com.vehicletrace;

import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    // GET /api/vehicles/{plate}  (FR4: search by plate, FR5: full repair history)
    public static void search(Context ctx) throws Exception {
        AuthController.requireUser(ctx);

        String plate = normalizePlate(ctx.pathParam("plate"));

        try (Connection conn = Database.getConnection()) {

            // 1. Find the vehicle
            int vehicleId;
            Map<String, Object> vehicle = new LinkedHashMap<>();
            String vehicleSql = "SELECT vehicle_id, number_plate, make, model, owner_name, owner_phone, created_at "
                    + "FROM vehicles WHERE number_plate = ?";
            try (PreparedStatement ps = conn.prepareStatement(vehicleSql)) {
                ps.setString(1, plate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        ctx.status(404).json(Map.of(
                                "error", "No vehicle found with number plate " + plate,
                                "numberPlate", plate
                        ));
                        return;
                    }
                    vehicleId = rs.getInt("vehicle_id");
                    vehicle.put("vehicleId", vehicleId);
                    vehicle.put("numberPlate", rs.getString("number_plate"));
                    vehicle.put("make", rs.getString("make"));
                    vehicle.put("model", rs.getString("model"));
                    vehicle.put("ownerName", rs.getString("owner_name"));
                    vehicle.put("ownerPhone", rs.getString("owner_phone"));
                    vehicle.put("registeredOn", rs.getString("created_at"));
                }
            }

            // 2. Get all parts used on this vehicle, grouped by repair
            Map<Integer, List<Map<String, Object>>> partsByRepair = new HashMap<>();
            String partsSql = """
                    SELECT p.part_id, p.repair_id, p.part_name, p.quantity, p.cost
                    FROM parts_used p
                    JOIN repairs r ON r.repair_id = p.repair_id
                    WHERE r.vehicle_id = ?
                    ORDER BY p.part_id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(partsSql)) {
                ps.setInt(1, vehicleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> part = new LinkedHashMap<>();
                        part.put("partId", rs.getInt("part_id"));
                        part.put("partName", rs.getString("part_name"));
                        part.put("quantity", rs.getInt("quantity"));
                        part.put("cost", rs.getBigDecimal("cost"));
                        partsByRepair.computeIfAbsent(rs.getInt("repair_id"), k -> new ArrayList<>()).add(part);
                    }
                }
            }

            // 3. Get all repairs from ALL garages, newest first
            List<Map<String, Object>> repairs = new ArrayList<>();
            String repairsSql = """
                    SELECT r.repair_id, r.problem_description, r.work_done, r.cost,
                           r.repair_date, r.is_repeat_problem,
                           g.garage_id, g.garage_name, g.province,
                           u.full_name AS recorded_by
                    FROM repairs r
                    JOIN garages g ON g.garage_id = r.garage_id
                    JOIN users u ON u.user_id = r.user_id
                    WHERE r.vehicle_id = ?
                    ORDER BY r.repair_date DESC, r.repair_id DESC
                    """;
            try (PreparedStatement ps = conn.prepareStatement(repairsSql)) {
                ps.setInt(1, vehicleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int repairId = rs.getInt("repair_id");
                        Map<String, Object> repair = new LinkedHashMap<>();
                        repair.put("repairId", repairId);
                        repair.put("repairDate", rs.getString("repair_date"));
                        repair.put("garageId", rs.getInt("garage_id"));
                        repair.put("garageName", rs.getString("garage_name"));
                        repair.put("province", rs.getString("province"));
                        repair.put("recordedBy", rs.getString("recorded_by"));
                        repair.put("problemDescription", rs.getString("problem_description"));
                        repair.put("workDone", rs.getString("work_done"));
                        repair.put("cost", rs.getBigDecimal("cost"));
                        repair.put("isRepeatProblem", rs.getBoolean("is_repeat_problem"));
                        repair.put("parts", partsByRepair.getOrDefault(repairId, List.of()));
                        repairs.add(repair);
                    }
                }
            }

            // 4. Send everything back
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("vehicle", vehicle);
            result.put("totalRepairs", repairs.size());
            result.put("repairs", repairs);
            ctx.json(result);
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