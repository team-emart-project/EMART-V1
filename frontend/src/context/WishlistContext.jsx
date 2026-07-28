import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const WishlistContext = createContext(null);
const LOCAL_WISHLIST_KEY = 'emart_wishlist';

/**
 * WishlistProvider
 * Same pattern as CartContext: local + localStorage now, ready for a
 * wishlistService swap-in once the backend module exists.
 */
export function WishlistProvider({ children }) {
  const [items, setItems] = useState(() => {
    try {
      const raw = localStorage.getItem(LOCAL_WISHLIST_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem(LOCAL_WISHLIST_KEY, JSON.stringify(items));
  }, [items]);

  const isWishlisted = (productId) => items.some((i) => i.productId === productId);

  const toggleWishlist = (product) => {
    // TODO once WishlistController exists: await wishlistService.toggle(product.id)
    setItems((prev) =>
      prev.some((i) => i.productId === product.id)
        ? prev.filter((i) => i.productId !== product.id)
        : [...prev, { productId: product.id, name: product.name, image: product.image, price: product.price }]
    );
  };

  const value = useMemo(
    () => ({ items, isWishlisted, toggleWishlist, totalCount: items.length }),
    [items]
  );

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

/**
 * useWishlist
 * @returns {{ items, isWishlisted, toggleWishlist, totalCount }}
 */
export function useWishlist() {
  const ctx = useContext(WishlistContext);
  if (!ctx) throw new Error('useWishlist must be used within a WishlistProvider');
  return ctx;
}
