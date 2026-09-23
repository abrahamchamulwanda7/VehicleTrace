package com.vehicletrace;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create()
                .get("/", ctx -> ctx.result("VehicleTrace backend is running"))
                .start(7070);
    }
}