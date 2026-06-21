SET @add_sale_date = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE orders ADD COLUMN sale_date DATE NULL AFTER total_cents',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND column_name = 'sale_date'
);
PREPARE add_sale_date_stmt FROM @add_sale_date;
EXECUTE add_sale_date_stmt;
DEALLOCATE PREPARE add_sale_date_stmt;

UPDATE orders SET sale_date = DATE(created_at) WHERE sale_date IS NULL;

SET @make_sale_date_not_null = (
  SELECT IF(
    is_nullable = 'YES',
    'ALTER TABLE orders MODIFY COLUMN sale_date DATE NOT NULL',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND column_name = 'sale_date'
);
PREPARE make_sale_date_not_null_stmt FROM @make_sale_date_not_null;
EXECUTE make_sale_date_not_null_stmt;
DEALLOCATE PREPARE make_sale_date_not_null_stmt;

SET @add_idx_sale_date = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE orders ADD INDEX idx_orders_sale_date (sale_date)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND index_name = 'idx_orders_sale_date'
);
PREPARE add_idx_sale_date_stmt FROM @add_idx_sale_date;
EXECUTE add_idx_sale_date_stmt;
DEALLOCATE PREPARE add_idx_sale_date_stmt;
