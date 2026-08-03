import { useEffect, useState } from 'react';
import Navbar from '../../components/Navbar/Navbar';
import Footer from '../../components/Footer/Footer';
import BannerSlider from '../../components/BannerSlider/BannerSlider';
import CategoryCard from '../../components/CategoryCard/CategoryCard';
import ProductCard from '../../components/ProductCard/ProductCard';
import Loader from '../../components/Loader/Loader';
import LoyaltyPointsButton from '../../components/LoyaltyPointsButton/LoyaltyPointsButton';
import useCountdown from '../../hooks/useCountdown';
import { useCart } from '../../context/CartContext';
import { useWishlist } from '../../context/WishlistContext';
import { mockBanners, mockCategories, mockProducts, mockLoyaltyPoints, dealEndsAt } from '../../utils/mockData';
import './Home.css';

/**
 * Home
 *
 * Currently reads from utils/mockData.js so the page has real content to
 * render. Swap the three useEffect blocks below for calls to:
 *   bannerService.getActiveBanners()
 *   categoryService.getAllCategories()
 *   productService.getFeaturedProducts()
 * as soon as those controllers are shared — the loading/error state
 * pattern is already in place, so the swap is a one-line change per block.
 */
export default function Home() {
  const [banners, setBanners] = useState([]);
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const countdown = useCountdown(dealEndsAt);
  const { addItem } = useCart();
  const { isWishlisted, toggleWishlist } = useWishlist();

  useEffect(() => {
    let cancelled = false;

    async function loadHomeData() {
      setLoading(true);
      setError(null);
      try {
        // --- Replace this block with real Axios calls once APIs exist ---
        await new Promise((resolve) => setTimeout(resolve, 700));
        if (cancelled) return;
        setBanners(mockBanners);
        setCategories(mockCategories);
        setProducts(mockProducts);
        // -----------------------------------------------------------------
      } catch (err) {
        if (!cancelled) setError('We could not load the homepage right now. Please refresh.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadHomeData();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <Navbar />

      <div className="emart-announcement-bar">
        Free delivery on orders above ₹499 &nbsp;•&nbsp; Easy 7-day returns &nbsp;•&nbsp; Pay on delivery available
      </div>

      <main className="container emart-home">
        {error && (
          <div className="emart-error-banner" role="alert">
            {error}
          </div>
        )}

        <section className="emart-home__section" aria-label="Promotions">
          {loading ? (
            <div className="emart-banner-skeleton skeleton" />
          ) : (
            <BannerSlider slides={banners} />
          )}
        </section>

        <section className="emart-home__section" aria-labelledby="shop-by-category">
          <h2 id="shop-by-category" className="emart-home__heading">Shop by category</h2>
          {loading ? (
            <div className="scroll-rail">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="skeleton emart-category-skeleton" />
              ))}
            </div>
          ) : (
            <div className="scroll-rail">
              {categories.map((cat) => (
                <CategoryCard key={cat.id} category={cat} />
              ))}
            </div>
          )}
        </section>

        <section className="emart-home__section" aria-label="Loyalty points">
          {/* TODO: replace mockLoyaltyPoints with user.loyaltyPoints from AuthContext once backend supplies it */}
          <LoyaltyPointsButton points={mockLoyaltyPoints} variant="large" />
        </section>

        <section className="emart-deal-strip" aria-labelledby="deal-of-day">
          <div className="emart-deal-strip__info">
            <h2 id="deal-of-day">Deal of the day</h2>
            <p>Grab it before the clock runs out</p>
          </div>
          <div className="emart-deal-strip__countdown font-mono" aria-live="polite">
            <span>{countdown.hours}</span>:<span>{countdown.minutes}</span>:<span>{countdown.seconds}</span>
          </div>
        </section>

        <section className="emart-home__section" aria-labelledby="featured-products">
          <h2 id="featured-products" className="emart-home__heading">Featured products</h2>
          {loading ? (
            <Loader variant="product-grid" count={6} label="Loading featured products" />
          ) : products.length === 0 ? (
            <p className="emart-empty-note">No products to show right now — check back soon.</p>
          ) : (
            <div className="row g-4">
              {products.map((product) => (
                <div className="col-6 col-md-4 col-lg-2" key={product.id}>
                  <ProductCard
                    product={product}
                    isWishlisted={isWishlisted(product.id)}
                    onToggleWishlist={() => toggleWishlist(product)}
                    onAddToCart={() => addItem(product)}
                  />
                </div>
              ))}
            </div>
          )}
        </section>
      </main>

      <Footer />
    </>
  );
}
