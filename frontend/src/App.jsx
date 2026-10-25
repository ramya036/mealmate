import { Link, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { useApp } from './state.jsx';
import MenuPage from './pages/MenuPage.jsx';
import CartPage from './pages/CartPage.jsx';
import AuthPage from './pages/AuthPage.jsx';
import OrdersPage from './pages/OrdersPage.jsx';
import CheckoutPage from './pages/CheckoutPage.jsx';
import AdminPage from './pages/AdminPage.jsx';

function RequireAuth({ children, admin = false }) {
  const { session } = useApp();
  if (!session) return <Navigate to="/login" replace />;
  if (admin && session.user.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const { session, cartCount, signOut } = useApp();
  return (
    <>
      <header className="nav">
        <Link to="/" className="brand">MealMate</Link>
        <nav>
          <NavLink to="/" end>Menu</NavLink>
          {session && <NavLink to="/orders">My orders</NavLink>}
          {session?.user.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
          <NavLink to="/cart" className="cart-link">Cart{cartCount > 0 && <span className="badge">{cartCount}</span>}</NavLink>
          {session
            ? <button className="link" onClick={signOut}>Sign out</button>
            : <NavLink to="/login">Sign in</NavLink>}
        </nav>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<MenuPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/login" element={<AuthPage />} />
          <Route path="/orders" element={<RequireAuth><OrdersPage /></RequireAuth>} />
          <Route path="/checkout/:orderId" element={<RequireAuth><CheckoutPage /></RequireAuth>} />
          <Route path="/admin" element={<RequireAuth admin><AdminPage /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
