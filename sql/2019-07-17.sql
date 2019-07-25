----------------------------------------------------------------------------------------------------

CREATE TABLE public.configuration_data (

    id    uuid NOT NULL,
    data bytea NOT NULL,

    CONSTRAINT configuration_data_pk PRIMARY KEY (id)

) WITH (OIDS = FALSE);

ALTER TABLE public.configuration_data OWNER to e2;

----------------------------------------------------------------------------------------------------

CREATE TABLE public.configuration_info (

    id               uuid     NOT NULL,
    version       integer     NOT NULL,

    size           bigint     NOT NULL DEFAULT 0,
    sha              char(40) NOT NULL,
    timestamp timestamptz     NOT NULL,

    CONSTRAINT configuration_info_pk PRIMARY KEY (id)

) WITH (OIDS = FALSE);

ALTER TABLE public.configuration_info OWNER to e2;

----------------------------------------------------------------------------------------------------

CREATE TABLE public.configuration_reference (

    id                           uuid      NOT NULL,
    version                   integer      NOT NULL,

    configuration_info_id        uuid      NOT NULL,
    file_name                 varchar(255) NOT NULL,
    timestamp             timestamptz      NOT NULL,

    CONSTRAINT configuration_reference_pk PRIMARY KEY (id),
    CONSTRAINT configuration_reference_configuration_info_id_fk FOREIGN KEY (configuration_info_id) REFERENCES public.configuration_info (id)

) WITH (OIDS = FALSE);

CREATE INDEX configuration_reference_configuration_info_id_idx ON
    public.configuration_reference(configuration_info_id);

ALTER TABLE public.configuration_info OWNER to e2;

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

DROP TABLE public.route_request;
DROP TABLE public.route_update;

----------------------------------------------------------------------------------------------------

DROP TABLE public.system_instance;

CREATE TABLE public.system_instance (

    id         uuid      NOT NULL,
    version integer      NOT NULL,
    name    varchar(255)     NULL COLLATE pg_catalog."default",

    CONSTRAINT system_instance_pk PRIMARY KEY (id)

) WITH (OIDS = FALSE);

ALTER TABLE public.system_instance OWNER to e2;

----------------------------------------------------------------------------------------------------

DROP TABLE public.system_type;

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
-- Превратим все таймстемпы без зоны в таймстемпы с зоной
----------------------------------------------------------------------------------------------------

ALTER TABLE error4conversion ALTER COLUMN timestamp TYPE timestamptz;
ALTER TABLE error4delivery   ALTER COLUMN timestamp TYPE timestamptz;
ALTER TABLE error4repeat     ALTER COLUMN timestamp TYPE timestamptz;
ALTER TABLE history4delivery ALTER COLUMN timestamp TYPE timestamptz;

ALTER TABLE queue4conversion ALTER COLUMN timestamp TYPE timestamptz;
ALTER TABLE queue4conversion ALTER COLUMN delivered_timestamp TYPE timestamptz;

ALTER TABLE queue4delivery   ALTER COLUMN timestamp TYPE timestamptz;
ALTER TABLE queue4delivery   ALTER COLUMN delivered_timestamp TYPE timestamptz;

ALTER TABLE queue4repeat     ALTER COLUMN timestamp TYPE timestamptz;
ALTER TABLE queue4repeat     ALTER COLUMN delivered_timestamp TYPE timestamptz;

----------------------------------------------------------------------------------------------------
