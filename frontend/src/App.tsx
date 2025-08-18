import { Link, Route, Routes } from 'react-router-dom';

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <header className="sticky top-0 bg-white border-b">
        <div className="mx-auto max-w-6xl px-4 py-3 flex items-center gap-6">
          <Link to="/" className="font-semibold text-lg">E-Commerce</Link>
          <nav className="ml-auto flex items-center gap-4">
            <Link to="/" className="hover:underline">Catalog</Link>
            <Link to="/cart" className="hover:underline">Cart</Link>
            <Link to="/checkout" className="hover:underline">Checkout</Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
    </div>
  );
}

function CatalogPage() {
  return <h1 className="text-2xl font-bold">Catalog</h1>;
}

function CartPage() {
  return <h1 className="text-2xl font-bold">Cart</h1>;
}

function CheckoutPage() {
  return <h1 className="text-2xl font-bold">Checkout</h1>;
}

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<CatalogPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
      </Routes>
    </Layout>
  );
}

export default App
