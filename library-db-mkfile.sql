--
-- PostgreSQL database dump
--

\restrict 2wCgI0009lKwNuABjmzhF3H2N2DWHNu2FvUU8rhTvhdE5VGA0uWbD82gEfErOat

-- Dumped from database version 17.10
-- Dumped by pg_dump version 17.10

-- Started on 2026-06-23 15:27:01

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE library_metadata;
--
-- TOC entry 4840 (class 1262 OID 16556)
-- Name: library_metadata; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE library_metadata WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'C.UTF-8';


ALTER DATABASE library_metadata OWNER TO postgres;

\unrestrict 2wCgI0009lKwNuABjmzhF3H2N2DWHNu2FvUU8rhTvhdE5VGA0uWbD82gEfErOat
\connect library_metadata
\restrict 2wCgI0009lKwNuABjmzhF3H2N2DWHNu2FvUU8rhTvhdE5VGA0uWbD82gEfErOat

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 4832 (class 0 OID 24744)
-- Dependencies: 222
-- Data for Name: books; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (26, 'F. Scott Fitzgerald', 'No description available for this book.', 'newone', '9780743273565', 2021, 'The Great Gatsby', 'https://covers.openlibrary.org/b/id/14314120-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (27, 'Harper Lee', 'When he was nearly thirteen, my brother Jem got his arm badly broken at the elbow.', 'newone', '9780061120084', 2006, 'To Kill a Mockingbird', 'https://covers.openlibrary.org/b/id/15162569-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (28, 'George Orwell', 'It was a bright cold day in April, and the clocks were striking thirteen.', 'newone', '9780452284234', 2003, 'Nineteen eighty-four', 'https://covers.openlibrary.org/b/id/7898938-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (29, 'Jane Austen', 'It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.', 'newone', '9780141439518', 2003, 'Pride and Prejudice', 'https://covers.openlibrary.org/b/id/12645114-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (30, 'Naoki Urasawa', 'No description available for this book.', 'newone', '9781421569079', 2014, 'Monster', 'https://covers.openlibrary.org/b/id/13111511-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (32, 'Olivie Blake', 'No description available for this book.', 'newone', '9781529095326', 2023, 'The Atlas Paradox', 'https://covers.openlibrary.org/b/id/15133394-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (33, 'Holly Black', 'No description available for this book.', 'newone', '9781471407277', 2018, 'The Cruel Prince (The Folk of the Air)', 'https://covers.openlibrary.org/b/id/8435418-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (34, 'Catherine Ryan Hyde', 'No description available for this book.', 'newone', '9781683245490', 2017, 'Allie and Bea', 'https://covers.openlibrary.org/b/id/10516688-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (35, 'Emma Darcy', 'No description available for this book.', 'a', '9780373114634', 1992, 'The Wedding', 'https://covers.openlibrary.org/b/id/10668053-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (36, 'Holly Jackson', 'No description available for this book.', 'a', '9780008621711', 2023, 'Good Girl, Bad Blood', 'https://covers.openlibrary.org/b/id/15178612-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (37, 'Hanya Yanagihara', 'The eleventh apartment had only one closet, but it did have a sliding glass door that opened onto a small balcony, from which he could see a man sitting across the way, outdoors in only a T-shirt and shorts even though it was October, smoking.', 'a', '9780385539258', 2015, 'A Little Life', 'https://covers.openlibrary.org/b/id/15231386-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (38, 'Lauren Roberts', 'No description available for this book.', 'a', '9798987380406', 2023, 'Powerless', 'https://covers.openlibrary.org/b/id/15172587-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (39, 'Colleen Hoover', '1500', 'a', '9781471156267', 2016, 'It Ends With Us', 'https://covers.openlibrary.org/b/id/15123232-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (40, 'Stephen King', 'Jack Torrance thought: Officious little prick.', 'a', '9780340797662', 1988, 'The Shining', 'https://covers.openlibrary.org/b/id/15090478-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (41, 'Stephen King', 'It was reliably reported by several persons that a rain of stones fell from a clear blue sky on Carlin Street in the town of Chamberlain on August 17th.', 'a', '9780340922842', 1999, 'Carrie', 'https://covers.openlibrary.org/b/id/14653022-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (42, 'Stephen King', 'umber whunnnn', 'a', '9781444741292', 2011, 'Misery', 'https://covers.openlibrary.org/b/id/15151485-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (44, 'Holly Black', 'No description available for this book.', 'a', '9781471408038', 2019, 'The Wicked King', 'https://covers.openlibrary.org/b/id/15158186-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (45, 'Holly Black', 'No description available for this book.', 'a', '9781471407598', 2020, 'The Queen of Nothing', 'https://covers.openlibrary.org/b/id/12645266-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (48, 'Holly Black', 'No description available for this book.', 'a', '9780316592703', 2023, 'The Stolen Heir', 'https://covers.openlibrary.org/b/id/13122196-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (43, 'Stephen King', 'Louis Creed, who had lost his father at three and who had never known a grandfather, never expected to find a father as he entered his middle age, but that was exactly what happened...although he called this man a friend, as a grown man must do when he finds the man who should have been his father relatively late in life.', 'a', '9780451150240', 1987, 'Pet Sematary', 'https://covers.openlibrary.org/b/id/14651682-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (46, 'Holly Black', 'No description available for this book.', 'a', '9780857073617', 2008, 'Tithe', 'https://covers.openlibrary.org/b/id/13151828-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (47, 'Holly Black', 'No description available for this book.', 'a', '9781780621746', 2016, 'The Darkest Part of the Forest', 'https://covers.openlibrary.org/b/id/13155932-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (49, 'William Peter Blatty', 'Northern Iraq...', 'aaa@library.com', '9780553113402', 1977, 'The Exorcist', 'https://covers.openlibrary.org/b/id/15231706-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (50, 'Freida McFadden', 'No description available for this book.', 'newone', '9781464221361', 2023, 'Never Lie', 'https://covers.openlibrary.org/b/id/15108166-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (51, 'H. G. Wells', 'No description available for this book.', 'newone', '9781402736889', 2007, 'Classic Starts', 'https://covers.openlibrary.org/b/id/1738958-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (53, 'Sarah J. Maas', 'No description available for this book.', 'aaa@library.com', '9781619634480', 2017, 'A Court of Wings and Ruin', 'https://covers.openlibrary.org/b/id/14859521-M.jpg', true);
INSERT INTO public.books (id, author, description, fetched_by, isbn, published_year, title, cover_url, available) VALUES (54, 'Mina N. Tanas', NULL, 'aaa@library.com', NULL, 2026, 'My Endless Apologies To Humankind', 'https://i.pinimg.com/736x/de/b2/36/deb2363f2db4d27733b5a5c132c52ba4.jpg', true);


--
-- NOTE: users is loaded before seats and lending, out of pg_dump's
-- alphabetical order. seats.reserved_by and lending.user_id are both
-- foreign keys to users(id).
-- TOC entry 4828 (class 0 OID 16558)
-- Dependencies: 218
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.users (id, email, password_hash, role) VALUES (1, 'john@library.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'LIBRARIAN');
INSERT INTO public.users (id, email, password_hash, role) VALUES (4, 'test@example.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'USER');
INSERT INTO public.users (id, email, password_hash, role) VALUES (6, 'lala@example.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'USER');
INSERT INTO public.users (id, email, password_hash, role) VALUES (9, 'aaa@library.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'LIBRARIAN');
INSERT INTO public.users (id, email, password_hash, role) VALUES (3, 'me@example.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'LIBRARIAN');
INSERT INTO public.users (id, email, password_hash, role) VALUES (5, 'newone@example.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'USER');
INSERT INTO public.users (id, email, password_hash, role) VALUES (8, 'a@example.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'LIBRARIAN');
INSERT INTO public.users (id, email, password_hash, role) VALUES (2, 'jane@library.com', '$2a$12$woRgcx5crq5IWy88GXlJruqIzRg5AhLY/X0x9oMvAGNAY.JVwxXDK', 'USER');


--
-- TOC entry 4834 (class 0 OID 32942)
-- Dependencies: 224
-- Data for Name: seats; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (1, 'A1', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (2, 'A2', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (3, 'A3', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (5, 'A5', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (7, 'A7', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (9, 'B1', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (10, 'B2', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (11, 'B3', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (12, 'B4', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (13, 'B5', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (14, 'B6', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (15, 'B7', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (16, 'B8', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (17, 'C1', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (18, 'C2', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (19, 'C3', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (20, 'C4', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (21, 'C5', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (22, 'C6', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (23, 'C7', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (24, 'C8', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (25, 'D1', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (26, 'D2', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (27, 'D3', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (28, 'D4', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (29, 'D5', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (30, 'D6', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (31, 'D7', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (32, 'D8', NULL);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (4, 'A4', 5);
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (8, 'A8', 6);
-- Was reserved_by = 6, but user 6 already holds seat A8 and Seat.user is
-- @OneToOne (UNIQUE on reserved_by), so this row could never load. Freed.
INSERT INTO public.seats (id, seat_number, reserved_by) VALUES (6, 'A6', NULL);


--
-- NOTE: lending is loaded last. Its foreign keys reference users(id)
-- and books(id), so loading it before users leaves every lending row
-- rejected on a fresh database.
-- TOC entry 4830 (class 0 OID 16580)
-- Dependencies: 220
-- Data for Name: lending; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (43, '2026-06-21', '2026-07-05', 6, 32);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (44, '2026-06-21', '2026-07-05', 6, 42);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (45, '2026-06-21', '2026-07-05', 6, 26);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (46, '2026-06-21', '2026-07-05', 6, 44);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (47, '2026-06-21', '2026-06-23', 6, 43);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (48, '2026-06-21', '2026-07-05', 1, 40);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (49, '2026-06-22', '2026-07-09', 6, 54);
INSERT INTO public.lending (id, borrow_date, return_date, user_id, book_id) VALUES (50, '2026-06-22', '2026-07-12', 1, 53);


--
-- TOC entry 4845 (class 0 OID 0)
-- Dependencies: 221
-- Name: books_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.books_id_seq', 54, true);


--
-- TOC entry 4846 (class 0 OID 0)
-- Dependencies: 219
-- Name: lending_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.lending_id_seq', 50, true);


--
-- TOC entry 4847 (class 0 OID 0)
-- Dependencies: 223
-- Name: seats_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.seats_id_seq', 32, true);


--
-- TOC entry 4848 (class 0 OID 0)
-- Dependencies: 217
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 9, true);


-- Completed on 2026-06-23 15:27:01

--
-- PostgreSQL database dump complete
--

\unrestrict 2wCgI0009lKwNuABjmzhF3H2N2DWHNu2FvUU8rhTvhdE5VGA0uWbD82gEfErOat

