import { createContext, useContext, useMemo, useReducer, useState } from 'react';
import { clearSession, getSession, saveSession } from './api.js';

const AppContext = createContext(null);

function cartReducer(cart, action) {
  switch (action.type) {
    case 'add': {
      const existing = cart.find((l) => l.item.id === action.item.id);
      if (existing) return cart.map((l) => (l === existing ? { ...l, quantity: Math.min(l.quantity + 1, 20) } : l));
      return [...cart, { item: action.item, quantity: 1 }];
    }
    case 'set':
      return action.quantity <= 0
        ? cart.filter((l) => l.item.id !== action.id)
        : cart.map((l) => (l.item.id === action.id ? { ...l, quantity: Math.min(action.quantity, 20) } : l));
    case 'clear':
      return [];
    default:
      return cart;
  }
}

export function AppProvider({ children }) {
  const [session, setSession] = useState(getSession);
  const [cart, dispatch] = useReducer(cartReducer, []);

  const value = useMemo(() => ({
    session,
    cart,
    cartCount: cart.reduce((n, l) => n + l.quantity, 0),
    cartTotal: cart.reduce((sum, l) => sum + Number(l.item.price) * l.quantity, 0),
    addToCart: (item) => dispatch({ type: 'add', item }),
    setQuantity: (id, quantity) => dispatch({ type: 'set', id, quantity }),
    clearCart: () => dispatch({ type: 'clear' }),
    signIn: (s) => { saveSession(s); setSession(s); },
    signOut: () => { clearSession(); setSession(null); },
  }), [session, cart]);

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export const useApp = () => useContext(AppContext);
