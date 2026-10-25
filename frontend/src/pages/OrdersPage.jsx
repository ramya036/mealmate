import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, money } from '../api.js';

export const statusLabel = (s) => s.replaceAll('_', ' ').toLowerCase();

export function OrderCard({ order, actions }) {
  return (
    <article className="card">
      <div className="row">
        <strong>Order #{order.id}</strong>
        <span className={`status ${order.status.toLowerCase()}`}>{statusLabel(order.status)}</span>
      </div>
      <p className="muted">{new Date(order.createdAt).toLocaleString()} · {order.deliveryAddress}</p>
      <ul className="lines">
        {order.items.map((l) => <li key={l.menuItemId}>{l.quantity} × {l.name}</li>)}
      </ul>
      <div className="row"><strong>{money(order.total)}</strong><div className="actions">{actions}</div></div>
    </article>
  );
}

export default function OrdersPage() {
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState('');

  const load = () => api('/orders').then(setOrders).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const cancel = async (id) => {
    try { await api(`/orders/${id}/cancel`, { method: 'POST' }); load(); } catch (e) { setError(e.message); }
  };

  if (!orders) return <p className="muted">{error || 'Loading…'}</p>;
  return (
    <div className="narrow">
      <h1>My orders</h1>
      {error && <p className="error">{error}</p>}
      {orders.length === 0 && <p className="muted">No orders yet. <Link to="/">Order something tasty.</Link></p>}
      {orders.map((o) => (
        <OrderCard key={o.id} order={o} actions={o.status === 'PENDING_PAYMENT' && (
          <>
            <Link className="button" to={`/checkout/${o.id}`}>Pay now</Link>
            <button className="ghost" onClick={() => cancel(o.id)}>Cancel</button>
          </>
        )} />
      ))}
    </div>
  );
}
