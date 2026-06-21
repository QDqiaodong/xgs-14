SET @add_confirmed_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE orders ADD COLUMN confirmed_at DATETIME NULL AFTER updated_at',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND column_name = 'confirmed_at'
);
PREPARE add_confirmed_at_stmt FROM @add_confirmed_at;
EXECUTE add_confirmed_at_stmt;
DEALLOCATE PREPARE add_confirmed_at_stmt;

SET @add_done_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE orders ADD COLUMN done_at DATETIME NULL AFTER confirmed_at',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND column_name = 'done_at'
);
PREPARE add_done_at_stmt FROM @add_done_at;
EXECUTE add_done_at_stmt;
DEALLOCATE PREPARE add_done_at_stmt;

SET @add_cancelled_at = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE orders ADD COLUMN cancelled_at DATETIME NULL AFTER done_at',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND column_name = 'cancelled_at'
);
PREPARE add_cancelled_at_stmt FROM @add_cancelled_at;
EXECUTE add_cancelled_at_stmt;
DEALLOCATE PREPARE add_cancelled_at_stmt;

UPDATE orders SET confirmed_at = updated_at WHERE status IN ('CONFIRMED', 'DONE') AND confirmed_at IS NULL;
UPDATE orders SET done_at = updated_at WHERE status = 'DONE' AND done_at IS NULL;
UPDATE orders SET cancelled_at = updated_at WHERE status = 'CANCELLED' AND cancelled_at IS NULL;
