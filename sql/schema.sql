SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS chuanzi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chuanzi;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role VARCHAR(16) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) DEFAULT '',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_users_username UNIQUE (username),
  CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'MERCHANT'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS dish_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_dish_categories_code UNIQUE (code),
  CONSTRAINT ck_dish_categories_sort CHECK (sort_order >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS dishes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  price_cents INT NOT NULL,
  description VARCHAR(512) DEFAULT '',
  is_available TINYINT(1) NOT NULL DEFAULT 1,
  max_quantity_per_order INT NOT NULL DEFAULT 10,
  category_id BIGINT NULL DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_dishes_category_id (category_id),
  CONSTRAINT fk_dishes_category FOREIGN KEY (category_id) REFERENCES dish_categories(id),
  CONSTRAINT ck_dishes_price CHECK (price_cents > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  total_cents INT NOT NULL,
  sale_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_orders_user_id (user_id),
  KEY idx_orders_status (status),
  KEY idx_orders_sale_date (sale_date),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT ck_orders_status CHECK (status IN ('NEW', 'CONFIRMED', 'CANCELLED', 'DONE')),
  CONSTRAINT ck_orders_total CHECK (total_cents >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  dish_id BIGINT NOT NULL,
  dish_name_snapshot VARCHAR(128) NOT NULL,
  price_cents_snapshot INT NOT NULL,
  quantity INT NOT NULL,
  KEY idx_order_items_order_id (order_id),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_items_dish FOREIGN KEY (dish_id) REFERENCES dishes(id),
  CONSTRAINT ck_order_items_price CHECK (price_cents_snapshot > 0),
  CONSTRAINT ck_order_items_qty CHECK (quantity > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS dish_daily_quotas (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dish_id BIGINT NOT NULL,
  sale_date DATE NOT NULL,
  available_quantity INT NOT NULL,
  sold_quantity INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_dish_daily_quotas_dish_date (dish_id, sale_date),
  CONSTRAINT fk_dish_daily_quotas_dish FOREIGN KEY (dish_id) REFERENCES dishes(id),
  CONSTRAINT uk_dish_daily_quotas_dish_date UNIQUE (dish_id, sale_date),
  CONSTRAINT ck_ddq_available CHECK (available_quantity >= 0),
  CONSTRAINT ck_ddq_sold CHECK (sold_quantity >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sessions (
  token VARCHAR(64) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_sessions_user_id (user_id),
  KEY idx_sessions_expires_at (expires_at),
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE = InnoDB;
