import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { WishlistProvider } from './context/WishlistContext';
import AppRoutes from './routes/AppRoutes';
import './styles/global.css';

/**
 * Provider order matters here: Auth wraps Cart/Wishlist because, once
 * real APIs exist, cart/wishlist actions may need to check
 * isAuthenticated (e.g. merge a guest cart into the user's account on
 * login) — so both inner contexts can safely call useAuth().
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <WishlistProvider>
            <AppRoutes />
          </WishlistProvider>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
