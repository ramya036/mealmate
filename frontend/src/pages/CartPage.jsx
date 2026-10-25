import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, money } from '../api.js';
import { useApp } from '../state.jsx';

export default function CartPage() {
  const { cart, cartTotal, setQuantity, clearCart, session } = useApp();
  const [address, setAddress] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  if (cart.length === 0) {
    return <div className="empty"><h2>Your cart is empty</h2><Link to="/">Browse the menu</Link></div>;
  }

  const placeOrder = async (e) => {
    e.preventDefault();
    if (!session) return navigate('/login', { state: { from: '/cart' } });
    setBusy(true);
    setError('');
    try {
      // Only ids and quantities are sent; the server prices the order.
      const order = await api('/orders', {
        method: 'POST',
        body: { deliveryAddress: address, items: cart.map((l) => ({ menuItemId: l.item.id, quantity: l.quantity })) },
      });
      clearCart();
      navigate(`/checkout/${order.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="narrow">
      <h1>Your cart</h1>
      <div className="card">
        {cart.map(({ item, quantity }) => (
          <div key={item.id} className="row">
            <span>{item.name}</span>
            <div className="qty">
              <button className="ghost" onClick={() => setQuantity(item.id, quantity - 1)} aria-label="Decrease">−</button>
              <span>{quantity}</span>
              <button className="ghost" onClick={() => setQuantity(item.id, quantity + 1)} aria-label="Increase">+</button>
            </div>
            <span>{money(item.price * quantity)}</span>
          </div>
        ))}
        <div className="row total"><span>Total</span><span>{money(cartTotal)}</span></div>
      </div>
      <form className="card form" onSubmit={placeOrder}>
        <label>Delivery address
          <input required value={address} onChange={(e) => setAddress(e.target.value)} placeholder="Street, city, ZIP" />
        </label>
        {error && <p className="error">{error}</p>}
        <button disabled={busy}>{busy ? 'Placing order…' : session ? 'Place order & pay' : 'Sign in to order'}</button>
      </form>
    </div>
  );
}
