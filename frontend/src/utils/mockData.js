/**
 * TEMPORARY MOCK DATA
 * -----------------------------------------------------------------
 * This file exists ONLY so the UI has realistic data to render before
 * the real Spring Boot endpoints are wired in.
 *
 * Once you share the controllers, this whole file gets deleted and
 * replaced by live calls through:
 *   - services/bannerService.js   -> GET /api/banners
 *   - services/categoryService.js -> GET /api/categories
 *   - services/productService.js  -> GET /api/products/featured
 *
 * Keep the SHAPE of these objects identical to what your DTOs return
 * (or tell me your DTO field names and I'll align the shape now).
 */

export const mockBanners = [
  {
    id: 1,
    title: 'Big Billion Style Sale',
    subtitle: 'Up to 70% off on fashion & footwear',
    ctaLabel: 'Shop the sale',
    image: 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1600&q=80',
    theme: 'amber',
  },
  {
    id: 2,
    title: 'Electronics Days',
    subtitle: 'Top brands. Bank offers up to ₹2,000 off',
    ctaLabel: 'Explore deals',
    image: 'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=1600&q=80',
    theme: 'teal',
  },
  {
    id: 3,
    title: 'New Season Arrivals',
    subtitle: 'Fresh drops across home & living',
    ctaLabel: 'Discover now',
    image: 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=1600&q=80',
    theme: 'navy',
  },
];

export const mockCategories = [
  { id: 1, name: 'Fashion', icon: '👗' },
  { id: 2, name: 'Electronics', icon: '📱' },
  { id: 3, name: 'Footwear', icon: '👟' },
  { id: 4, name: 'Home & Living', icon: '🛋️' },
  { id: 5, name: 'Beauty', icon: '💄' },
  { id: 6, name: 'Grocery', icon: '🛒' },
  { id: 7, name: 'Toys', icon: '🧸' },
  { id: 8, name: 'Sports', icon: '🏸' },
  { id: 9, name: 'Books', icon: '📚' },
  { id: 10, name: 'Jewellery', icon: '💍' },
];

export const mockProducts = [
  {
    id: 101,
    name: 'Wireless Over-Ear Headphones',
    brand: 'SoundCore',
    image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&q=80',
    price: 2499,
    mrp: 4999,
    rating: 4.3,
    ratingCount: 1284,
    inStock: true,
  },
  {
    id: 102,
    name: "Men's Slim Fit Casual Shirt",
    brand: 'Urban Threads',
    image: 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&q=80',
    price: 899,
    mrp: 1799,
    rating: 4.1,
    ratingCount: 532,
    inStock: true,
  },
  {
    id: 103,
    name: 'Running Shoes - Lightweight',
    brand: 'Stride',
    image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80',
    price: 1999,
    mrp: 3499,
    rating: 4.5,
    ratingCount: 2210,
    inStock: true,
  },
  {
    id: 104,
    name: 'Smart Watch Series 5',
    brand: 'PulseTech',
    image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&q=80',
    price: 3499,
    mrp: 5999,
    rating: 4.2,
    ratingCount: 891,
    inStock: false,
  },
  {
    id: 105,
    name: 'Ceramic Coffee Mug Set (4pc)',
    brand: 'HomeCraft',
    image: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=600&q=80',
    price: 599,
    mrp: 999,
    rating: 4.6,
    ratingCount: 340,
    inStock: true,
  },
  {
    id: 106,
    name: "Women's Tote Handbag",
    brand: 'Vera Luxe',
    image: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=600&q=80',
    price: 1299,
    mrp: 2599,
    rating: 4.0,
    ratingCount: 175,
    inStock: true,
  },
];

/** Deal-of-the-day ends this many ms from now (demo only) */
export const dealEndsAt = Date.now() + 1000 * 60 * 60 * 6; // 6 hours from load

export const mockCoupons = [
  {
    id: 1,
    code: 'WELCOME200',
    discountLabel: '₹200 OFF',
    description: 'On your first order above ₹999',
    expiresOn: '31 Aug 2026',
  },
  {
    id: 2,
    code: 'FASHION50',
    discountLabel: '50% OFF',
    description: 'On fashion & footwear, up to ₹500',
    expiresOn: '15 Aug 2026',
  },
  {
    id: 3,
    code: 'ELEC10',
    discountLabel: '10% OFF',
    description: 'On electronics & accessories',
    expiresOn: '10 Aug 2026',
  },
];

/** Will come from the logged-in user's profile once the backend supplies it */
export const mockLoyaltyPoints = 1250;

