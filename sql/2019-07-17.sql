----------------------------------------------------------------------------------------------------

CREATE TABLE public.configuration_data (

    id    uuid NOT NULL,
    data bytea NOT NULL,

    CONSTRAINT configuration_data_pk PRIMARY KEY (id)

) WITH (OIDS = FALSE);

ALTER TABLE public.configuration_data OWNER to e2;

----------------------------------------------------------------------------------------------------

CREATE TABLE public.configuration_info (

    id             uuid     NOT NULL,
    version     integer     NOT NULL,

    size         bigint     NOT NULL DEFAULT 0,
    sha            char(40) NOT NULL,
    created timestamptz     NOT NULL,

    CONSTRAINT configuration_info_pk PRIMARY KEY (id)

) WITH (OIDS = FALSE);

ALTER TABLE public.configuration_info OWNER to e2;

----------------------------------------------------------------------------------------------------

CREATE TABLE public.configuration_reference (

    id                           uuid      NOT NULL,
    version                   integer      NOT NULL,

    configuration_info_id        uuid      NOT NULL,
    file_name                 varchar(255) NOT NULL,
    created               timestamptz      NOT NULL,

    CONSTRAINT configuration_reference_pk PRIMARY KEY (id),
    CONSTRAINT configuration_reference_configuration_info_id_fk FOREIGN KEY (configuration_info_id) REFERENCES public.configuration_info (id)

) WITH (OIDS = FALSE);

CREATE INDEX configuration_reference_configuration_info_id_idx ON
    public.configuration_reference(configuration_info_id);

ALTER TABLE public.configuration_info OWNER to e2;

----------------------------------------------------------------------------------------------------
