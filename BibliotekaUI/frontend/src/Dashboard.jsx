import { useEffect, useRef, useState } from 'react';
import { api, collection } from './api.js';
import { Cover, Header, Notice, Stats } from './components.jsx';
import { AddBook, BookDetails } from './BookModals.jsx';

const borrower = loan => loan.userEmail || loan.user?.email || loan.borrowerEmail || '';
const dueDate = loan => loan.returnDate || loan.returnedDate || loan.returnedAt || '';

export function Dashboard() {
  const [books, setBooks] = useState([]);
  const [loans, setLoans] = useState([]);
  const [results, setResults] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [adding, setAdding] = useState(false);
  const [returning, setReturning] = useState(null);
  const searchRequest = useRef(0);
  const librarian = localStorage.getItem('role') === 'LIBRARIAN';

  async function refresh() {
    searchRequest.current++;
    setLoading(true); setError(''); setResults(null); setKeyword('');
    try {
      const [bookData, loanData] = await Promise.all([api('/api/books/booksAll'), api('/api/lendings')]);
      setBooks(collection(bookData)); setLoans(collection(loanData));
    } catch (error) { setError(error.message); } finally { setLoading(false); }
  }
  useEffect(() => { refresh(); }, []);

  async function search(event) {
    event.preventDefault();
    const request = ++searchRequest.current;
    if (!keyword.trim()) { setResults(null); setLoading(false); return; }
    setLoading(true); setError('');
    try {
      const data = await api(`/api/books/search?keyword=${encodeURIComponent(keyword.trim())}`);
      if (request === searchRequest.current) setResults(collection(data));
    } catch (error) { if (request === searchRequest.current) setError(error.message); }
    finally { if (request === searchRequest.current) setLoading(false); }
  }

  async function returnBook(loan) {
    if (!window.confirm('Vrati ovu knjigu?')) return;
    const id = loan.id ?? loan.loanId;
    setReturning(id); setError('');
    try { await api(`/api/lendings/delete/${encodeURIComponent(id)}`, { method: 'DELETE' }); await refresh(); }
    catch (error) { setError(error.message); } finally { setReturning(null); }
  }

  const loanedIds = new Set(loans.map(loan => String(loan.bookId ?? loan.book?.id)));
  const booksById = new Map(books.map(book => [String(book.id), book]));
  const available = (results ?? books).filter(book => !loanedIds.has(String(book.id)));
  const counts = new Map();
  for (const loan of loans) { const email = borrower(loan); if (email) counts.set(email, (counts.get(email) || 0) + 1); }
  const top = [...counts].sort((a, b) => b[1] - a[1])[0]?.[0].split('@')[0] || 'No data';

  return <div className="library-dashboard"><Header onAddBook={librarian ? () => setAdding(true) : undefined}>
    <form className="search-wrapper" onSubmit={search}><input aria-label="Pretrazi po kljucnoj reci" placeholder="Pretrazi po kljucnoj reci" value={keyword} onChange={event => setKeyword(event.target.value)} /><button>Pretrazi</button></form>
  </Header><Stats items={[
    ['Ukupan broj knjiga', books.length, 'book'], ['Ukupan broj autora', new Set(books.map(book => book.author).filter(Boolean)).size, 'pen-fancy'],
    ['Trenutno pozajmljeno', loans.length, 'hand-holding-heart'], ['Trenutni high score', top, 'calendar-plus'],
  ]} /><Notice message={error} error />{error && <button className="btn btn-secondary" onClick={refresh}>Pokusaj ponovo</button>}
    <div className="main-content catalog-layout"><section className="catalog-section"><div className="section-header"><h2>{results ? 'Rezultati pretrage' : 'Najskorije dodato'}</h2></div>
      <div className="book-grid">{loading ? <p role="status">Ucitavanje...</p> : available.length ? available.map(book => <button className="book-card" key={book.id} onClick={() => setSelected(book)}>
        <Cover book={book} /><h4>{book.title || book.bookTitle}</h4><div className="author">{book.author || book.bookAuthor}</div><div className="book-meta"><span>{book.isbn || 'N/A'}</span><span className={`status-badge ${book.available === false ? 'borrowed' : 'available'}`}>{book.available === false ? 'borrowed' : 'available'}</span></div>
      </button>) : <p>Nema knjiga.</p>}</div>
    </section><aside className="lent-section lending-panel"><div className="section-header"><h2>Trenutno pozajmljeno</h2></div>
      {!loading && loans.length === 0 && <p>Trenutno nema pozajmljenih knjiga!</p>}
      <ul className="lending-list">
      {[...loans].sort((a, b) => (dueDate(a) || '9999').localeCompare(dueDate(b) || '9999')).map(loan => {
        const catalogBook = booksById.get(String(loan.bookId ?? loan.book?.id));
        const book = {
          title: loan.book?.title || catalogBook?.title || loan.bookTitle || loan.title,
          author: loan.book?.author || catalogBook?.author || loan.bookAuthor,
          coverUrl: loan.book?.coverUrl || loan.book?.cover_url || loan.book?.cover || catalogBook?.coverUrl || catalogBook?.cover_url || catalogBook?.cover || loan.coverUrl || loan.cover_url || loan.cover,
        };
        const overdue = loan.overdue || loan.isOverdue;
        return <li className="lending-item" key={loan.id ?? loan.loanId}>
          <Cover book={book} /><div className="lending-info"><h4 title={book.title}>{book.title}</h4><div className="author">{book.author}</div>
          <div className="user-name">{borrower(loan)}</div>
          <div className="loan-meta">
            <div className={`lending-date${overdue ? ' overdue' : ''}`}>{dueDate(loan) || 'N/A'}{overdue && <span> — Kasni</span>}</div>
            {librarian && <button className="btn btn-secondary return-book-btn" disabled={returning !== null} onClick={() => returnBook(loan)}>Vrati knjigu</button>}
          </div>
          </div>
        </li>;
      })}
      </ul>
    </aside></div><div className="footer-meta"><span>Mina Nikolic 70/2019</span><span className="badge">PMF u Kragujevcu</span></div>
    {selected && <BookDetails book={selected} onClose={() => setSelected(null)} onSaved={refresh} />}
    {adding && <AddBook onClose={() => setAdding(false)} onSaved={refresh} />}
  </div>;
}
