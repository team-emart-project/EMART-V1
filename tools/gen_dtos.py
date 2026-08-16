"""Regenerates the pricing-related response DTOs from a single field list."""
from pathlib import Path
from gen_pojo import build_pojo

ROOT = Path(__file__).resolve().parent.parent
DTO = ROOT / "backend/src/main/java/com/example/demo/dto/response"
PKG = "com.example.demo.dto.response"

# ---------------------------------------------------------------- Product
product = build_pojo(
    PKG, "ProductResponse",
    imports=[
        "com.fasterxml.jackson.annotation.JsonInclude",
        "java.math.BigDecimal",
        "java.util.List",
    ],
    class_javadoc='''/**
 * What the API returns for one product.
 *
 * PRICE VISIBILITY
 * ----------------
 * mrpPrice is the normal price and is always sent. The three e-MART card
 * offers are only populated for an APPROVED cardholder; for everyone else the
 * service leaves them null and @JsonInclude(NON_NULL) drops them from the JSON
 * entirely. A non-member therefore cannot read member pricing out of the
 * payload — the numbers are not merely hidden by CSS, they are not sent.
 *
 * A null offer field ALSO means "this product does not carry that offer", which
 * is what the product card reads to decide whether to render each checkbox.
 * Both meanings collapse to the same UI behaviour (no checkbox), so one null
 * check covers them.
 */''',
    class_annotations=["@JsonInclude(JsonInclude.Include.NON_NULL)"],
    fields=[
        ("Integer", "prodId", None),
        ("String", "prodName", None),
        ("String", "prodShortDesc", None),
        ("String", "prodLongDesc",
         "/** Detail page only; omitted from list responses to keep grids small. */"),
        ("BigDecimal", "mrpPrice",
         "/** The normal price. Always present, always the top line on the card. */"),
        ("BigDecimal", "cardholderPrice",
         "/** Option 1 — member cash price. Null = not offered / caller is not a member. */"),
        ("BigDecimal", "cardholderSaving",
         "/** mrpPrice - cardholderPrice, precomputed so the UI never does money maths. */"),
        ("Integer", "pointsPrice",
         "/** Option 2 — e-Points to buy one unit outright; cash charged is 0. */"),
        ("BigDecimal", "hybridCashPrice", "/** Option 3 — the cash half of the combo. */"),
        ("Integer", "hybridPoints", "/** Option 3 — the points half of the combo. */"),
        ("String", "brand", None),
        ("Integer", "stockQuantity", None),
        ("Boolean", "inStock", "/** stockQuantity > 0, precomputed for the UI. */"),
        ("BigDecimal", "rating", None),
        ("Integer", "ratingCount", None),
        ("BigDecimal", "discountPercentage", None),
        ("String", "prodImagePath", "/** Primary thumbnail. */"),
        ("Integer", "catmasterId", None),
        ("String", "categoryName", None),
        ("List<ProductVariantResponse>", "variants",
         "/** Detail page only. */"),
        ("List<ProductImageResponse>", "images",
         "/** Detail page only — a 12-product grid needs 12 thumbnails, not 60 URLs. */"),
    ],
)
(DTO / "ProductResponse.java").write_text(product, encoding="utf-8")

# --------------------------------------------------------------- Cart line
cart_item = build_pojo(
    PKG, "CartItemResponse",
    imports=[
        "com.example.demo.enums.PriceOption",
        "com.fasterxml.jackson.annotation.JsonInclude",
        "java.math.BigDecimal",
    ],
    class_javadoc='''/**
 * One line in the cart, with its price already resolved.
 *
 * unitPriceApplied is the single number the UI should display and the only one
 * that feeds the total. It is derived server-side from priceOption, so the
 * front end never has to know the pricing rules.
 */''',
    class_annotations=["@JsonInclude(JsonInclude.Include.NON_NULL)"],
    fields=[
        ("Integer", "cartItemId", None),
        ("Integer", "prodId", None),
        ("String", "prodName", None),
        ("String", "prodImagePath", None),
        ("BigDecimal", "mrpPrice", "/** Normal price, shown struck through when a cheaper option is active. */"),

        # The other offers are echoed back so the CART can render a switcher.
        # Without them a shopper who ticked the wrong box would have to delete
        # the line and start again from the product page.
        ("BigDecimal", "cardholderPrice", "/** Option 1. Null if not offered, or caller is not a member. */"),
        ("Integer", "pointsPrice", "/** Option 2. Null if not offered, or caller is not a member. */"),
        ("BigDecimal", "hybridCashPrice", "/** Option 3, cash half. */"),
        ("Integer", "hybridPoints", "/** Option 3, points half. */"),

        ("PriceOption", "priceOption", "/** REGULAR | MEMBER | POINTS | HYBRID — what the shopper ticked. */"),
        ("BigDecimal", "unitPriceApplied", "/** Cash per unit under the chosen option. 0 for POINTS. */"),
        ("Integer", "unitPointsApplied", "/** e-Points per unit under the chosen option. 0 for REGULAR/MEMBER. */"),
        ("Integer", "quantity", None),
        ("BigDecimal", "lineTotal", "/** unitPriceApplied * quantity. */"),
        ("BigDecimal", "lineSavings", "/** (mrpPrice - unitPriceApplied) * quantity. */"),
        ("Integer", "pointsUsed", "/** unitPointsApplied * quantity. */"),
    ],
)
(DTO / "CartItemResponse.java").write_text(cart_item, encoding="utf-8")

print("generated ProductResponse.java, CartItemResponse.java")
