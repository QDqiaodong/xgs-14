SET NAMES utf8mb4;

USE chuanzi;

INSERT INTO users(role, username, password_hash, display_name, phone, created_at, updated_at)
SELECT 'MERCHANT', 'merchant_admin', 'eba4329da41dcee7e8a71fb60e09b0a5ea5df037464aed002cd9f3ba96eb4e6d', '默认商家', '13800000000', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'merchant_admin');

INSERT INTO users(role, username, password_hash, display_name, phone, created_at, updated_at)
SELECT 'CUSTOMER', 'customer_test', 'c932fc90a60b2fcc1e408e06ba5bd992096b213e87829bb1d9343b7ca43d120b', '测试顾客', '13900000000', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'customer_test');

INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at)
SELECT 'HOT', '热菜', 10, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dish_categories WHERE code = 'HOT');

INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at)
SELECT 'COLD', '凉菜', 20, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dish_categories WHERE code = 'COLD');

INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at)
SELECT 'STAPLE', '主食', 30, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dish_categories WHERE code = 'STAPLE');

INSERT INTO dishes(name, price_cents, description, is_available, created_at, updated_at)
SELECT '宫保鸡丁', 3200, '经典下饭菜', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name = '宫保鸡丁');

INSERT INTO dishes(name, price_cents, description, is_available, created_at, updated_at)
SELECT '鱼香肉丝', 2800, '微辣开胃', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name = '鱼香肉丝');

INSERT INTO dishes(name, price_cents, description, is_available, created_at, updated_at)
SELECT '青椒土豆丝', 1800, '家常清爽', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dishes WHERE name = '青椒土豆丝');

UPDATE dishes
SET category_id = (SELECT id FROM dish_categories WHERE code = 'HOT')
WHERE name IN ('宫保鸡丁', '鱼香肉丝') AND category_id IS NULL;

UPDATE dishes
SET category_id = (SELECT id FROM dish_categories WHERE code = 'COLD')
WHERE name = '青椒土豆丝' AND category_id IS NULL;
