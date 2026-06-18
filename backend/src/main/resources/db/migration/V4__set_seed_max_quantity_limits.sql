SET @add_max_qty_column = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE dishes ADD COLUMN max_quantity_per_order INT NOT NULL DEFAULT 10 AFTER is_available',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dishes'
    AND column_name = 'max_quantity_per_order'
);
PREPARE add_max_qty_column_stmt FROM @add_max_qty_column;
EXECUTE add_max_qty_column_stmt;
DEALLOCATE PREPARE add_max_qty_column_stmt;

UPDATE dishes
SET max_quantity_per_order = 5
WHERE name = '宫保鸡丁';

UPDATE dishes
SET max_quantity_per_order = 8
WHERE name = '鱼香肉丝';

ALTER TABLE dishes
ADD CONSTRAINT ck_dishes_max_qty CHECK (max_quantity_per_order > 0);
