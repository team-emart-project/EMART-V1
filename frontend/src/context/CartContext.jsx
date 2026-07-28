import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const CartContext = createContext(null);
const LOCAL_CART_KEY = 'emart_cart';

/**
 * CartProvider
 * Runs entirely client-side for now (persisted to localStorage so a
 * refresh doesn't lose the cart). Once your CartController exists, each
 * action below gets a matching cartService call added alongside the
 * local state update — the local update stays as an optimistic UI update,
 * the API call becomes the source of truth. No component using useCart()
 * will need to change when that happens.
 */
export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    try {
      const raw = localStorage.getItem(LOCAL_CART_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem(LOCAL_CART_KEY, JSON.stringify(items));
  }, [items]);

  const addItem = (product, qty = 1) => {
    // TODO once CartController exists: await cartService.addItem(product.id, qty)
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === product.id);
      if (existing) {
        return prev.map((i) =>
          i.productId === product.id ? { ...i, qty: i.qty + qty } : i
        );
      }
      return [
        ...prev,
        {
          productId: product.id,
          name: product.name,
          image: product.image,
          price: product.price,
          qty,
        },
      ];
    });
  };

  const removeItem = (productId) => {
    // TODO once CartController exists: await cartService.removeItem(productId)
    setItems((prev) => prev.filter((i) => i.productId !== productId));
  };

  const updateQty = (productId, qty) => {
    // TODO once CartController exists: await cartService.updateQty(productId, qty)
    setItems((prev) =>
      qty <= 0
        ? prev.filter((i) => i.productId !== productId)
        : prev.map((i) => (i.productId === productId ? { ...i, qty } : i))
    );
  };

  const clearCart = () => setItems([]);

  const totalCount = items.reduce((sum, i) => sum + i.qty, 0);
  const totalAmount = items.reduce((sum, i) => sum + i.qty * i.price, 0);

  const value = useMemo(
    () => ({ items, addItem, removeItem, updateQty, clearCart, totalCount, totalAmount }),
    [items]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

/**
 * useCart
 * @returns {{ items, addItem, removeItem, updateQty, clearCart, totalCount, totalAmount }}
 */
export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within a CartProvider');
  return ctx;
}
