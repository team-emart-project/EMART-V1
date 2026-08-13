-- =====================================================================
-- e-MART — schema drift repair
--
-- Symptom:  Unknown column 'u1_0.created_at' in 'field list'
--
-- Cause:    the `emart` database was not built from emart_schema.sql.
--           Hibernate's old `ddl-auto=update` setting tried to patch it and
--           FAILED on three NOT NULL timestamp columns, because MySQL 9 will
--           not add a NOT NULL datetime to a table that already has rows
--           (it would need the illegal '0000-00-00' default).
--
--           Those three ALTERs are the ones that errored in your startup log:
--               users.created_at
--               cart.created_at
--               cart_items.added_at
--
-- =====================================================================
-- STEP 1 — RUN THIS FIRST to see what your database actually has.
-- =====================================================================

USE emart;

-- Which of the 13 required tables exist?
SELECT  t.expected_table,
        CASE WHEN it.TABLE_NAME IS NULL THEN '*** MISSING ***' ELSE 'ok' END AS status
FROM (
    SELECT 'users' AS expected_table UNION ALL SELECT 'address'
    UNION ALL SELECT 'emart_card'      UNION ALL SELECT 'category_master'
    UNION ALL SELECT 'product_master'  UNION ALL SELECT 'config_master'
    UNION ALL SELECT 'prod_dtl_master' UNION ALL SELECT 'cart'
    UNION ALL SELECT 'cart_items'      UNION ALL SELECT 'wishlist'
    UNION ALL SELECT 'orders'          UNION ALL SELECT 'order_details'
    UNION ALL SELECT 'payment'
) t
LEFT JOIN information_schema.TABLES it
       ON it.TABLE_SCHEMA = 'emart' AND it.TABLE_NAME = t.expected_table
ORDER BY status DESC, t.expected_table;

-- Are the three problem columns present?
SELECT 'users.created_at'     AS col,
       IF(COUNT(*) > 0, 'ok', '*** MISSING ***') AS status
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='users' AND COLUMN_NAME='created_at'
UNION ALL
SELECT 'cart.created_at',
       IF(COUNT(*) > 0, 'ok', '*** MISSING ***')
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='cart' AND COLUMN_NAME='created_at'
UNION ALL
SELECT 'cart_items.added_at',
       IF(COUNT(*) > 0, 'ok', '*** MISSING ***')
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='cart_items' AND COLUMN_NAME='added_at';


-- =====================================================================
-- STEP 2 — If any TABLE is missing, stop here.
--          Do the clean reset instead (see FIX-LOGIN-ERROR.md):
--              mysql -u root -p emart < emart_schema.sql
--              mysql -u root -p emart < emart_seed_data.sql
--
--          If only the three COLUMNS are missing and you want to keep your
--          existing data, run STEP 3 below.
-- =====================================================================


-- =====================================================================
-- STEP 3 — Targeted repair (keeps your data)
--
-- The key difference from what Hibernate tried: we supply
-- DEFAULT CURRENT_TIMESTAMP, so existing rows get a valid value instead of
-- the illegal zero date. That is exactly why Hibernate's version failed.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE cart
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE cart_items
    ADD COLUMN added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- These may already exist (they succeeded in your earlier run). If MySQL says
-- "Duplicate column name", that column is fine — ignore it and carry on.
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE cart  ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP;

-- Undo the enum Hibernate forced onto users.role, back to the designed VARCHAR.
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';


-- =====================================================================
-- STEP 4 — Verify. Every row should say 'ok'.
-- =====================================================================
SELECT 'users.created_at' AS col,
       IF(COUNT(*) > 0, 'ok', 'STILL MISSING') AS status
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='users' AND COLUMN_NAME='created_at'
UNION ALL
SELECT 'cart.created_at',
       IF(COUNT(*) > 0, 'ok', 'STILL MISSING')
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='cart' AND COLUMN_NAME='created_at'
UNION ALL
SELECT 'cart_items.added_at',
       IF(COUNT(*) > 0, 'ok', 'STILL MISSING')
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='emart' AND TABLE_NAME='cart_items' AND COLUMN_NAME='added_at';
