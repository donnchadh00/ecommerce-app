import { Link, NavLink, Route, Routes } from "react-router-dom";
import CatalogPage from "./features/catalog/CatalogPage";
import CartPage from "./features/cart/CartPage";
import CartBadge from "./features/cart/CartBadge";
import CheckoutPage from "./features/checkout/CheckoutPage";
import OrderConfirmationPage from "./features/checkout/OrderConfirmationPage";
import OrdersPage from "./features/orders/OrderPage";
import ProtectedRoute from "./features/auth/ProtectedRoute";
import LoginPage from "./features/auth/LoginPage";
import RegisterPage from "./features/auth/RegisterPage";
import AuthMenu from "./features/auth/AuthMenu";
import AccountPage from "./features/account/AccountPage";

const navClassName = ({ isActive }: { isActive: boolean }) =>
  `nav-link${isActive ? " nav-link-active" : ""}`;

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen text-slate-900">
      <header className="sticky top-0 z-30 border-b border-white/70 bg-slate-50/85 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between">
          <Link to="/" className="shrink-0">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-sm font-semibold text-white shadow-sm">
                EC
              </div>
              <div>
                <div className="text-base font-semibold tracking-tight text-slate-950">E-Commerce</div>
                <div className="text-xs text-slate-500">Microservices storefront</div>
              </div>
            </div>
          </Link>

          <nav className="flex flex-wrap items-center gap-2 lg:justify-end">
            <NavLink to="/" end className={navClassName}>
              Catalog
            </NavLink>
            <NavLink to="/cart" className={navClassName}>
              Cart <CartBadge />
            </NavLink>
            <NavLink to="/checkout" className={navClassName}>
              Checkout
            </NavLink>
            <AuthMenu />
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 pb-12 pt-6 sm:px-6 sm:pt-8">{children}</main>

      <footer className="mx-auto max-w-6xl px-4 pb-8 pt-2 text-sm text-slate-500 sm:px-6">
        <div className="border-t border-white/60 pt-6">
          Browse products, manage your cart, and track orders across the storefront.
        </div>
      </footer>
    </div>
  );
}

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<CatalogPage />} />
        <Route
          path="/cart"
          element={
            <ProtectedRoute>
              <CartPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/checkout"
          element={
            <ProtectedRoute>
              <CheckoutPage />
            </ProtectedRoute>
          }
        />
        <Route path="/order/:id" element={<OrderConfirmationPage />} />
        <Route
          path="/orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <AccountPage />
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Routes>
    </Layout>
  );
}

export default App
