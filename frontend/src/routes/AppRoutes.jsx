import { Suspense, lazy } from 'react';
import { Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import AdminRoute from './AdminRoute';
import Loader from '../components/Loader/Loader';

// --- Customer pages (code-split: each route ships its own chunk) ---
const Home = lazy(() => import('../pages/Home/Home'));

// --- Stubs for pages not yet built. Replace each with the real
//     `lazy(() => import('../pages/X/X'))` as we build that page. ---
const ComingSoon = ({ label }) => (
  <div style={{ padding: '4rem', textAlign: 'center' }}>{label} — coming next</div>
);

export default function AppRoutes() {
  return (
    <Suspense fallback={<Loader label="Loading page…" />}>
      <Routes>
        {/* Public customer routes */}
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<ComingSoon label="Login" />} />
        <Route path="/register" element={<ComingSoon label="Register" />} />
        <Route path="/forgot-password" element={<ComingSoon label="Forgot Password" />} />
        <Route path="/categories/:id" element={<ComingSoon label="Category listing" />} />
        <Route path="/products" element={<ComingSoon label="Products" />} />
        <Route path="/products/:id" element={<ComingSoon label="Product Details" />} />
        <Route path="/search" element={<ComingSoon label="Search results" />} />
        <Route path="/cart" element={<ComingSoon label="Cart" />} />
        <Route path="/wishlist" element={<ComingSoon label="Wishlist" />} />

        {/* Protected customer routes — require login */}
        <Route element={<ProtectedRoute />}>
          <Route path="/checkout" element={<ComingSoon label="Checkout" />} />
          <Route path="/orders" element={<ComingSoon label="Orders" />} />
          <Route path="/orders/:id" element={<ComingSoon label="Order Details" />} />
          <Route path="/profile" element={<ComingSoon label="Profile" />} />
          <Route path="/loyalty-points" element={<ComingSoon label="Loyalty Points" />} />
          <Route path="/coupons" element={<ComingSoon label="Coupons" />} />
        </Route>

        {/* Admin routes — require login + ADMIN role */}
        <Route path="/admin" element={<AdminRoute />}>
          <Route path="dashboard" element={<ComingSoon label="Admin Dashboard" />} />
          <Route path="categories" element={<ComingSoon label="Category Management" />} />
          <Route path="products" element={<ComingSoon label="Product Management" />} />
          <Route path="banners" element={<ComingSoon label="Banner Management" />} />
          <Route path="users" element={<ComingSoon label="User Management" />} />
          <Route path="orders" element={<ComingSoon label="Order Management" />} />
          <Route path="discounts" element={<ComingSoon label="Discount Management" />} />
          <Route path="reports" element={<ComingSoon label="Reports" />} />
        </Route>

        <Route path="*" element={<ComingSoon label="404 — Page not found" />} />
      </Routes>
    </Suspense>
  );
}
