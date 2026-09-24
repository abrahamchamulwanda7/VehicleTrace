DROP DATABASE IF EXISTS vehicletrace;

CREATE DATABASE vehicletrace
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_uca1400_ai_ci;

USE vehicletrace;

CREATE TABLE garages (
    garage_id    INT AUTO_INCREMENT PRIMARY KEY,
    garage_name  VARCHAR(100) NOT NULL,
    province     ENUM('Central','Copperbelt','Eastern','Luapula','Lusaka',
                      'Muchinga','Northern','North-Western','Southern','Western') NOT NULL,
    address      VARCHAR(255) NOT NULL,
    phone        VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (garage_name, province)
) ENGINE=InnoDB;

CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    garage_id      INT NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
    is_active      BOOLEAN NOT NULL