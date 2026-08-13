import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from './ProtectedRoute';
import PublicOnlyRoute from './PublicOnlyRoute';
import PageLoader from '@/components/common/PageLoader';

/**
 * REQUIREMENT 2 — Lazy loading.
 *
 * Every page is loaded with React.lazy(), so Vite emits a separate JS chunk per
 * route and the browser only downloads the code for the page being visited.
 * The initial bundle stays small: someone landing on the home page never
 * downloads the checkout or payment code.
 *
 * <Suspense> supplies the fallback shown while a chunk is in flight.
 */

// Public
const HomePage           = lazy(() => import('@/pages/home/HomePage'));
const ProductListPage    = lazy(() => import('@/pages/catalog/ProductListPage'));
const ProductDetailPage  = lazy(() => import('@/pages/catalog/ProductDetailPage'));
const CategoryPage       = lazy(() => import('@/pages/catalog/CategoryPage'));
const NotFoundPage       = lazy(() => import('@/pages/NotFoundPage'));

// Auth (only for logged-OUT users)
const LoginPage          = lazy(() => import('@/pages/auth/LoginPage'));
const RegisterPage       = lazy(() => import('@/pages/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage'));
const ResetPasswordPage  = lazy(() => import('@/pages/auth/ResetPasswordPage'));

// Protected
const CartPage           = lazy(() => import('@/pages/cart/CartPage'));
const CheckoutPage       = lazy(() => import('@/pages/cart/CheckoutPage'));
const PaymentPage        = lazy(() => import('@/pages/cart/PaymentPage'));
const WishlistPage       = lazy(() => import('@/pages/wishlist/WishlistPage'));
const ProfilePage        = lazy(() => import('@/pages/user/ProfilePage'));
const AddressesPage      = lazy(() => import('@/pages/user/AddressesPage'));
const EmartCardPage      = lazy(() => import('@/pages/user/EmartCardPage'));
const OrdersPage         = lazy(() => import('@/pages/user/OrdersPage'));
const OrderDetailPage    = lazy(() => import('@/pages/user/OrderDetailPage'));

export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route element={<MainLayout />}>

          {/* ---- public ---- */}
          <Route path="/" element={<HomePage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/:prodId" element={<ProductDetailPage />} />
          <Route path="/categories/:catmasterId" element={<CategoryPage />} />

          {/* ---- logged-out only ---- */}
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
          </Route>

          {/* ---- requires login: ONE guard for the whole group ---- */}
          <Route element={<ProtectedRoute />}>
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/payment/:orderId" element={<PaymentPage />} />
            <Route path="/wishlist" element={<WishlistPage />} />
            <Route path="/account/profile" element={<ProfilePage />} />
            <Route path="/account/addresses" element={<AddressesPage />} />
            <Route path="/account/card" element={<EmartCardPage />} />
            <Route path="/account/orders" element={<OrdersPage />} />
            <Route path="/account/orders/:orderId" element={<OrderDetailPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
