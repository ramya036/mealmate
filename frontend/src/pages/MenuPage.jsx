import { useEffect, useMemo, useState } from 'react';
import { api, money } from '../api.js';
import { useApp } from '../state.jsx';

export default function MenuPage() {
  const { addToCart } = useApp();
  const [items, setItems] = useState([]);
  const [category, setCategory] = useState('All');
  const [error, setError] = useState('');
  const [added, setAdded] = useState(null);

  useEffect(() => {
    api('/menu').then(setItems).catch((e) => setError(e.message));
  }, []);

  const categories = useMemo(() => ['All', ...new Set(items.map((i) => i.category))], [items]);
  const shown = category === 'All' ? items : items.filter((i) => i.category === category);

  const add = (item) => {
    addToCart(item);
    setAdded(item.id);
    setTimeout(() => setAdded(null), 900);
  };

  return (
    <>
      <h1>What are you craving?</h1>
      {error && <p className="error">{error}</p>}
      <div className="chips">
        {categories.map((c) => (
          <button key={c} className={`chip ${c === category ? 'active' : ''}`} onClick={() => setCategory(c)}>{c}</button>
        ))}
      </div>
      <div className="grid">
        {shown.map((item) => (
          <article key={item.id} className="card menu-card">
            <div className="menu-top">
              <h3>{item.name}</h3>
              <span className="price">{money(item.price)}</span>
            </div>
            <p className="muted">{item.description}</p>
            <button onClick={() => add(item)}>{added === item.id ? 'Added ✓' : 'Add to cart'}</button>
          </article>
        ))}
      </div>
    </>
  );
}
