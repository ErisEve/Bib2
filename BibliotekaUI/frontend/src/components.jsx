import { useEffect, useId, useRef, useState } from 'react';
import { api, logout } from './api.js';
import { ThemeToggle } from './ThemeToggle.jsx';

export const Icon = ({ name }) => <i aria-hidden="true" className={`fas fa-${name}`} />;
export const Notice = ({ message, error = false }) => message && <div role={error ? 'alert' : 'status'} className={`notice ${error ? 'error' : ''}`}>{message}</div>;

export function Field({ label, ...props }) {
  const id = useId();
  return <div className="form-group"><label htmlFor={id}>{label}</label><input id={id} {...props} /></div>;
}

export function Modal({ title, onClose, children }) {
  const ref = useRef(null);
  const titleId = useId();
  useEffect(() => {
    const previous = document.activeElement;
    const node = ref.current;
    node.querySelector('button, input')?.focus();
    function keydown(event) {
      if (event.key === 'Escape') onClose();
      if (event.key !== 'Tab') return;
      const elements = [...node.querySelectorAll('button:not(:disabled), input:not(:disabled), textarea, [tabindex="0"]')];
      const first = elements[0], last = elements.at(-1);
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    }
    node.addEventListener('keydown', keydown);
    return () => { node.removeEventListener('keydown', keydown); previous?.focus(); };
  }, [onClose]);
  return <div className="modal-overlay open" onMouseDown={event => event.target === event.currentTarget && onClose()}>
    <div className="modal-card modal-lg" role="dialog" aria-modal="true" aria-labelledby={titleId} ref={ref}>
      <div className="modal-header"><h2 id={titleId}>{title}</h2><button className="modal-close" onClick={onClose} aria-label="Zatvori">×</button></div>
      {children}
    </div>
  </div>;
}

export function Profile({ onClose, onSaved }) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (values.password !== values.confirmPassword) return setError('Sifre se ne poklapaju.');
    setBusy(true); setError('');
    try {
      const email = values.email.trim();
      const result = await api('/api/users/update', { method: 'PUT', body: { email, ...(values.password ? { password: values.password } : {}) } });
      if (result?.newToken) localStorage.setItem('jwtToken', result.newToken);
      localStorage.setItem('userEmail', email);
      onSaved(email); onClose();
    } catch (error) { setError(error.message); } finally { setBusy(false); }
  }
  return <Modal title="Izmeni profil" onClose={onClose}><form onSubmit={submit}>
    <Field label="Email address" name="email" type="email" required defaultValue={localStorage.getItem('userEmail') || ''} />
    <Field label="Nova sifra" name="password" type="password" minLength={6} autoComplete="new-password" />
    <Field label="Potvrdi sifru" name="confirmPassword" type="password" autoComplete="new-password" />
    <Notice message={error} error /><div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Odustani</button><button className="btn btn-primary" disabled={busy}>Sacuvaj promene</button></div>
  </form></Modal>;
}

export function Header({ seats = false, onAddBook, children }) {
  const [menu, setMenu] = useState(false);
  const [profile, setProfile] = useState(false);
  const [email, setEmail] = useState(localStorage.getItem('userEmail') || '');
  const ref = useRef(null);
  useEffect(() => {
    const close = event => { if (!ref.current?.contains(event.target) || event.key === 'Escape') setMenu(false); };
    document.addEventListener('click', close); document.addEventListener('keydown', close);
    return () => { document.removeEventListener('click', close); document.removeEventListener('keydown', close); };
  }, []);
  return <><header className="dashboard-header">
    <div className="header-left"><a href="/dashboard" className="brand-icon" aria-label="Biblioteka"><Icon name="book-open" /></a><h1>Biblioteka<span>{seats ? 'Rezervacija stola' : 'Online Servis'}</span></h1></div>
    <a className="nav-link active" href={seats ? '/dashboard' : '/seat_reservation'}><Icon name={seats ? 'book' : 'chair'} /> {seats ? 'Pozajmi knjigu' : 'Rezerviši sedište'}</a>
    <div className="header-actions">{children}<ThemeToggle /><div className="user-profile" ref={ref}>
      <button className="profile-trigger" aria-expanded={menu} aria-haspopup="true" onClick={() => setMenu(!menu)}><span className="avatar">{email.charAt(0).toUpperCase() || 'L'}</span>{email.split('@')[0]}<Icon name="chevron-down" /></button>
      {menu && <div className="context-menu open"><button className="menu-item" onClick={() => { setProfile(true); setMenu(false); }}><Icon name="user-edit" />Promeni podatke</button>
        {onAddBook && <button className="menu-item" onClick={() => { onAddBook(); setMenu(false); }}><Icon name="plus-circle" />Dodaj novu knjigu</button>}
        <button className="menu-item danger" onClick={logout}><Icon name="sign-out-alt" />Odjavi se</button></div>}
    </div></div>
  </header>{profile && <Profile onClose={() => setProfile(false)} onSaved={setEmail} />}</>;
}

export function Stats({ items }) {
  return <div className="stats-grid">{items.map(([label, value, icon]) => <div className="stat-card" key={label}><div className="stat-info"><h4>{label}</h4><div className="value">{value}</div></div><div className="stat-icon"><Icon name={icon} /></div></div>)}</div>;
}

export function Cover({ book, detail = false }) {
  const [failed, setFailed] = useState(false);
  const url = book.coverUrl || book.cover_url || book.cover;
  return <div className={detail ? 'book-detail-cover' : 'book-cover'}>{url && !failed ? <img src={url} alt={book.title || 'Naslovnica'} onError={() => setFailed(true)} /> : <Icon name="book-open" />}</div>;
}
