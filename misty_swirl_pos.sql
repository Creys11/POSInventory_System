-- ============================================================
-- Misty Swirl POS and Inventory System - MySQL / MariaDB (XAMPP)
-- Import in phpMyAdmin: Import tab -> choose this file -> Go
-- (or paste it into the SQL tab). Creates its own database.
-- ============================================================

CREATE DATABASE IF NOT EXISTS MSPOS1
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE MSPOS1;

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- LOGIN ACCOUNTS (one ADMIN and one STAFF, like the original app)
-- Passwords are stored as SHA-256 hashes.
CREATE TABLE users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash CHAR(64)     NOT NULL,
  role          ENUM('ADMIN','STAFF') NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO users (username, password_hash, role) VALUES
  ('admin', SHA2('admin123', 256), 'ADMIN'),
  ('staff', SHA2('staff123', 256), 'STAFF');

-- MENU, PRICES AND STOCK
CREATE TABLE products (
  id    INT AUTO_INCREMENT PRIMARY KEY,
  name  VARCHAR(120) NOT NULL UNIQUE,
  price INT NOT NULL,
  stock INT NOT NULL DEFAULT 20,
  CONSTRAINT chk_price CHECK (price >= 0),
  CONSTRAINT chk_stock CHECK (stock >= 0)
) ENGINE=InnoDB;

INSERT INTO products (name, price, stock) VALUES
  ('Frozen Yogurt - Petit', 49, 20),
  ('Frozen Yogurt - Classic', 59, 20),
  ('Sundae - Caramel - Petit', 29, 20),
  ('Sundae - Caramel - Classic', 39, 20),
  ('Sundae - Chocolate - Petit', 29, 20),
  ('Sundae - Chocolate - Classic', 39, 20),
  ('Sundae - Matcha - Petit', 29, 20),
  ('Sundae - Matcha - Classic', 39, 20),
  ('Sundae - Strawberry - Petit', 29, 20),
  ('Sundae - Strawberry - Classic', 39, 20),
  ('Yogurt Overload Sundae', 129, 20),
  ('Sauce - Chocolate', 10, 20),
  ('Sauce - Caramel', 10, 20),
  ('Sauce - Biscoff', 10, 20),
  ('Sauce - Matcha', 10, 20),
  ('Sauce - Ube', 10, 20),
  ('Add-on - Chocolate Chips', 15, 20),
  ('Add-on - Marshmallows', 10, 20),
  ('Add-on - Rice Crispies', 10, 20),
  ('Add-on - Crushed Biscoff', 15, 20),
  ('Add-on - Alcapone', 10, 20),
  ('Add-on - Nips', 10, 20),
  ('Add-on - Pearls', 15, 20),
  ('Add-on - Crushed Oreo', 10, 20),
  ('Add-on - Sprinkles', 10, 20),
  ('Add-on - Kitkat', 20, 20),
  ('Add-on - Biscoff', 10, 20),
  ('Add-on - Crushed Grahams', 10, 20),
  ('Misty Premium - Biscoff Sundae', 59, 20),
  ('Misty Premium - Kitkat Sundae', 59, 20),
  ('Misty Premium - Oreo Sundae', 59, 20),
  ('Misty Premium - Marshie Sundae', 49, 20),
  ('Misty Premium - Graham Sundae', 49, 20),
  ('Misty Premium - Nutella Sundae', 59, 20),
  ('Soda Float - Fizz', 49, 20),
  ('Soda Float - Fountain', 59, 20),
  ('Soda Float - Deluxe', 69, 20),
  ('Ice Cream Cone - Classic', 20, 20),
  ('Ice Cream Cone - With Dip', 25, 20),
  ('Belgian Cone - Classic', 25, 20),
  ('Belgian Cone - With Dip', 30, 20),
  ('Coffee Float Series - Brewed Coffee - Fizz', 49, 20),
  ('Coffee Float Series - Brewed Coffee - Fountain', 59, 20),
  ('Coffee Float Series - Brewed Coffee - Deluxe', 69, 20),
  ('Coffee Float Series - Coffee Float - Fizz', 49, 20),
  ('Coffee Float Series - Coffee Float - Fountain', 59, 20),
  ('Coffee Float Series - Coffee Float - Deluxe', 69, 20),
  ('Coffee Float Series - Mocha Float - Fizz', 49, 20),
  ('Coffee Float Series - Mocha Float - Fountain', 59, 20),
  ('Coffee Float Series - Mocha Float - Deluxe', 69, 20),
  ('Oreo Float Series - Oreo Milk - Fizz', 49, 20),
  ('Oreo Float Series - Oreo Milk - Fountain', 59, 20),
  ('Oreo Float Series - Oreo Milk - Deluxe', 69, 20),
  ('Oreo Float Series - Oreo Chocolate - Fizz', 49, 20),
  ('Oreo Float Series - Oreo Chocolate - Fountain', 59, 20),
  ('Oreo Float Series - Oreo Chocolate - Deluxe', 69, 20),
  ('Oreo Float Series - Oreo Milk Caramel - Fizz', 49, 20),
  ('Oreo Float Series - Oreo Milk Caramel - Fountain', 59, 20),
  ('Oreo Float Series - Oreo Milk Caramel - Deluxe', 69, 20),
  ('Mini Donuts - 3pcs Classic', 25, 20),
  ('Mini Donuts - 3pcs Pro - 1 Add-on Topping', 30, 20),
  ('Mini Donuts - 3pcs Pro - 2 Add-ons', 35, 20),
  ('Nachos - Small', 39, 20),
  ('Nachos - Big', 79, 20),
  ('Other Sundae - Bobba Sundae', 59, 20),
  ('Other Sundae - Affogato Sundae', 59, 20);

-- COMPLETED ORDERS (order numbers start at 1001 -> #ORD-1001)
CREATE TABLE orders (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_by   ENUM('ADMIN','STAFF') NOT NULL,
  payment_method VARCHAR(20) NOT NULL,
  total          DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=1001;

CREATE TABLE order_items (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  order_id     INT NOT NULL,
  product_id   INT NULL,
  product_name VARCHAR(120) NOT NULL,
  quantity     INT NOT NULL,
  unit_price   INT NOT NULL,
  CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders(id)   ON DELETE CASCADE,
  CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
) ENGINE=InnoDB;
