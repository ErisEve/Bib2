import { useEffect, useState } from 'react';
import { api, collection } from './api.js';
import { Header, Notice, Stats } from './components.jsx';

export function Seats() {
  const [seats, setSeats] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  async function refresh() {
    setLoading(true); setError(''); setSelectedId(null);
    try {
      const data = await api('/api/seats');
      setSeats(collection(data).map(seat => ({ ...seat,
        number: seat.seat_number || seat.seatNumber || seat.number || 'N/A',
        occupied: Boolean(seat.user || seat.reservedBy || seat.status === 'occupied'),
      })));
    } catch (error) { setError(error.message); setSeats([]); } finally { setLoading(false); }
  }
  useEffect(() => { refresh(); }, []);
  const selected = seats.find(seat => seat.id === selectedId);
  async function reserve() {
    if (!selected || selected.occupied || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await api('/api/seats/reserve', { method: 'PUT', body: { seatId: selected.id, userId: localStorage.getItem('userId'), email: localStorage.getItem('userEmail') } });
      setMessage(`Seat ${selected.number} reserved successfully!`);
      await refresh();
    } catch (error) { setError(error.message); } finally { setBusy(false); }
  }
  return <div className="library-dashboard"><Header seats /><Stats items={[
    ['Slobodno', seats.filter(seat => !seat.occupied && seat.id !== selectedId).length, 'check-circle'],
    ['Zauzeto', seats.filter(seat => seat.occupied).length, 'times-circle'],
    ['Selektovano', selected ? 1 : 0, 'check-double'],
  ]} /><Notice message={error} error /><Notice message={message} />
    <div className="section-header"><h2>Prostor za citanje</h2><span className="badge-count">{seats.length} seats</span></div>
    <div className="seat-grid">{loading ? <p role="status">Ucitavanje...</p> : seats.length ? seats.map(seat => {
      const status = seat.occupied ? 'occupied' : seat.id === selectedId ? 'selected' : 'available';
      return <button className="seat-item" key={seat.id} disabled={seat.occupied || busy} aria-pressed={seat.id === selectedId} onClick={() => setSelectedId(seat.id === selectedId ? null : seat.id)}>
        <div className={`seat-icon ${status}`}>{seat.occupied ? '🔴' : seat.id === selectedId ? '🔵' : '🟢'}</div><div className="seat-label">{seat.number}</div><div className={`seat-status-text ${status}`}>{status}{seat.user?.email ? ` (${seat.user.email.split('@')[0]})` : ''}</div>
      </button>;
    }) : <p>Nema sedista.</p>}</div>
    <div className="action-panel"><div className="selected-info">Selected: {selected?.number || 'None'}{selected && <button className="remove-btn" aria-label="Ponisti izbor" disabled={busy} onClick={() => setSelectedId(null)}>×</button>}</div>
      <button className="btn btn-success" disabled={!selected || busy || loading} onClick={reserve}>Rezervisi</button><button className="btn btn-outline" disabled={busy || loading} onClick={() => { setMessage(''); refresh(); }}>Osvezi</button>
    </div><div className="footer-meta">🟢 Slobodno　🔴 Zauzeto　🔵 Selektovano</div>
  </div>;
}
