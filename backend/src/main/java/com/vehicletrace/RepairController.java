package com.vehicletrace;

import io.javalin.http.Context;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepairController {

    // POST /api/repairs  (FR6: record repair, FR7: parts used, FR8: repeat problem check)
    public static void record(Context ctx) throws Exception {
        Sessions.SessionUser user = AuthController.requireUser(ctx);

        RepairRequest req = ctx.bodyAsClass(RepairRequest.class);

        // 1. Check the input
        String error = validate(req);
        if (error != null) {
            ctx.status(400).json(Map.of("error", error));
            return;
        }

        LocalDate repairDate = isBlank(req.repairDate())
                ? LocalDate.now()
                : LocalDate.parse(req.repairDate().trim());

        List<RepairRequest.PartRequest> parts = req.parts() == null ? List.of() : req.parts();
        String plate = VehicleController.normalizePlate(req.numberPlate());

        try (Connection conn = Database.getConnection()) {

            // 2. Find the vehicle
            int vehicleId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT vehicle_id FROM vehicles WHERE number_plate = ?")) {
                ps.setString(1, plate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        ctx.status(404).json(Map.of(
                                "error", "No vehicle found with number plate " + plate + ". Register the vehicle first.",
                                "numberPlate", plate
                        ));
                        return;
                    }
                    vehicleId = rs.getInt("vehicle_id");
                }
            }

            // 3. FR8: compare with ALL previous repairs on this vehicle, from any garage
            List<Map<String, Object>> matchingRepairs = new ArrayList<>();
            String previousSql = """
                    SELECT r.repair_id, r.problem_description, r.repair_date,
                           g.garage_name, g.province
                    FROM repairs r
                    JOIN garages g ON g.garage_id = r.garage_id
                    WHERE r.vehicle_id = ?
                    ORDER BY r.repair_date DESC, r.repair_id DESC
                    """;
            try (PreparedStatement ps = conn.prepareStatement(previousSql)) {
                ps.setInt(1, vehicleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String oldProblem = rs.getString("problem_description");
                        if (RepeatDetector.isRepeat(req.problemDescription(), oldProblem)) {
                            Map<String, Object> match = new LinkedHashMap<>();
                            match.put("repairId", rs.getInt("repair_id"));
                            match.put("repairDate", rs.getString("repair_date"));
                            match.put("garageName", rs.getString("garage_name"));
                            match.put("province", rs.getString("province"));
                            match.put("problemDescription", oldProblem);
                            matchingRepairs.add(match);
                        }
                    }
                }
            }
            boolean isRepeat = !matchingRepairs.isEmpty();

            // 4. Save the repair and its parts together (transaction)
            int repairId;
            conn.setAutoCommit(false);
            try {
                String repairSql = """
                        INSERT INTO repairs (vehicle_id, garage_id, user_id, problem_description,
                                             work_done, cost, repair_date, is_repeat_problem)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(repairSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, vehicleId);
                    ps.setInt(2, user.garageId());
                    ps.setInt(3, user.userId());
                    ps.setString(4, req.problemDescription().trim());
                    ps.setString(5, req.workDone().trim());
                    ps.setBigDecimal(6, req.cost());
                    ps.setDate(7, java.sql.Date.valueOf(repairDate));
                    ps.setBoolean(8, isRepeat);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        repairId = keys.getInt(1);
                    }
                }

                // FR7: parts used
                if (!parts.isEmpty()) {
                    String partSql = "INSERT INTO parts_used (repair_id, part_name, quantity, cost) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(partSql)) {
                        for (RepairRequest.PartRequest part : parts) {
                            ps.setInt(1, repairId);
                            ps.setString(2, part.partName().trim());
                            ps.setInt(3, part.quantity());
                            ps.setBigDecimal(4, part.cost());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

            // 5. Send the result back
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Repair recorded successfully");
            result.put("repairId", repairId);
            result.put("numberPlate", plate);
            result.put("garageName", user.garageName());
            result.put("recordedBy", user.fullName());
            result.put("repairDate", repairDate.toString());
            result.put("partsRecorded", parts.size());
            result.put("isRepeatProblem", isRepeat);
            if (isRepeat) {
                result.put("warning", "Repeat Problem: this problem has been recorded "
                        + matchingRepairs.size() + " time(s) before on this vehicle.");
                result.put("matchingRepairs", matchingRepairs);
            }
            ctx.status(201).json(result);
        }
    }

    // Returns an error message, or null if everything is valid
    private static String validate(RepairRequest req) {
        if (req == null) return "Request body is missing";
        if (isBlank(req.numberPlate())) return "Number plate is required";
        if (isBlank(req.problemDescription())) return "Problem description is required";
        if (isBlank(req.workDone())) return "Work done is required";
        if (req.cost() == null) return "Cost is required";
        if (req.cost().compareTo(BigDecimal.ZERO) < 0) return "Cost cannot be negative";

        if (!isBlank(req.repairDate())) {
            try {
                LocalDate date = LocalDate.parse(req.repairDate().trim());
                if (date.isAfter(LocalDate.now())) return "Repair date cannot be in the future";
            } catch (DateTimeParseException e) {
                return "Repair date must be in the format YYYY-MM-DD";
            }
        }

        if (req.parts() != null) {
            for (RepairRequest.PartRequest part : req.parts()) {
                if (part == null || isBlank(part.partName())) return "Each part needs a name";
                if (part.quantity() == null || part.quantity() < 1) return "Part quantity must be at least 1";
                if (part.cost() == null) return "Each part needs a cost";
                if (part.cost().compareTo(BigDecimal.ZERO) < 0) return "Part cost cannot be negative";
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}