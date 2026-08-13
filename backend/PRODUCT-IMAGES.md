# Product image gallery

## 1. Schema change — a new table

`product_master.prod_image_path` is a single `VARCHAR`. It cannot hold the 4–6
gallery images a detail page needs, so a product's images now live in their own
table. A product having many images is a genuine one-to-many, so this is a real
table rather than a delimited string crammed into one column.

```sql
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
```

**`prod_image_path` is kept**, still used as the listing thumbnail — nothing
that already reads it breaks.

This is the 14th table and the second addition beyond your teacher's original
sheet (the first was `users.reset_password_token`). Flag it to them if the
original design is not supposed to grow.

## 2. Seed data

**724 images across all 145 products — 4 to 6 each, exactly one primary.**
Verified programmatically, not by eye.

URLs point at `picsum.photos` with a deterministic seed
(`.../seed/emart-{prodId}-{n}/900/900`), so every product gets its own
consistent, visibly different set. Swap them for real files by updating
`image_url` — nothing in the code assumes that host.

```bat
cd D:\EMART-V1\backend
mysql -u root -p emart < emart_schema.sql
mysql -u root -p emart < emart_seed_data.sql
```

## 3. Backend

| File | Purpose |
|---|---|
| `entity/ProductImage.java` | maps the new table |
| `repository/ProductImageRepository.java` | by-product, ordered; plus a batch query |
| `dto/response/ProductImageResponse.java` | API shape |
| `ProductResponse.images` | populated on the **detail** endpoint only |

`GET /api/products/{id}` now returns:

```json
"images": [
  { "prodImageId": 1, "imageUrl": "…", "altText": "…",
    "displayOrder": 0, "isPrimary": true }
]
```

List endpoints deliberately do **not** include images — a 12-product grid only
needs one thumbnail each, and shipping 60+ URLs per page would be wasteful.
`findByProductIds` exists for when a listing does need them, so the grid can be
served in one query rather than N+1.

## 4. The gallery — `components/common/ProductGallery.jsx`

Myntra-style behaviour:

- **Thumbnail rail** — vertical beside the image on desktop, horizontal scroll
  strip below it on mobile.
- **Hover to preview, click to pin.** This needs two pieces of state, not one:
  `pinnedIndex` is what the user chose, `hoverIndex` is a temporary override
  that clears on mouse-leave. Tracking a single index leaves the wrong image
  showing once the pointer moves away.
- **Direction-aware sliding** — next slides in from the right, previous from
  the left, so the motion matches the gesture.
- **Swipe** — drag past 60px, or flick fast, to advance.
- **Keyboard** — arrow keys navigate, Escape closes the lightbox. Thumbnails
  respond to focus as well as hover, so keyboard users get the preview too.
- **Dots** on mobile, since the rail is a scroll strip there and gives no sense
  of position.
- **Lightbox** — click the main image for a full-screen view.
- **Graceful degradation** — one image hides the rail, arrows, dots and
  counter; no images at all falls back to `prod_image_path`.

Images are sorted by `displayOrder` in the component rather than trusting the
API to return them in order.

## 5. Layout fixes

Real problems found and fixed, not cosmetic churn:

| Problem | Fix |
|---|---|
| Product detail used `px-4 sm:px-6`; every other redesigned page uses `lg:px-8`. Content sat ~2rem left of the navbar on wide screens. | Added `lg:px-8` to the detail page **and** the eight other pages that had drifted |
| Gallery scrolled away while reading a long description | `lg:sticky lg:top-24` on the gallery column |
| Cards in a grid misaligned: a product **with** a member discount renders a strikethrough + badge (2 lines), one **without** renders 1 line, so the Add-to-cart buttons sat at different heights | `min-h-[2.75rem]` reserves the wrap line |
| Titles clamped to 2 lines but short titles collapsed to 1, shifting everything below | `min-h-[2.5rem]` on the title |
| Missing `prodShortDesc` shifted the price block up | renders a non-breaking space with `min-h-[1rem]` |
| Long product names could blow out the grid column | `min-w-0` on the details column |
| Fixed `gap-10` was cramped on tablet | `gap-8 lg:gap-12` |
| Action buttons overflowed on very narrow screens | `flex-wrap` + `min-w-[180px]` |
| Loading skeleton used different gutters/gap from the real layout, so the page jumped when data arrived | matched to the real values |

Everything uses the existing design tokens — `brand-*` colours, `rounded-2xl`,
the same border and shadow treatment as the rest of the app.
