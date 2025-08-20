import { Link, Route, Routes } from 'react-router-dom';
import CatalogPage from "./features/catalog/CatalogPage";
import CartPage from './features/cart/CartPage';
import CartBadge from './features/cart/CartBadge';
import CheckoutPage from './features/checkout/CheckoutPage';
import OrderConfirmationPage from './features/checkout/OrderConfirmationPage';
import OrdersPage from './features/orders/OrderPage';
import LoginPage from "./features/auth/LoginPage";
import RegisterPage from "./features/auth/RegisterPage";

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <header className="sticky top-0 bg-white border-b">
        <div className="mx-auto max-w-6xl px-4 py-3 flex items-center gap-6">
          <Link to="/" className="font-semibold text-lg">E-Commerce</Link>
          <nav className="ml-auto flex items-center gap-4">
            <Link to="/" className="hover:underline">Catalog</Link>
            <Link to="/orders" className="hover:underline">Orders</Link>
            <Link to="/cart" className="hover:underline flex items-center">
              Cart <CartBadge />
            </Link>
            <Link to="/checkout" className="hover:underline">Checkout</Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
    </div>
  );
}

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<CatalogPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/order/:id" element={<OrderConfirmationPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Routes>
    </Layout>
  );
}

export default App
