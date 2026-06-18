ALTER TABLE dishes
ADD COLUMN max_quantity_per_order INT NOT NULL DEFAULT 10
AFTER is_available;
