usersvehicletraceDROP DATABASE IF EXISTS vehicletrace;

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
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (garage_id) REFERENCES garages(garage_id)
) ENGINE=InnoDB;

CREATE TABLE vehicles (
    vehicle_id    INT AUTO_INCREMENT PRIMARY KEY,
    number_plate  VARCHAR(15)  NOT NULL UNIQUE,
    make          VARCHAR(50)  NOT NULL,
    model         VARCHAR(50)  NOT NULL,
    owner_name    VARCHAR(100) NOT NULL,
    owner_phone   VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE repairs (
    repair_id            INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id           INT NOT NULL,
    garage_id            INT NOT NULL,
    user_id              INT NOT NULL,
    problem_description  TEXT NOT NULL,
    work_done            TEXT NOT NULL,
    cost                 DECIMAL(10,2) NOT NULL CHECK (cost >= 0),
    repair_date          DATE NOT NULL,
    is_repeat_problem    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    FOREIGN KEY (garage_id)  REFERENCES garages(garage_id),
    FOREIGN KEY (user_id)    REFERENCES users(user_id),
    INDEX idx_vehicle_date (vehicle_id, repair_date)
) ENGINE=InnoDB;

CREATE TABLE parts_used (
    part_id    INT AUTO_INCREMENT PRIMARY KEY,
    repair_id  INT NOT NULL,
    part_name  VARCHAR(100) NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0),
    cost       DECIMAL(10,2) NOT NULL CHECK (cost >= 0),
    FOREIGN KEY (repair_id) REFERENCES repairs(repair_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE USER IF NOT EXISTS 'vehicletrace_app'@'localhost' IDENTIFIED BY 'VtApp2026!';
CREATE USER IF NOT EXISTS 'vehicletrace_app'@'127.0.0.1' IDENTIFIED BY 'VtApp2026!';
GRANT SELECT, INSERT, UPDATE, DELETE ON vehicletrace.* TO 'vehicletrace_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON vehicletrace.* TO 'vehicletrace_app'@'127.0.0.1';
FLUSH PRIVILEGES;