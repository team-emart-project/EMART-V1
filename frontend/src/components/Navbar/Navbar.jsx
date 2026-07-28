import { useState } from 'react';
import { FaSearch, FaHeart, FaShoppingCart, FaUser, FaBars, FaTimes, FaThLarge, FaTags, FaTruck } from 'react-icons/fa';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { useWishlist } from '../../context/WishlistContext';
import LoyaltyPointsButton from '../LoyaltyPointsButton/LoyaltyPointsButton';
import { mockLoyaltyPoints } from '../../utils/mockData';
import './Navbar.css';

/**
 * Navbar
 * Reads live counts and auth state from context — no props needed from
 * the parent page. Every page that renders <Navbar /> gets the same
 * live cart/wishlist/auth state automatically.
 */
export default function Navbar() {
  const { isAuthenticated } = useAuth();
  const { totalCount: cartCount } = useCart();
  const { totalCount: wishlistCount } = useWishlist();
  const [query, setQuery] = useState('');
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (!query.trim()) return;
    window.location.href = `/search?q=${encodeURIComponent(query.trim())}`;
  };

  return (
    <header className="emart-navbar">
      <div className="container emart-navbar__inner">
        <a href="/" className="emart-navbar__logo font-display">
          E-Mart<span className="emart-navbar__logo-accent">Solution</span>
        </a>

        <form className="emart-navbar__search" onSubmit={handleSearchSubmit} role="search">
          <input
            type="search"
            placeholder="Search for products, brands and more"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            aria-label="Search products"
          />
          <button type="submit" aria-label="Search">
            <FaSearch />
          </button>
        </form>

        <nav className="emart-navbar__actions">
          <a href={isAuthenticated ? '/profile' : '/login'} className="emart-navbar__action">
            <FaUser />
            <span>{isAuthenticated ? 'Account' : 'Login'}</span>
          </a>
          <a href="/wishlist" className="emart-navbar__action">
            <span className="emart-navbar__icon-wrap">
              <FaHeart />
              {wishlistCount > 0 && <span className="emart-navbar__badge">{wishlistCount}</span>}
            </span>
            <span>Wishlist</span>
          </a>
          {/* TODO: replace mockLoyaltyPoints with user.loyaltyPoints from AuthContext once backend supplies it */}
          <LoyaltyPointsButton points={mockLoyaltyPoints} variant="compact" />
          <a href="/cart" className="emart-navbar__action">
            <span className="emart-navbar__icon-wrap">
              <FaShoppingCart />
              {cartCount > 0 && <span className="emart-navbar__badge">{cartCount}</span>}
            </span>
            <span>Cart</span>
          </a>
        </nav>

        <button
          type="button"
          className="emart-navbar__hamburger"
          onClick={() => setMobileOpen((v) => !v)}
          aria-label="Toggle menu"
          aria-expanded={mobileOpen}
        >
          {mobileOpen ? <FaTimes /> : <FaBars />}
        </button>
      </div>

      <div className="emart-navbar__quicklinks container">
        <a href="/categories"><FaThLarge /> Categories</a>
        <a href="#deal-of-day"><FaTags /> Today's Deals</a>
        <a href="/coupons"><FaTags /> Coupons</a>
        <a href="/orders"><FaTruck /> Track Order</a>
      </div>

      {mobileOpen && (
        <div className="emart-navbar__mobile-panel">
          <form className="emart-navbar__search emart-navbar__search--mobile" onSubmit={handleSearchSubmit}>
            <input
              type="search"
              placeholder="Search products"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              aria-label="Search products"
            />
            <button type="submit" aria-label="Search">
              <FaSearch />
            </button>
          </form>
          <a href={isAuthenticated ? '/profile' : '/login'}>{isAuthenticated ? 'Account' : 'Login'}</a>
          <a href="/wishlist">Wishlist{wishlistCount > 0 ? ` (${wishlistCount})` : ''}</a>
          <a href="/cart">Cart{cartCount > 0 ? ` (${cartCount})` : ''}</a>
          <a href="/loyalty-points">Loyalty Points ({mockLoyaltyPoints.toLocaleString('en-IN')})</a>
          <a href="/coupons">Coupons</a>
          <a href="/categories">Categories</a>
        </div>
      )}
    </header>
  );
}
