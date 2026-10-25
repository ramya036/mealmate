import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { api, money } from '../api.js';

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = publishableKey ? loadStripe(publishableKey) : null;

function PaymentForm({ orderId }) {
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const pay = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;
    setBusy(true);
    const { error: err } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: `${window.location.origin}/orders?paid=${orderId}` },
    });
    // Only reached on immediate errors; on success Stripe redirects, and the webhook marks the order PAID.
    setError(err?.message || '');
    setBusy(false);
  };

  return (
    <form onSubmit={pay} className="form">
      <PaymentElement />
      {error && <p className="error">{error}</p>}
      <button disabled={!stripe || busy}>{busy ? 'Processing…' : 'Pay'}</button>
      <p className="muted small">Test card: 4242 4242 4242 4242, any future date, any CVC.</p>
    </form>
  );
}

export default function CheckoutPage() {
  const { orderId } = useParams();
  const [checkout, setCheckout] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api(`/orders/${orderId}/checkout`, { method: 'POST' }).then(setCheckout).catch((e) => setError(e.message));
  }, [orderId]);

  return (
    <div className="narrow">
      <h1>Checkout</h1>
      <div className="card">
        {error && <><p className="error">{error}</p><Link to="/orders">Back to orders</Link></>}
        {!error && !checkout && <p className="muted">Preparing payment…</p>}
        {checkout && !stripePromise && <p className="error">Set VITE_STRIPE_PUBLISHABLE_KEY in frontend/.env to enable payments.</p>}
        {checkout && stripePromise && (
          <>
            <div className="row total"><span>Order #{checkout.orderId}</span><span>{money(checkout.amountCents / 100)}</span></div>
            <Elements stripe={stripePromise} options={{ clientSecret: checkout.clientSecret }}>
              <PaymentForm orderId={checkout.orderId} />
            </Elements>
          </>
        )}
      </div>
    </div>
  );
}
