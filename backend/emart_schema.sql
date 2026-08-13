-- =====================================================================
-- e-MART Database Schema (v1)
-- MySQL 8 / MariaDB compatible
--
-- category_master, product_master, prod_dtl_master, config_master are
-- built directly on the structure given in the teacher-provided file
-- "e-mart-DB-Documentation v1.0.xlsx" (sheet: DB Design). Field names
-- have been converted to valid snake_case SQL identifiers (e.g.
-- "Cat-id" -> cat_id, "Prod-short desc" -> prod_short_desc) but the
-- columns, types intent, and relationships are unchanged from that doc.
--
-- One addition beyond the teacher's sheet: product_master.prod_image_path,
-- needed because the BRD requires a photo to be attached to each product
-- during the Excel bulk-upload. Flag this to your instructor if the
-- original design should not be touched.
--
-- All other tables (users, address, emart_card, cart, cart_items,
-- wishlist, orders, order_details, payment) are new, designed to
-- support the rest of the BRD (registration, e-MART card / e-Points,
-- cart, checkout, invoice, payment).
--
-- Note: "order" is a reserved SQL keyword, so the orders table is named
-- `orders` (plural) instead of `order` to avoid having to quote it in
-- every query.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS emart
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE emart;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS order_details;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product_image;
DROP TABLE IF EXISTS prod_dtl_master;
DROP TABLE IF EXISTS config_master;
DROP TABLE IF EXISTS product_master;
DROP TABLE IF EXISTS category_master;
DROP TABLE IF EXISTS emart_card;
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- users — auth + registration profile (BRD "Registration" page)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id            INT AUTO_INCREMENT PRIMARY KEY,
    membership_no       VARCHAR(20)   NOT NULL UNIQUE,
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NULL,
    email               VARCHAR(150)  NOT NULL UNIQUE,

    -- NULLABLE since Google sign-in was added. A user who only ever signs in
    -- with Google has no password at all, and storing a fake or empty hash
    -- would be worse: it would look like a real credential to any future code
    -- that reads this column.
    password_hash       VARCHAR(255)  NULL,

    -- ---------------------------------------------------------------
    -- Google / OAuth sign-in
    -- ---------------------------------------------------------------
    --   LOCAL  = email + password only
    --   GOOGLE = Google only, no password
    --   BOTH   = registered with a password, later linked Google
    auth_provider       VARCHAR(10)   NOT NULL DEFAULT 'LOCAL',

    -- Google's immutable user id (the "sub" claim). UNIQUE so one Google
    -- account cannot be attached to two e-MART accounts. Matched on BEFORE
    -- email, because a Google account's email address can change but its sub
    -- never does.
    google_sub          VARCHAR(64)   NULL UNIQUE,

    profile_image_url   VARCHAR(500)  NULL,

    reset_password_token         VARCHAR(255) NULL,  -- added for Forgot/Reset Password flow, no refresh-token table used
    reset_password_token_expiry  TIMESTAMP    NULL,
    phone               VARCHAR(20)   NULL,
    dob                 DATE          NULL,
    gender              VARCHAR(10)   NULL,
    education           VARCHAR(100)  NULL,
    occupation          VARCHAR(100)  NULL,
    annual_income       DECIMAL(12,2) NULL,
    marketing_consent   TINYINT(1)    NOT NULL DEFAULT 0,
    role                VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',   -- CUSTOMER | ADMIN
    is_cardholder       TINYINT(1)    NOT NULL DEFAULT 0,
    is_active           TINYINT(1)    NOT NULL DEFAULT 1,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_auth_provider
        CHECK (auth_provider IN ('LOCAL','GOOGLE','BOTH')),

    -- A LOCAL or BOTH account must be able to log in with a password; a
    -- GOOGLE-only account must not pretend to have one. Enforced here so no
    -- future code path can create a passwordless account that still claims to
    -- accept passwords.
    CONSTRAINT chk_users_password_present
        CHECK ((auth_provider = 'GOOGLE' AND password_hash IS NULL)
            OR (auth_provider IN ('LOCAL','BOTH') AND password_hash IS NOT NULL)),

    -- Likewise a Google-linked account must carry the Google id it linked to.
    CONSTRAINT chk_users_google_sub_present
        CHECK ((auth_provider = 'LOCAL' AND google_sub IS NULL)
            OR (auth_provider IN ('GOOGLE','BOTH') AND google_sub IS NOT NULL))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- address — billing / shipping addresses per user
-- ---------------------------------------------------------------------
CREATE TABLE address (
    address_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT           NOT NULL,
    address_line1  VARCHAR(255)  NOT NULL,
    address_line2  VARCHAR(255)  NULL,
    city           VARCHAR(100)  NOT NULL,
    state          VARCHAR(100)  NOT NULL,
    zip_code       VARCHAR(20)   NOT NULL,
    country        VARCHAR(100)  NOT NULL DEFAULT 'India',
    address_type   VARCHAR(20)   NOT NULL DEFAULT 'SHIPPING',  -- BILLING | SHIPPING | BOTH
    is_default     TINYINT(1)    NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- emart_card — loyalty card + e-Points balance (BRD "e-MART Card Application")
-- ---------------------------------------------------------------------
CREATE TABLE emart_card (
    card_id             INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT           NOT NULL UNIQUE,
    card_number         VARCHAR(30)   NOT NULL UNIQUE,
    application_date    DATE          NOT NULL,
    approval_date       DATE          NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED
    points_balance      INT           NOT NULL DEFAULT 0,
    employment_details  VARCHAR(255)  NULL,
    bank_account_no     VARCHAR(30)   NULL,
    pan_number          VARCHAR(20)   NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emart_card_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- Tables below this line follow the teacher-provided "DB Design" sheet
-- =====================================================================

-- ---------------------------------------------------------------------
-- category_master  (source: Category Master)
-- ---------------------------------------------------------------------
CREATE TABLE category_master (
    catmaster_id     INT AUTO_INCREMENT PRIMARY KEY,
    cat_id           CHAR(3)       NOT NULL,   -- e.g. ELE = Electronics, HAP = Home App
    subcat_id        CHAR(3)       NULL,       -- e.g. TVS = TV, CAM = Camera
    cat_name         VARCHAR(255)  NOT NULL,
    cat_image_path   VARCHAR(255)  NULL,
    flag             TINYINT(1)    NOT NULL DEFAULT 0  -- indicates whether to route straight to Product page
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- product_master  (source: Product Master)
-- ---------------------------------------------------------------------
CREATE TABLE product_master (
    prod_id            INT AUTO_INCREMENT PRIMARY KEY,
    catmaster_id       INT            NOT NULL,
    prod_name          VARCHAR(255)   NOT NULL,
    prod_short_desc    VARCHAR(500)   NULL,
    prod_long_desc     TEXT           NULL,
    mrp_price          DECIMAL(10,2)  NOT NULL,   -- the normal/regular price, always present

    -- ---------------------------------------------------------------
    -- e-MART card (member) pricing. A product may offer any, all, or
    -- none of these three. NULL means "this option is not offered on
    -- this product", which is what the UI uses to decide whether to
    -- render the checkbox at all.
    --
    --   cardholder_price ....... Option 1  buy at the member cash price
    --   points_price ........... Option 2  buy with points only, cash = 0
    --   hybrid_cash_price
    --   + hybrid_points ........ Option 3  part cash, part points
    --
    -- Both hybrid columns must be set together or both left NULL; a
    -- half-filled hybrid offer is meaningless, so a CHECK enforces it.
    -- ---------------------------------------------------------------
    cardholder_price   DECIMAL(10,2)  NULL,
    points_price       INT            NULL,
    hybrid_cash_price  DECIMAL(10,2)  NULL,
    hybrid_points      INT            NULL,

    -- Retail attributes. Added because a Flipkart-style catalogue needs them;
    -- the original sheet had none of these.
    brand              VARCHAR(100)   NULL,
    stock_quantity     INT            NOT NULL DEFAULT 0,
    rating             DECIMAL(2,1)   NOT NULL DEFAULT 0.0,   -- 0.0 - 5.0
    rating_count       INT            NOT NULL DEFAULT 0,
    discount_percentage DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    prod_image_path    VARCHAR(255)   NULL,  -- addition: needed for Excel + photo upload requirement
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category
        FOREIGN KEY (catmaster_id) REFERENCES category_master(catmaster_id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_hybrid_pair CHECK (
        (hybrid_cash_price IS NULL AND hybrid_points IS NULL)
     OR (hybrid_cash_price IS NOT NULL AND hybrid_points IS NOT NULL)
    )
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- product_image — gallery images for a product (one-to-many)
--
-- ADDITION beyond the teacher's original sheet. product_master has a single
-- prod_image_path column, which cannot hold the 4-6 gallery images a product
-- detail page needs. That is a genuine one-to-many, so it gets its own table
-- rather than a delimited string stuffed into one column.
--
-- product_master.prod_image_path is KEPT as the thumbnail used in listings,
-- so nothing that already reads it breaks.
-- ---------------------------------------------------------------------
CREATE TABLE product_image (
    prod_image_id  INT AUTO_INCREMENT PRIMARY KEY,
    prod_id        INT           NOT NULL,
    image_url      VARCHAR(500)  NOT NULL,
    alt_text       VARCHAR(255)  NULL,
    display_order  INT           NOT NULL DEFAULT 0,   -- 0 = shown first
    is_primary     TINYINT(1)    NOT NULL DEFAULT 0,   -- exactly one per product
    CONSTRAINT fk_prodimg_product
        FOREIGN KEY (prod_id) REFERENCES product_master(prod_id) ON DELETE CASCADE,
    INDEX idx_prodimg_product_order (prod_id, display_order)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- config_master  (source: Config Master)
-- ---------------------------------------------------------------------
CREATE TABLE config_master (
    config_id    INT AUTO_INCREMENT PRIMARY KEY,
    config_name  VARCHAR(100) NOT NULL   -- e.g. Color, Size
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- prod_dtl_master  (source: Prod-Dtl Master)
-- ---------------------------------------------------------------------
CREATE TABLE prod_dtl_master (
    prod_dtl_id  INT AUTO_INCREMENT PRIMARY KEY,
    prod_id      INT           NOT NULL,
    config_id    INT           NOT NULL,
    config_dtls  VARCHAR(255)  NOT NULL,  -- e.g. 'Red', 'XL'
    CONSTRAINT fk_proddtl_product
        FOREIGN KEY (prod_id) REFERENCES product_master(prod_id) ON DELETE CASCADE,
    CONSTRAINT fk_proddtl_config
        FOREIGN KEY (config_id) REFERENCES config_master(config_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- Cart / Wishlist / Orders / Payment
-- =====================================================================

-- ---------------------------------------------------------------------
-- cart — one active cart per user
-- ---------------------------------------------------------------------
CREATE TABLE cart (
    cart_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CONVERTED
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- cart_items — line items in a cart
-- ---------------------------------------------------------------------
CREATE TABLE cart_items (
    cart_item_id    INT AUTO_INCREMENT PRIMARY KEY,
    cart_id         INT         NOT NULL,
    prod_id         INT         NOT NULL,
    quantity        INT         NOT NULL DEFAULT 1,

    -- Which of the four price options the shopper picked on the product
    -- card. Stored per line, not per cart, because a shopper can pay
    -- points for one item and cash for another in the same order.
    --   REGULAR | MEMBER | POINTS | HYBRID
    price_option    VARCHAR(10) NOT NULL DEFAULT 'REGULAR',
    points_used     INT         NOT NULL DEFAULT 0,  -- derived from price_option, stored for auditing
    added_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cartitem_cart
        FOREIGN KEY (cart_id) REFERENCES cart(cart_id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_product
        FOREIGN KEY (prod_id) REFERENCES product_master(prod_id) ON DELETE RESTRICT,
    CONSTRAINT chk_cartitem_price_option
        CHECK (price_option IN ('REGULAR','MEMBER','POINTS','HYBRID'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- wishlist — saved-for-later products per user
-- ---------------------------------------------------------------------
CREATE TABLE wishlist (
    wishlist_id  INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT        NOT NULL,
    prod_id      INT        NOT NULL,
    added_at     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_product
        FOREIGN KEY (prod_id) REFERENCES product_master(prod_id) ON DELETE RESTRICT,
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, prod_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- orders  (named "orders" not "order" — ORDER is a reserved SQL keyword)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    order_id              INT AUTO_INCREMENT PRIMARY KEY,
    order_no              VARCHAR(30)    NOT NULL UNIQUE,
    user_id               INT            NOT NULL,
    shipping_address_id   INT            NOT NULL,
    billing_address_id    INT            NOT NULL,
    order_date            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal_amount       DECIMAL(12,2)  NOT NULL,
    -- No tax column. This project does not model tax at all: the price the
    -- shopper sees on the product card is the price they are charged.
    total_amount          DECIMAL(12,2)  NOT NULL,
    points_redeemed       INT            NOT NULL DEFAULT 0,
    points_earned         INT            NOT NULL DEFAULT 0,
    payment_status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',  -- PENDING | PAID | FAILED
    order_status           VARCHAR(20)    NOT NULL DEFAULT 'PLACED',   -- PLACED | PAID | CANCELLED | SHIPPED | DELIVERED
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_shipaddr
        FOREIGN KEY (shipping_address_id) REFERENCES address(address_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_billaddr
        FOREIGN KEY (billing_address_id) REFERENCES address(address_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- order_details — line items on a placed order (invoice detail)
-- ---------------------------------------------------------------------
CREATE TABLE order_details (
    order_dtl_id        INT AUTO_INCREMENT PRIMARY KEY,
    order_id             INT            NOT NULL,
    prod_id              INT            NOT NULL,
    prod_name_snapshot   VARCHAR(255)   NOT NULL,  -- captured at order time, in case product changes later
    quantity             INT            NOT NULL,
    mrp_price            DECIMAL(10,2)  NOT NULL,
    cardholder_price     DECIMAL(10,2)  NULL,      -- snapshot; NULL if the product had no member offer
    price_option         VARCHAR(10)    NOT NULL DEFAULT 'REGULAR',
    price_charged        DECIMAL(10,2)  NOT NULL,  -- cash actually applied to this line (0 for POINTS)
    points_redeemed      INT            NOT NULL DEFAULT 0,
    CONSTRAINT fk_orderdtl_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_orderdtl_product
        FOREIGN KEY (prod_id) REFERENCES product_master(prod_id) ON DELETE RESTRICT,
    CONSTRAINT chk_orderdtl_price_option
        CHECK (price_option IN ('REGULAR','MEMBER','POINTS','HYBRID'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- payment — payment attempt/result per order
-- ---------------------------------------------------------------------
CREATE TABLE payment (
    payment_id        INT AUTO_INCREMENT PRIMARY KEY,
    order_id          INT           NOT NULL,
    payment_method    VARCHAR(30)   NOT NULL DEFAULT 'CARD',
    card_last4        VARCHAR(4)    NULL,
    amount            DECIMAL(12,2) NOT NULL,
    status            VARCHAR(20)   NOT NULL,  -- SUCCESS | FAILED
    transaction_ref   VARCHAR(100)  NULL,
    transaction_date  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB;
