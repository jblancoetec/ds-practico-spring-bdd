CREATE DATABASE IF NOT EXISTS practico_bdd
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE practico_bdd;

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    precio DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0)
);

INSERT INTO productos (nombre, precio, stock) VALUES
    ('Cuaderno A5', 950.00, 25),
    ('Lapicera azul', 220.50, 100);
