import { useState } from 'react';
import { api } from './api.js';
import { Cover, Field, Modal, Notice } from './components.jsx';

export function BookDetails({ book, onClose, onSaved }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const ownEmail = (localStorage.getItem('role') || 'USER') === 'USER';
  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true); setError('');
    try {
      await api('/api/lendings/loanABook', { method: 'POST', body: { token: localStorage.getItem('jwtToken'), bookId: Number(book.id), userEmail: values.email.trim(), days: Number(values.days) } });
      onSaved(); onClose();
    } catch (error) { setError(error.message); } finally { setBusy(false); }
  }
  return <Modal title={book.title || 'Book Details'} onClose={onClose}>
    <div className="book-detail-content"><Cover book={book} detail /><div className="book-detail-info">
      {[['Naziv', book.title], ['Autor', book.author], ['ISBN', book.isbn], ['Godina', book.publishedYear || book.year], ['Opis', book.description]].map(([label, value]) => <div className="detail-row" key={label}><span className="detail-label">{label}:</span><span className="detail-value">{value || 'N/A'}</span></div>)}
      <span className={`status-badge ${book.available === false ? 'borrowed' : 'available'}`}>{book.available === false ? 'borrowed' : 'available'}</span>
    </div></div>
    <div className="loan-section"><h3>Pozajmi ovu knjigu:</h3><form className="loan-form" onSubmit={submit}><div className="form-row">
      <Field label="Korisnicki email" name="email" type="email" required readOnly={ownEmail} defaultValue={ownEmail ? localStorage.getItem('userEmail') : ''} />
      <Field label="Broj dana" name="days" type="number" min="1" max="30" defaultValue="14" required />
    </div><Notice message={error} error /><div className="modal-actions"><button className="btn btn-secondary" type="button" onClick={onClose}>Odustani</button><button className="btn btn-primary" disabled={busy || book.available === false}>Pozajmi knjigu</button></div></form></div>
  </Modal>;
}

export function AddBook({ onClose, onSaved }) {
  const [tab, setTab] = useState('isbn');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true); setError('');
    try {
      if (tab === 'isbn') await api(`/api/books/fetch?isbn=${encodeURIComponent(values.isbn.trim())}`, { method: 'POST' });
      else await api('/api/books/add', { method: 'POST', body: {
        title: values.title.trim(), author: values.author.trim(), isbn: values.isbn.trim() || null,
        publishedYear: values.publishedYear ? Number(values.publishedYear) : null,
        description: values.description.trim() || null, coverUrl: values.coverUrl.trim() || null,
      } });
      onSaved(); onClose();
    } catch (error) { setError(error.message); } finally { setBusy(false); }
  }
  return <Modal title="Dodaj novu knjigu" onClose={onClose}><div className="add-book-tabs">
    {[['isbn', 'By ISBN'], ['manual', 'Manuelno ubacivanje']].map(([value, label]) => <button key={value} disabled={busy} className={`tab-btn ${tab === value ? 'active' : ''}`} onClick={() => { setTab(value); setError(''); }}>{label}</button>)}
  </div><form key={tab} className="add-book-form" onSubmit={submit}>
    {tab === 'manual' && <><Field label="Naslov" name="title" required /><Field label="Autor" name="author" required /></>}
    <Field label="ISBN" name="isbn" required={tab === 'isbn'} />
    {tab === 'manual' && <><Field label="Godina" name="publishedYear" type="number" min="1000" max="2100" /><div className="form-group"><label htmlFor="description">Opis</label><textarea id="description" name="description" rows="4" /></div><Field label="Slika naslovnice URL" name="coverUrl" type="url" /></>}
    <Notice message={error} error /><Notice message={busy ? 'Ubacivanje knjige...' : ''} /><div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Odustani</button><button className="btn btn-primary" disabled={busy}>{tab === 'isbn' ? 'Fetch Book' : 'Dodaj knjigu'}</button></div>
  </form></Modal>;
}
