import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useApp } from '../state.jsx';

export default function AuthPage() {
  const { signIn } = useApp();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ email: '', password: '', fullName: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const body = mode === 'login' ? { email: form.email, password: form.password } : form;
      const session = await api(`/auth/${mode === 'login' ? 'login' : 'register'}`, { method: 'POST', body });
      signIn(session);
      navigate(location.state?.from || '/');
    } catch (err) {
      setError(err.message);
    }
  };

  const field = (name) => ({ value: form[name], onChange: (e) => setForm({ ...form, [name]: e.target.value }) });

  return (
    <div className="narrow">
      <h1>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
      <form className="card form" onSubmit={submit}>
        {mode === 'register' && <label>Full name<input required {...field('fullName')} /></label>}
        <label>Email<input type="email" required {...field('email')} /></label>
        <label>Password<input type="password" required minLength={mode === 'register' ? 8 : undefined} {...field('password')} /></label>
        {error && <p className="error">{error}</p>}
        <button>{mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <button type="button" className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'New here? Create an account' : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  );
}
