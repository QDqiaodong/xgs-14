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

SET @add_category_id = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE dishes ADD COLUMN category_id BIGINT NULL DEFAULT NULL AFTER is_available',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dishes'
    AND column_name = 'category_id'
);
PREPARE add_category_id_stmt FROM @add_category_id;
EXECUTE add_category_id_stmt;
DEALLOCATE PREPARE add_category_id_stmt;

SET @add_dish_category_fk = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE dishes ADD CONSTRAINT fk_dishes_category FOREIGN KEY (category_id) REFERENCES dish_categories(id)',
    'SELECT 1'
  )
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'dishes'
    AND constraint_name = 'fk_dishes_category'
);
PREPARE add_dish_category_fk_stmt FROM @add_dish_category_fk;
EXECUTE add_dish_category_fk_stmt;
DEALLOCATE PREPARE add_dish_category_fk_stmt;

INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at)
SELECT 'HOT', '热菜', 10, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dish_categories WHERE code = 'HOT');

INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at)
SELECT 'COLD', '凉菜', 20, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dish_categories WHERE code = 'COLD');

INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at)
SELECT 'STAPLE', '主食', 30, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dish_categories WHERE code = 'STAPLE');

UPDATE dishes
SET category_id = (SELECT id FROM dish_categories WHERE code = 'HOT')
WHERE name IN ('宫保鸡丁', '鱼香肉丝') AND category_id IS NULL;

UPDATE dishes
SET category_id = (SELECT id FROM dish_categories WHERE code = 'COLD')
WHERE name = '青椒土豆丝' AND category_id IS NULL;
