"""Regenerates the order response DTOs: no tax, price option per line."""
from pathlib import Path
from gen_pojo import build_pojo

ROOT = Path(__file__).resolve().parent.parent
DTO = ROOT / "backend/src/main/java/com/example/demo/dto/response"
PKG = "com.example.demo.dto.response"

order_detail = build_pojo(
    PKG, "OrderDetailResponse",
    imports=[
        "com.example.demo.enums.PriceOption",
        "com.fasterxml.jackson.annotation.JsonInclude",
        "java.math.BigDecimal",
    ],
    class_javadoc='''/**
 * One line of a placed order. Every figure here is a SNAPSHOT taken when the
 * order was placed, so a two-year-old invoice still shows what the customer
 * actually saw and paid even if the catalogue has since changed.
 */''',
    class_annotations=["@JsonInclude(JsonInclude.Include.NON_NULL)"],
    fields=[
        ("Integer", "orderDtlId", None),
        ("Integer", "prodId", None),
        ("String", "prodName", "/** Snapshot of the name at order time, not the live one. */"),
        ("Integer", "quantity", None),
        ("BigDecimal", "mrpPrice", None),
        ("BigDecimal", "cardholderPrice", "/** Null if the product carried no member offer. */"),
        ("PriceOption", "priceOption", "/** Which option this line was bought under. */"),
        ("BigDecimal", "priceCharged", "/** Cash per unit. Zero when the line was paid in points. */"),
        ("BigDecimal", "lineTotal", "/** priceCharged * quantity. */"),
        ("BigDecimal", "lineSavings", "/** (mrpPrice - priceCharged) * quantity. */"),
        ("Integer", "pointsRedeemed", "/** e-Points spent on this line in total. */"),
    ],
)
(DTO / "OrderDetailResponse.java").write_text(order_detail, encoding="utf-8")

order = build_pojo(
    PKG, "OrderResponse",
    imports=[
        "com.fasterxml.jackson.annotation.JsonInclude",
        "java.math.BigDecimal",
        "java.time.LocalDateTime",
        "java.util.List",
    ],
    class_javadoc='''/**
 * A placed order, or a checkout PREVIEW of one.
 *
 * NO TAX. This project charges exactly the price shown on the product card:
 * totalAmount equals subtotalAmount. There is deliberately no tax field here,
 * on the order table, or on the invoice.
 *
 * e-Points are not modelled as a discount either. If a line was bought with
 * points, its cash price is simply lower (or zero) — the points are part of the
 * price, not something subtracted afterwards. pointsRedeemed records how many
 * were spent so the invoice can show it.
 */''',
    class_annotations=["@JsonInclude(JsonInclude.Include.NON_NULL)"],
    fields=[
        ("Integer", "orderId", "/** Null on a preview — nothing was saved. */"),
        ("String", "orderNo", "/** Null on a preview. */"),
        ("LocalDateTime", "orderDate", None),
        ("String", "customerName", None),
        ("String", "membershipNo", None),
        ("Boolean", "cardholder", None),
        ("AddressResponse", "shippingAddress", None),
        ("AddressResponse", "billingAddress", None),
        ("List<OrderDetailResponse>", "items", None),
        ("BigDecimal", "subtotalMrp", "/** What the basket would cost at normal prices. */"),
        ("BigDecimal", "subtotalAmount", "/** Cash actually payable across all lines. */"),
        ("BigDecimal", "totalSavings", "/** subtotalMrp - subtotalAmount. */"),
        ("BigDecimal", "totalAmount", "/** Equals subtotalAmount. Kept as its own field so the UI and the payment tamper-check have one obvious number to read. */"),
        ("Integer", "pointsRedeemed", None),
        ("Integer", "pointsEarned", None),
        ("Integer", "pointsBalanceAfter", "/** Only populated by the payment response. */"),
        ("String", "paymentStatus", None),
        ("String", "orderStatus", None),
        ("Boolean", "preview", "/** True when this was produced by /checkout-preview. */"),
    ],
)
(DTO / "OrderResponse.java").write_text(order, encoding="utf-8")

print("generated OrderDetailResponse.java, OrderResponse.java")
