--
-- PostgreSQL database dump
--

-- Dumped from database version 14.10 (Homebrew)
-- Dumped by pg_dump version 14.10 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;


--
-- Data for Name: institutions; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.institutions (id, active, confirmation_token, creation_date, email, name, nif, token_generation_date) FROM stdin;
1	t	abca428c09862e89	2026-02-06 17:58:21.402146	demo_institution@mail.com	DEMO INSTITUTION	000000000	2026-02-06 17:58:21.402134
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.users (user_type, id, creation_date, name, role, state, institution_id) FROM stdin;
MEMBER	2	2026-02-06 17:58:21.419878	DEMO-MEMBER	MEMBER	ACTIVE	1
VOLUNTEER	3	2026-02-06 17:58:23.732513	DEMO-VOLUNTEER	VOLUNTEER	ACTIVE	\N
VOLUNTEER	4	2026-02-06 17:58:23.732513	DEMO-VOLUNTEER2	VOLUNTEER	ACTIVE	\N
VOLUNTEER	5	2026-02-06 17:58:23.732513	DEMO-VOLUNTEER3	VOLUNTEER	ACTIVE	\N
\.


--
-- Data for Name: auth_users; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.auth_users (auth_type, id, active, email, last_access, password, username, confirmation_token, token_generation_date, user_id) FROM stdin;
DEMO	2	t	demo_member@mail.com	\N	\N	demo-member	\N	\N	2
DEMO	3	t	demo_volunteer@mail.com	\N	\N	demo-volunteer	\N	\N	3
DEMO	4	t	demo_volunteer@mail.com	\N	\N	demo-volunteer-2	\N	\N	4
DEMO	5	t	demo_volunteer@mail.com	\N	\N	demo-volunteer-3	\N	\N	5
\.


-- Data for Name: activity; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.activity (id, application_deadline, creation_date, description, ending_date, name, participants_number_limit, region, starting_date, state, institution_id) FROM stdin;
1	2026-02-06 17:58:21.402146	2026-01-06 17:58:21.402146	Has vacancies	2026-02-08 17:58:21.402146	A1	5	Lisbon	2026-02-07 17:58:21.402146	APPROVED	1
2	2026-02-06 17:58:21.402146	2026-01-06 17:58:21.402146	Has no vacancies	2026-02-08 17:58:21.402146	A2	1	Lisbon	2026-02-07 17:58:21.402146	APPROVED	1
\.


--
-- Data for Name: shift; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.shift (id, end_time, location, participants_limit, start_time, activity_id) FROM stdin;
1	2026-02-08 17:58:21.402146	Lisbon	2	2026-02-07 17:58:21.402146	1
2	2026-02-08 17:58:21.402146	Lisbon	2	2026-02-07 17:58:21.402146	1
3	2026-02-08 17:58:21.402146	Lisbon	1	2026-02-07 17:58:21.402146	2
\.


--
-- Data for Name: enrollment; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.enrollment (id, enrollment_date_time, motivation, volunteer_id) FROM stdin;
1	2026-02-06 18:51:37.595713	Has vacancies and do not participate	3
2	2026-02-06 19:51:37.595713	Has vacancies and participate	4
3	2026-02-06 18:51:37.595713	Has no vacancies and participate	3
4	2026-02-06 20:51:37.595713	Has no vacancies and do not participate	5
\.


--
-- Data for Name: enrollment_shifts; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.enrollment_shifts (enrollments_id, shifts_id) FROM stdin;
1	1
2	1
3	2
4	2
\.


--
-- Data for Name: participation; Type: TABLE DATA; Schema: public; Owner: ars
--

COPY public.participation (id, acceptance_date, member_rating, member_review, enrollment_id, shift_id) FROM stdin;
5	2026-02-06 18:51:37.595713	5	A-member-review	2	1
6	2026-02-06 18:51:37.595713	5	Another-member-review	3	2
\.

--
-- PostgreSQL database dump complete
--
