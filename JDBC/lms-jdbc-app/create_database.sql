-- Run this ONCE in PostgreSQL before launching the app, e.g.:
--   psql -U postgres -f create_database.sql
-- or paste it into pgAdmin's Query Tool while connected to the default 'postgres' database.
--
-- The application itself creates all TABLES automatically on first run.
-- This script only creates the DATABASE, since PostgreSQL cannot create
-- the database it is currently connected to from within Java/JDBC.

CREATE DATABASE lms_db;
