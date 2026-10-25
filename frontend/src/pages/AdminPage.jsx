import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { OrderCard } from './OrdersPage.jsx';

const NEXT = {
  PENDING_PAYMENT: ['CANCELLED'],
  PAID: ['PREPARING', 'CANCELLED'],
  PREPARING: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED'],
};

export default function AdminPage() {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState('ACTIVE');
  const [error, setError] = useState('');

  const load = () => api('/admin/orders').then(setOrders).catch((e) => setError(e.message));
  useEffect(() => {
    load();
    const t = setInterval(load, 10000); // simple polling for new orders
    return () => clearInterval(t);
  }, []);

  const move = async (id, status) => {
    try { await api(`/admin/orders/${id}/status`, { method: 'PATCH', body: { status } }); load(); }
    catch (e) { setError(e.message); }
  };

  const shown = orders.filter((o) => filter === 'ALL' || !['DELIVERED', 'CANCELLED'].includes(o.status));

  return (
    <div className="narrow">
      <h1>Kitchen dashboard</h1>
      <div className="chips">
        {['ACTIVE', 'ALL'].map((f) => (
          <button key={f} className={`chip ${filter === f ? 'active' : ''}`} onClick={() => setFilter(f)}>{f.toLowerCase()}</button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      {shown.length === 0 && <p className="muted">No orders here.</p>}
      {shown.map((o) => (
        <OrderCard key={o.id} order={o} actions={(NEXT[o.status] || []).map((s) => (
          <button key={s} className={s === 'CANCELLED' ? 'ghost' : ''} onClick={() => move(o.id, s)}>
            {s.replaceAll('_', ' ').toLowerCase()}
          </button>
        ))} />
      ))}
    </div>
  );
}
