CREATE TABLE person (
	id integer NOT NULL PRIMARY KEY,
	sn varchar(30) NOT NULL,
	givenName varchar(20) NOT NULL,
	end_of_validity timestamp NOT NULL,
	general_title_id integer default NULL,
	manager_id integer NOT NULL,
	structure_id integer NOT NULL,
	location_id integer default NULL
);

CREATE INDEX person_manager ON person (manager_id);
CREATE INDEX person_structure ON person (structure_id);
CREATE INDEX person_location ON person (location_id);

CREATE TABLE structure (
	id integer NOT NULL PRIMARY KEY,
	code varchar(20) NOT NULL,
	description varchar(255) default NULL
);

CREATE TABLE activity (
	id integer NOT NULL PRIMARY KEY,
	person_id integer NOT NULL,
	structure_id integer NOT NULL,
	title_id integer NOT NULL
);

CREATE INDEX activity_person ON activity (person_id);
CREATE INDEX activity_structure ON activity (structure_id);

CREATE TABLE title (
	id integer NOT NULL PRIMARY KEY,
	code varchar(20) NOT NULL,
	description varchar(255) default NULL
);

CREATE TABLE location (
	id integer NOT NULL PRIMARY KEY,
	street varchar(255) NOT NULL,
	postal_code integer NOT NULL,
	l varchar(255) NOT NULL
);

CREATE TABLE computer (
	id integer NOT NULL PRIMARY KEY,
	hostname varchar(255) NOT NULL,
	end_of_validity timestamp NOT NULL,
	password varchar(255) NOT NULL
);

COMMIT;
SHUTDOWN;
