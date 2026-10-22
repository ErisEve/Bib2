import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Login } from './App.jsx';
import { Dashboard } from './Dashboard.jsx';
import { Seats } from './Seats.jsx';
import { Profile } from './components.jsx';
import { AddBook } from './BookModals.jsx';

const respond = data => ({ ok: true, status: 200, text: async () => JSON.stringify(data) });
beforeEach(() => {
  localStorage.clear();
  localStorage.setItem('jwtToken', 'test-token');
  localStorage.setItem('userEmail', 'reader@example.com');
  localStorage.setItem('userId', '7');
  localStorage.setItem('role', 'USER');
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

test('failed login displays the server error and does not replace the session', async () => {
  const fetch = vi.fn().mockResolvedValue({ ok: false, status: 401, text: async () => 'Invalid email or password' });
  vi.stubGlobal('fetch', fetch);
  render(<Login />);
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Email'), 'wrong@example.com');
  await user.type(screen.getByLabelText('Sifra'), 'wrong-password');
  await user.click(screen.getByRole('button', { name: 'Uloguj se' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Invalid email or password');
  expect(localStorage.getItem('userEmail')).toBe('reader@example.com');
  expect(fetch.mock.calls[0][1].headers.Authorization).toBeUndefined();
});

test('dashboard loads empty collections without waiting for books indefinitely', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respond([])));
  render(<Dashboard />);
  expect(await screen.findByText('Nema knjiga.')).toBeTruthy();
  expect(screen.getByText('Trenutno nema pozajmljenih knjiga!')).toBeTruthy();
});

test('search and borrowing retain the API contract and exclude loaned books', async () => {
  const book = { id: 10, title: 'React Book', author: 'Author' };
  const fetch = vi.fn(async path => respond(path.includes('lendings') ? [] : [book]));
  vi.stubGlobal('fetch', fetch);
  render(<Dashboard />);
  const user = userEvent.setup();
  await screen.findByRole('button', { name: /React Book/ });
  await user.type(screen.getByLabelText('Pretrazi po kljucnoj reci'), 'React & JS');
  await user.click(screen.getByRole('button', { name: 'Pretrazi' }));
  await waitFor(() => expect(fetch.mock.calls.some(([url]) => url === '/api/books/search?keyword=React%20%26%20JS')).toBe(true));
  await user.click(await screen.findByRole('button', { name: /React Book/ }));
  await user.click(screen.getByRole('button', { name: 'Pozajmi knjigu' }));
  await waitFor(() => expect(fetch.mock.calls.some(([url]) => url === '/api/lendings/loanABook')).toBe(true));
  const options = fetch.mock.calls.find(([url]) => url === '/api/lendings/loanABook')[1];
  expect(JSON.parse(options.body)).toEqual({ token: 'test-token', bookId: 10, userEmail: 'reader@example.com', days: 14 });
  expect(options.headers.Authorization).toBe('Bearer test-token');
});

test('seat selection reserves the selected ID and refreshes availability', async () => {
  let reserved = false;
  const fetch = vi.fn(async path => {
    if (path.endsWith('/reserve')) { reserved = true; return respond({}); }
    return respond([{ id: 1, seat_number: 'A1', user: reserved ? { email: 'reader@example.com' } : null }, { id: 2, seat_number: 'A2', user: { email: 'other@example.com' } }]);
  });
  vi.stubGlobal('fetch', fetch);
  render(<Seats />);
  const user = userEvent.setup();
  expect((await screen.findByRole('button', { name: /A2/ })).disabled).toBe(true);
  await user.click(screen.getByRole('button', { name: /A1/ }));
  await user.click(screen.getByRole('button', { name: 'Rezervisi' }));
  await waitFor(() => expect(screen.getByRole('button', { name: /A1/ }).disabled).toBe(true));
  expect(JSON.parse(fetch.mock.calls.find(([url]) => url.endsWith('/reserve'))[1].body)).toEqual({ seatId: 1, userId: '7', email: 'reader@example.com' });
});

test('seat errors do not create reservable demo data', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Offline')));
  render(<Seats />);
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Offline');
  expect(screen.getByRole('button', { name: 'Rezervisi' }).disabled).toBe(true);
  expect(screen.queryByRole('button', { name: /A1/ })).toBeNull();
});

test('profile updates store the refreshed token and email', async () => {
  const fetch = vi.fn().mockResolvedValue(respond({ newToken: 'refreshed-token' }));
  vi.stubGlobal('fetch', fetch);
  const onSaved = vi.fn();
  render(<Profile onClose={() => {}} onSaved={onSaved} />);
  const user = userEvent.setup();
  await user.clear(screen.getByLabelText('Email address'));
  await user.type(screen.getByLabelText('Email address'), 'new@example.com');
  await user.click(screen.getByRole('button', { name: 'Sacuvaj promene' }));
  await waitFor(() => expect(onSaved).toHaveBeenCalledWith('new@example.com'));
  expect(localStorage.getItem('jwtToken')).toBe('refreshed-token');
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ email: 'new@example.com' });
});

test('manual and ISBN book creation use the existing endpoints', async () => {
  const fetch = vi.fn().mockResolvedValue(respond('Saved'));
  vi.stubGlobal('fetch', fetch);
  const saved = vi.fn();
  render(<AddBook onClose={() => {}} onSaved={saved} />);
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('ISBN'), '9781234567890');
  await user.click(screen.getByRole('button', { name: 'Fetch Book' }));
  await waitFor(() => expect(saved).toHaveBeenCalledTimes(1));
  expect(fetch.mock.calls[0][0]).toBe('/api/books/fetch?isbn=9781234567890');
  expect(fetch.mock.calls[0][1].method).toBe('POST');
  await user.click(screen.getByRole('button', { name: 'Manuelno ubacivanje' }));
  await user.type(screen.getByLabelText('Naslov'), 'New Book');
  await user.type(screen.getByLabelText('Autor'), 'Writer');
  await user.click(screen.getByRole('button', { name: 'Dodaj knjigu' }));
  await waitFor(() => expect(saved).toHaveBeenCalledTimes(2));
  expect(fetch.mock.calls[1][0]).toBe('/api/books/add');
  expect(JSON.parse(fetch.mock.calls[1][1].body)).toEqual({ title: 'New Book', author: 'Writer', isbn: null, publishedYear: null, description: null, coverUrl: null });
});

test('librarians can return loans and borrowed books are excluded from the grid', async () => {
  localStorage.setItem('role', 'LIBRARIAN');
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  const fetch = vi.fn(async path => respond(path.includes('booksAll') ? [{ id: 10, title: 'Borrowed Book' }] : path.includes('delete') ? null : [{ id: 30, bookId: 10, bookTitle: 'Borrowed Book', userEmail: 'reader@example.com' }]));
  vi.stubGlobal('fetch', fetch);
  render(<Dashboard />);
  const user = userEvent.setup();
  await screen.findByText('Borrowed Book');
  expect(screen.queryByRole('button', { name: /Borrowed Book/ })).toBeNull();
  await user.click(screen.getByRole('button', { name: 'Vrati knjigu' }));
  await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url === '/api/lendings/delete/30' && options.method === 'DELETE')).toBe(true));
});

test.each([
  { bookId: '10', bookTitle: 'Borrowed Book' },
  { book: { id: 10, title: 'Borrowed Book', author: 'Writer', coverUrl: '/covers/borrowed.jpg' } },
])('lent cards resolve covers from catalog IDs or embedded books', async loanBook => {
  const book = { id: 10, title: 'Borrowed Book', author: 'Writer', coverUrl: '/covers/borrowed.jpg' };
  vi.stubGlobal('fetch', vi.fn(async path => respond(path.includes('booksAll') ? [book] : [
    { id: 30, ...loanBook, userEmail: 'reader@example.com', returnDate: '2026-09-01', overdue: true },
  ])));
  render(<Dashboard />);
  const cover = await screen.findByRole('img', { name: 'Borrowed Book' });
  expect(cover.getAttribute('src')).toBe('/covers/borrowed.jpg');
  expect(screen.getByText('Writer')).toBeTruthy();
  expect(screen.getByText('reader@example.com')).toBeTruthy();
  expect(screen.getByText(/2026-09-01/).classList.contains('overdue')).toBe(true);
  expect(screen.queryByRole('button', { name: 'Vrati knjigu' })).toBeNull();
  expect(screen.queryByRole('button', { name: /Borrowed Book/ })).toBeNull();
});

test('only librarians see the add-book menu item', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respond([])));
  const user = userEvent.setup();
  render(<Dashboard />);
  await screen.findByText('Nema knjiga.');
  await user.click(screen.getByRole('button', { name: /reader/ }));
  expect(screen.queryByRole('button', { name: 'Dodaj novu knjigu' })).toBeNull();
  cleanup();

  localStorage.setItem('role', 'LIBRARIAN');
  render(<Dashboard />);
  await screen.findByText('Nema knjiga.');
  await user.click(screen.getByRole('button', { name: /reader/ }));
  expect(screen.getByRole('button', { name: 'Dodaj novu knjigu' })).toBeTruthy();
});
