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
