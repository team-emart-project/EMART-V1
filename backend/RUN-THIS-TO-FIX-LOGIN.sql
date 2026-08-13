-- =====================================================================
--  e-MART — verify the login fix
--
--  This file does NOT rebuild anything. Run the two real scripts first,
--  in this order, then run this to confirm it worked.
--
--  IN MYSQL WORKBENCH
--     File > Open SQL Script  ->  emart_schema.sql     ->  run (lightning bolt)
--     File > Open SQL Script  ->  emart_seed_data.sql  ->  run
--     File > Open SQL Script  ->  this file            ->  run
--
--  ON THE COMMAND LINE
--     mysql -u root -p emart < emart_schema.sql
--     mysql -u root -p emart < emart_seed_data.sql
--     mysql -u root -p emart < RUN-THIS-TO-FIX-LOGIN.sql
--
--  (There is no SOURCE command here on purpose — SOURCE only works in the
--   mysql CLI and silently does nothing in Workbench.)
-- =====================================================================

USE emart;

-- 1. All 13 tables present?   Expect: table_count = 13
SELECT COUNT(*) AS table_count
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'emart';

-- 2. The three columns that broke.   Expect: every status = 'ok'
SELECT 'users.created_at' AS column_checked,
       IF(COUNT(*) > 0, 'ok', '*** STILL MISSING ***') AS status
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='users' AND COLUMN_NAME='created_at'
UNION ALL
SELECT 'cart.created_at',
       IF(COUNT(*) > 0, 'ok', '*** STILL MISSING ***')
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='cart' AND COLUMN_NAME='created_at'
UNION ALL
SELECT 'cart_items.added_at',
       IF(COUNT(*) > 0, 'ok', '*** STILL MISSING ***')
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='cart_items' AND COLUMN_NAME='added_at';

-- 3. Demo users loaded?   Expect: 4 rows
SELECT user_id, membership_no, email, is_cardholder, is_active
  FROM users;

-- 4. This is the exact query the login endpoint runs. If it returns a row
--    without error, login WILL work.
SELECT user_id, email, is_active, created_at
  FROM users
 WHERE email = 'rishi.chhalotre@example.com';
