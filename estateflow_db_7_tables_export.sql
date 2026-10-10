--
-- PostgreSQL database dump
--

\restrict x9NCiG0rPdwWBfeyq9y4Diy43ZWHVHwZqHh4aBGfgxr3L9TUwjOmeJWxgbzRCfo

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: agent_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.agent_profiles (
    id character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    license_no character varying(100) NOT NULL,
    phone character varying(50) NOT NULL,
    email character varying(255) NOT NULL,
    rating numeric(3,2) DEFAULT 4.90,
    reviews_count integer DEFAULT 124,
    total_sales_volume character varying(50) DEFAULT '₱48.2M'::character varying,
    active_listings_count integer DEFAULT 0,
    bio text,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: inquiries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inquiries (
    id character varying(100) NOT NULL,
    property_id character varying(100),
    property_title character varying(255) NOT NULL,
    property_address character varying(255) DEFAULT ''::character varying,
    sender_name character varying(255) NOT NULL,
    sender_email character varying(255) NOT NULL,
    sender_phone character varying(50) DEFAULT ''::character varying,
    message text NOT NULL,
    time_ago character varying(50) DEFAULT 'Just now'::character varying,
    is_unread boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: inquiry_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inquiry_messages (
    id character varying(100) NOT NULL,
    inquiry_id character varying(100) NOT NULL,
    sender character varying(50) NOT NULL,
    text text NOT NULL,
    "time" character varying(50) DEFAULT 'Just now'::character varying,
    is_from_me boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id character varying(100) NOT NULL,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    timestamp_formatted character varying(50) DEFAULT 'Just now'::character varying,
    type character varying(50) DEFAULT 'VIEWING_BOOKED'::character varying NOT NULL,
    is_read boolean DEFAULT false,
    target_id character varying(100),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: properties; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.properties (
    id character varying(100) NOT NULL,
    title character varying(255) NOT NULL,
    address character varying(255) NOT NULL,
    city_state_zip character varying(255) NOT NULL,
    price bigint NOT NULL,
    price_formatted character varying(100) NOT NULL,
    is_rental boolean DEFAULT false,
    beds numeric(3,1) DEFAULT 1.0 NOT NULL,
    baths numeric(3,1) DEFAULT 1.0 NOT NULL,
    sqft integer DEFAULT 500 NOT NULL,
    property_type character varying(100) DEFAULT 'Condominium'::character varying NOT NULL,
    description text,
    image_res_id integer DEFAULT 0,
    media_uris text DEFAULT '[]'::text,
    is_favorite boolean DEFAULT false,
    status character varying(50) DEFAULT 'Active'::character varying,
    available_dates text DEFAULT '["Mon, Oct 14", "Tue, Oct 15", "Wed, Oct 16", "Thu, Oct 17"]'::text,
    available_time_slots text DEFAULT '["10:00 AM", "1:30 PM", "4:00 PM", "5:30 PM"]'::text,
    amenities text DEFAULT '["Modern Design", "High Speed Internet", "Near Transit"]'::text,
    year_built integer DEFAULT 2022,
    agent_name character varying(255) DEFAULT 'Sarah Jenkins'::character varying,
    agent_title character varying(255) DEFAULT 'Premier Real Estate Broker • Cebu City Properties'::character varying,
    agent_phone character varying(50) DEFAULT '+63 917 555 0192'::character varying,
    agent_email character varying(255) DEFAULT 'sarah.jenkins@estateflow.com'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    phone character varying(50) DEFAULT ''::character varying,
    role character varying(20) DEFAULT 'BUYER'::character varying,
    license_no character varying(100) DEFAULT ''::character varying,
    agency character varying(255) DEFAULT ''::character varying,
    professional_role character varying(100) DEFAULT 'Licensed Agent'::character varying,
    password_hash character varying(255) DEFAULT ''::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: viewing_appointments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.viewing_appointments (
    id character varying(100) NOT NULL,
    property_id character varying(100),
    property_title character varying(255) NOT NULL,
    property_address character varying(255) NOT NULL,
    client_name character varying(255) NOT NULL,
    client_phone character varying(50) DEFAULT ''::character varying,
    client_email character varying(255) DEFAULT ''::character varying,
    date character varying(100) NOT NULL,
    time_slot character varying(50) NOT NULL,
    type character varying(50) DEFAULT 'Viewing'::character varying,
    status character varying(50) DEFAULT 'UPCOMING'::character varying,
    notes text,
    created_timestamp bigint DEFAULT ((EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric))::bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Data for Name: agent_profiles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.agent_profiles (id, name, title, license_no, phone, email, rating, reviews_count, total_sales_volume, active_listings_count, bio, updated_at) FROM stdin;
agent_verified_sarah	Sarah Jenkins	Premier Real Estate Broker • Cebu City Properties	PRC-REB-0094821	+63 917 555 0192	sarah.jenkins@estateflow.com	4.90	124	₱48.2M	5	Specializing in premier residential condominiums, townhomes, and luxury family estates across Cebu City, Cebu IT Park, and Cebu Business Park.	2026-09-25 21:22:38.823752+08
\.


--
-- Data for Name: inquiries; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.inquiries (id, property_id, property_title, property_address, sender_name, sender_email, sender_phone, message, time_ago, is_unread, created_at, updated_at) FROM stdin;
749a68be-4381-45e4-b096-9640908792bd	prop_sunny_meadows	Sunny Meadows Craftsman Estate	1240 Whispering Pines Lane	Maverick Teves	tevesmaverick@gmail.com	09502291607	Hi Sarah, I'm very interested in this property. Could you provide more details about the strata fees and scheduling an in-person viewing?	Just now	f	2026-10-07 09:03:32.15181+08	2026-10-07 09:03:32.15181+08
inq_demo_1	\N	The Pinecrest Glasshouse Sanctuary	88 High Ridge Way, Beverly Crest	Marcus Vance	marcus.vance@example.com	+1 (555) 987-1234	Hello Sarah, what are the HOA dues and does the property have backup solar batteries installed?	2 hours ago	f	2026-09-25 21:22:38.823752+08	2026-09-25 21:22:38.823752+08
\.


--
-- Data for Name: inquiry_messages; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.inquiry_messages (id, inquiry_id, sender, text, "time", is_from_me, created_at) FROM stdin;
msg_demo_1_1	inq_demo_1	user	Hello Sarah, what are the HOA dues and does the property have backup solar batteries installed?	2 hours ago	f	2026-09-25 21:22:38.823752+08
msg_demo_1_2	inq_demo_1	agent	Hi Marcus! There are no HOA fees on this parcel. It has 3 Tesla Powerwall 3 batteries included.	1 hour ago	t	2026-09-25 21:22:38.823752+08
msg_f52a376c3413	749a68be-4381-45e4-b096-9640908792bd	user	Hi Sarah, I'm very interested in this property. Could you provide more details about the strata fees and scheduling an in-person viewing?	Just now	f	2026-10-07 09:03:32.15181+08
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notifications (id, title, message, timestamp_formatted, type, is_read, target_id, created_at) FROM stdin;
notif_demo_1	Viewing Appointment Booked	David Miller scheduled a viewing for The Pinecrest Glasshouse Sanctuary on Mon, Oct 14 at 1:30 PM.	10 mins ago	VIEWING_BOOKED	f	appt_demo_1	2026-09-25 21:22:38.823752+08
notif_demo_2	New Lead Inquiry	Marcus Vance submitted an inquiry on The Pinecrest Glasshouse Sanctuary.	2 hours ago	NEW_INQUIRY	f	inq_demo_1	2026-09-25 21:22:38.823752+08
notif_e24b9ed1a0cd	New Property Published	Smaple is now live on Estateflow with active viewing bookings.	Just now	LISTING_UPDATE	f	31390ca2-a272-434d-9242-627b92b6ba5c	2026-09-25 21:41:56.300539+08
notif_5dca64997505	Listing Removed	Smaple has been removed from active listings in PostgreSQL.	Just now	LISTING_UPDATE	f	31390ca2-a272-434d-9242-627b92b6ba5c	2026-09-25 23:26:35.742093+08
notif_eb2a777daaa0	Listing Removed	The Pinecrest Glasshouse Sanctuary has been removed from active listings in PostgreSQL. 1 related viewing appointment(s) were cancelled.	Just now	LISTING_UPDATE	f	prop_pinecrest	2026-09-25 23:30:03.209657+08
notif_6dda47b973a2	Listing Removed	Skyline Penthouse at The Zenith has been removed from active listings in PostgreSQL. 1 related viewing appointment(s) were cancelled.	Just now	LISTING_UPDATE	f	prop_skyline	2026-09-25 23:30:06.364244+08
notif_50e96d712cbc	Viewing Appointment Booked	Viewing for Cedar Heights Luxury Brownstone confirmed on Tue, Oct 15 at 10:00 AM.	Just now	VIEWING_BOOKED	f	3f9f4204-786b-4dbf-9f3a-a2539a48d2c6	2026-09-25 23:31:25.794067+08
notif_f578d5ef3b50	Listing Removed	The Industrial Foundry Modern Loft has been removed from active listings in PostgreSQL.	Just now	LISTING_UPDATE	f	prop_urban_loft	2026-09-30 20:49:22.832+08
notif_b2b6580dd312	New Property Published	sample is now live on Estateflow with active viewing bookings.	Just now	LISTING_UPDATE	f	4d20ca18-1441-477f-b252-1878978b428d	2026-09-30 22:42:53.551911+08
notif_2ac8e1e319f1	Listing Removed	sample has been removed from active listings in PostgreSQL.	Just now	LISTING_UPDATE	f	4d20ca18-1441-477f-b252-1878978b428d	2026-09-30 22:55:35.651548+08
notif_33fb05358354	New Property Published	Maves is now live on Estateflow with active viewing bookings.	Just now	LISTING_UPDATE	f	f70c02d0-b09b-484d-a3f6-1cb2c7e19695	2026-10-07 08:25:18.743519+08
notif_ad027bfff64e	New Lead Inquiry	Maverick Teves submitted an inquiry for Sunny Meadows Craftsman Estate.	Just now	NEW_INQUIRY	f	749a68be-4381-45e4-b096-9640908792bd	2026-10-07 09:03:32.15181+08
notif_2cfb2bcdf15a	Viewing Appointment Booked	Viewing for Cedar Heights Luxury Brownstone confirmed on Tue, Oct 15 at 10:00 AM.	Just now	VIEWING_BOOKED	f	dc89f97f-5f5d-413a-8709-36c65db0c35c	2026-10-07 09:28:18.647028+08
notif_d1f7757c4f91	Viewing Appointment Booked	Viewing for Sunny Meadows Craftsman Estate confirmed on Sat, Oct 19 at 11:30 AM.	Just now	VIEWING_BOOKED	f	6f3ee7cf-8d5a-49bc-ab0a-5eb41b857468	2026-10-07 10:24:12.348314+08
notif_fcaa84151d23	New Property Published	sample is now live on Estateflow with active viewing bookings.	Just now	LISTING_UPDATE	f	d98f479a-ab4f-48e9-8f6a-241f9c2e1939	2026-10-07 10:25:14.031009+08
notif_aabe04422b51	Viewing Appointment Booked	Viewing for Sunny Meadows Craftsman Estate confirmed on Sat, Oct 19 at 11:30 AM.	Just now	VIEWING_BOOKED	f	c8ceae96-df1d-413d-8457-8264d6d89bbb	2026-10-07 11:58:46.792171+08
notif_163597423a64	Viewing Appointment Booked	Viewing for Sunny Meadows Craftsman Estate confirmed on Sat, Oct 19 at 11:30 AM.	Just now	VIEWING_BOOKED	f	04ac3e1c-cae0-469d-bc63-0419b96aace2	2026-10-08 01:48:07.393082+08
\.


--
-- Data for Name: properties; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.properties (id, title, address, city_state_zip, price, price_formatted, is_rental, beds, baths, sqft, property_type, description, image_res_id, media_uris, is_favorite, status, available_dates, available_time_slots, amenities, year_built, agent_name, agent_title, agent_phone, agent_email, created_at, updated_at) FROM stdin;
prop_sunny_meadows	Sunny Meadows Craftsman Estate	1240 Whispering Pines Lane	Austin, TX 78704	1280000	₱1,280,000	f	4.0	3.5	3100	Single Family	Warm, welcoming modern Craftsman nestled on a lush quarter-acre lot with majestic live oaks. Features wrap-around mahogany porch, white oak wide-plank flooring, primary spa suite, and a screened sunroom overlooking the manicured grounds.	0	["android.resource://com.example/drawable/property_sunny_meadows_1787131753558"]	f	Active	["Mon, Oct 14", "Thu, Oct 17", "Sat, Oct 19"]	["9:30 AM", "11:30 AM", "2:00 PM", "4:00 PM"]	["Sprawling Garden", "Covered Patio & Outdoor BBQ", "Home Office Suite", "EV Ready 2-Car Garage"]	2021	Sarah Jenkins	Premier Real Estate Broker • Cebu City Properties	+63 917 555 0192	sarah.jenkins@estateflow.com	2026-09-25 21:22:38.823752+08	2026-09-25 21:22:38.823752+08
prop_cedar_townhouse	Cedar Heights Luxury Brownstone	518 Montgomery St, Unit 3	Jersey City, NJ 07302	6500	₱6,500 / mo	t	3.0	2.5	1950	Townhouse	Spectacular modern brownstone residence available for executive lease. Tri-level living with private landscaped bluestone courtyard garden, bespoke millwork, gas fireplace, and seamless 12-minute transit into Lower Manhattan.	0	["android.resource://com.example/drawable/property_townhouse_1787131686098"]	f	Active	["Tue, Oct 15", "Wed, Oct 16", "Fri, Oct 18"]	["10:00 AM", "12:00 PM", "3:00 PM", "5:30 PM"]	["Private Garden Courtyard", "Modern Chef Kitchen", "Gas Fireplace", "Smart Security", "Walk to PATH Station"]	2022	Sarah Jenkins	Premier Real Estate Broker • Cebu City Properties	+63 917 555 0192	sarah.jenkins@estateflow.com	2026-09-25 21:22:38.823752+08	2026-09-25 21:22:38.823752+08
d98f479a-ab4f-48e9-8f6a-241f9c2e1939	sample	sample	Cebu City, Cebu • 6000	9000	₱9,000	f	3.0	2.5	1500	Houses	Contemporary architectural sanctuary with designer touches, high efficiency appliances, and prime central location in Cebu City.	0	["content:\\/\\/media\\/picker\\/0\\/com.android.providers.media.photopicker\\/media\\/1000170132"]	f	New Listing	["Mon, Oct 14","Tue, Oct 15","Wed, Oct 16","Thu, Oct 17"]	["10:00 AM","1:30 PM","4:00 PM","5:30 PM"]	["Rooftop Deck","Garage Parking","Central AC"]	2021	Sarah Jenkins	Premier Real Estate Broker • Cebu City Properties	+63 917 555 0192	sarah.jenkins@estateflow.com	2026-10-07 10:25:14.027261+08	2026-10-07 10:25:14.027261+08
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, name, email, phone, role, license_no, agency, professional_role, password_hash, created_at, updated_at) FROM stdin;
agent_verified_sarah	Sarah Jenkins	sarah.jenkins@estateflow.com	+63 917 555 0192	AGENT	PRC-REB-0094821	Estateflow Luxury Realty	Premier Real Estate Broker	$2y$10$TcyIP70/GCakorNWPAfg6uG2Us5AWCE0mEPIwUnLA8zCz5.Qh0haq	2026-09-25 21:22:38.823752+08	2026-09-25 23:45:19.598019+08
client_ef65b63e	Alex Turner	alex.turner@example.com	+1 (555) 019-8234	BUYER			Licensed Agent		2026-09-30 22:57:55.930569+08	2026-09-30 22:57:55.930569+08
client_0bc7e014	Maverick Teves	tevesmaverick@gmail.com	09502291607	BUYER			Licensed Agent	$2y$10$NAo0kxMK9kabYYLhxOSMouc98deC24qsaTOWeDwKPFa9TFPiCGwh.	2026-10-07 09:02:08.314231+08	2026-10-07 09:02:08.314231+08
\.


--
-- Data for Name: viewing_appointments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.viewing_appointments (id, property_id, property_title, property_address, client_name, client_phone, client_email, date, time_slot, type, status, notes, created_timestamp, created_at) FROM stdin;
appt_demo_1	\N	The Pinecrest Glasshouse Sanctuary	88 High Ridge Way, Beverly Crest	David Miller	+1 (555) 234-5678	david.miller@example.com	Mon, Oct 14	1:30 PM	Viewing	CANCELLED	Pre-approved luxury buyer requesting private walk-through of the master wing and wine cellar. [Listing "The Pinecrest Glasshouse Sanctuary" was removed by agent. Viewing cancelled.]	1790342558824	2026-09-25 21:22:38.823752+08
appt_demo_2	\N	Skyline Penthouse at The Zenith	450 Marina Boulevard, Penthouse 42B	Sophia Chen	+1 (555) 876-5432	sophia.chen@example.com	Wed, Oct 16	4:30 PM	Viewing	CANCELLED	Client wants sunset viewing to assess evening bay lighting. [Listing "Skyline Penthouse at The Zenith" was removed by agent. Viewing cancelled.]	1790342558824	2026-09-25 21:22:38.823752+08
04ac3e1c-cae0-469d-bc63-0419b96aace2	prop_sunny_meadows	Sunny Meadows Craftsman Estate	1240 Whispering Pines Lane	Alex Turner	+1 (555) 019-8234	alex.turner@example.com	Sat, Oct 19	11:30 AM	Viewing	UPCOMING	Scheduled via Estateflow instant booking.	1791395286377	2026-10-08 01:48:07.371524+08
\.


--
-- Name: agent_profiles agent_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_profiles
    ADD CONSTRAINT agent_profiles_pkey PRIMARY KEY (id);


--
-- Name: inquiries inquiries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inquiries
    ADD CONSTRAINT inquiries_pkey PRIMARY KEY (id);


--
-- Name: inquiry_messages inquiry_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inquiry_messages
    ADD CONSTRAINT inquiry_messages_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: properties properties_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.properties
    ADD CONSTRAINT properties_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: viewing_appointments viewing_appointments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.viewing_appointments
    ADD CONSTRAINT viewing_appointments_pkey PRIMARY KEY (id);


--
-- Name: idx_appointments_client_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointments_client_email ON public.viewing_appointments USING btree (client_email);


--
-- Name: idx_appointments_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointments_status ON public.viewing_appointments USING btree (status);


--
-- Name: idx_inquiry_messages_inquiry_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inquiry_messages_inquiry_id ON public.inquiry_messages USING btree (inquiry_id);


--
-- Name: idx_notifications_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (is_read);


--
-- Name: idx_properties_is_rental; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_properties_is_rental ON public.properties USING btree (is_rental);


--
-- Name: idx_properties_price; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_properties_price ON public.properties USING btree (price);


--
-- Name: idx_properties_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_properties_type ON public.properties USING btree (property_type);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: inquiries inquiries_property_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inquiries
    ADD CONSTRAINT inquiries_property_id_fkey FOREIGN KEY (property_id) REFERENCES public.properties(id) ON DELETE SET NULL;


--
-- Name: inquiry_messages inquiry_messages_inquiry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inquiry_messages
    ADD CONSTRAINT inquiry_messages_inquiry_id_fkey FOREIGN KEY (inquiry_id) REFERENCES public.inquiries(id) ON DELETE CASCADE;


--
-- Name: viewing_appointments viewing_appointments_property_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.viewing_appointments
    ADD CONSTRAINT viewing_appointments_property_id_fkey FOREIGN KEY (property_id) REFERENCES public.properties(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict x9NCiG0rPdwWBfeyq9y4Diy43ZWHVHwZqHh4aBGfgxr3L9TUwjOmeJWxgbzRCfo

