import { useEffect, useState } from 'react';
import { api } from './api.js';
import { Field, Icon, Notice } from './components.jsx';
import { Dashboard } from './Dashboard.jsx';
import { Seats } from './Seats.jsx';
import { ThemeToggle } from './ThemeToggle.jsx';

export function Login() {
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true); setError('');
    try {
      const email = values.email.trim();
      const data = await api('/api/auth/login', { method: 'POST', body: { email, password: values.password }, authenticated: false });
      if (!data?.token || !data?.role || data.userId == null) throw new Error('Nepotpun odgovor servera.');
      for (const [key, value] of Object.entries({ jwtToken: data.token, role: data.role, userId: data.userId, userEmail: email })) localStorage.setItem(key, value);
      window.location.assign('/dashboard');
    } catch (error) { setError(error.message); } finally { setBusy(false); }
  }
  return <div className="login-container"><div className="login-card"><div className="login-header"><div className="brand-icon"><Icon name="book-open" /></div><h1>Biblioteka<span>Online Servis</span></h1><p>Uloguj se u biblioteku</p></div>
    <div className="login-theme"><ThemeToggle /></div>
    <Notice message={error} error /><form className="login-form" onSubmit={submit}>
      <Field label="Email" name="email" type="email" autoComplete="username" required placeholder="librarian@library.org" />
      <div className="password-wrapper"><Field label="Sifra" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" required /><button type="button" className="toggle-btn" aria-label={visible ? 'Sakrij sifru' : 'Prikazi sifru'} onClick={() => setVisible(!visible)}><Icon name={visible ? 'eye-slash' : 'eye'} /></button></div>
      <button className="login-btn" disabled={busy}>{busy ? 'Logovanje...' : 'Uloguj se'}</button>
    </form></div></div>;
}

export function App() {
  const path = window.location.pathname;
  const login = path === '/' || path === '/login';
  const authenticated = Boolean(localStorage.getItem('jwtToken'));
  useEffect(() => { if (!login && !authenticated) window.location.replace('/login'); }, [login, authenticated]);
  if (login) return <Login />;
  if (!authenticated) return null;
  return path === '/seat_reservation' ? <Seats /> : <Dashboard />;
}
