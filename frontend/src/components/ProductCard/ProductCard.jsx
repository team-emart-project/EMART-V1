import { useState } from 'react';
import { FaHeart, FaRegHeart, FaShoppingCart } from 'react-icons/fa';
import Rating from '../Rating/Rating';
import './ProductCard.css';

/**
 * ProductCard
 * Presentational only — receives data + handlers as props so it can be
 * reused on Home, Category listing, Search results, and Wishlist pages
 * without caring where the data came from.
 *
 * @param {object} product - { id, name, brand, image, price, mrp, rating, ratingCount, inStock }
 * @param {boolean} [isWishlisted=false]
 * @param {(id:number)=>void} [onToggleWishlist]
 * @param {(id:number)=>void} [onAddToCart]
 */
export default function ProductCard({
  product,
  isWishlisted = false,
  onToggleWishlist,
  onAddToCart,
}) {
  const [wishActive, setWishActive] = useState(isWishlisted);
  const discountPct = Math.round(((product.mrp - product.price) / product.mrp) * 100);

  const handleWishlist = (e) => {
    e.preventDefault();
    setWishActive((prev) => !prev);
    onToggleWishlist?.(product.id);
  };

  const handleAddToCart = (e) => {
    e.preventDefault();
    if (!product.inStock) return;
    onAddToCart?.(product.id);
  };

  return (
    <a href={`/products/${product.id}`} className="emart-product-card" aria-label={product.name}>
      <div className="emart-product-card__media">
        {discountPct > 0 && (
          <span className="emart-ticket-badge">
            <span className="font-mono">{discountPct}%</span>
            <span className="emart-ticket-badge__off">OFF</span>
          </span>
        )}

        <button
          type="button"
          className={`emart-wish-btn ${wishActive ? 'emart-wish-btn--active' : ''}`}
          onClick={handleWishlist}
          aria-pressed={wishActive}
          aria-label={wishActive ? 'Remove from wishlist' : 'Add to wishlist'}
        >
          {wishActive ? <FaHeart /> : <FaRegHeart />}
        </button>

        <img src={product.image} alt={product.name} loading="lazy" />

        {!product.inStock && (
          <div className="emart-product-card__oos-overlay">Out of stock</div>
        )}
      </div>

      <div className="emart-product-card__body">
        <p className="emart-product-card__brand">{product.brand}</p>
        <p className="emart-product-card__name" title={product.name}>
          {product.name}
        </p>

        <Rating value={product.rating} count={product.ratingCount} />

        <div className="emart-product-card__price-row">
          <span className="emart-product-card__price font-mono">
            ₹{product.price.toLocaleString('en-IN')}
          </span>
          {product.mrp > product.price && (
            <span className="emart-product-card__mrp font-mono">
              ₹{product.mrp.toLocaleString('en-IN')}
            </span>
          )}
        </div>

        <button
          type="button"
          className="emart-add-to-cart-btn"
          onClick={handleAddToCart}
          disabled={!product.inStock}
        >
          <FaShoppingCart />
          {product.inStock ? 'Add to cart' : 'Notify me'}
        </button>
      </div>
    </a>
  );
}
