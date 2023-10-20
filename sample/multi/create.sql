CREATE ROLE lsc LOGIN
  ENCRYPTED PASSWORD 'md5809ead1da2f082b19e643d95a616110f'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE;
  
CREATE DATABASE lsc
  WITH OWNER = lsc
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       CONNECTION LIMIT = -1;

\c lsc

CREATE SEQUENCE public.inetorgperson_pkey
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

ALTER TABLE public.inetorgperson_pkey OWNER TO lsc;

CREATE TABLE public.inetorgperson
(
  uid character varying(255) NOT NULL,
  sn character varying(255) NOT NULL,
  givenname character varying(255),
  cn character varying(512) NOT NULL,
  mail character varying(255) NOT NULL,
  address character varying(512),
  telephonenumber character varying(255),
  id bigint NOT NULL DEFAULT nextval('inetorgperson_pkey'::regclass),
  CONSTRAINT inetorgperson_pkey1 PRIMARY KEY (id)
);

GRANT ALL ON TABLE public.inetorgperson TO lsc;
