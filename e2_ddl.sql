---------------------------------------------------------------------------------------------

CREATE TABLE public.system_instance (
	id           uuid         NOT NULL,
	"version"    int4         NOT NULL,
	name         varchar(255)     NULL,

	CONSTRAINT system_instance_pk PRIMARY KEY (id)
);

---------------------------------------------------------------------------------------------

CREATE TABLE public.queue4conversion (
	id                  uuid      NOT NULL,
	"version"           int4      NOT NULL,
	"timestamp"         timestamp NOT NULL,
	message             text          NULL,
	"size"              int8      NOT NULL DEFAULT 0,
	attempt             int4      NOT NULL DEFAULT 0,
	processed           bool      NOT NULL DEFAULT false,
	delivered_timestamp timestamp     NULL,
	stuck               bool      NOT NULL DEFAULT false,

	CONSTRAINT queue4input_pk PRIMARY KEY (id),
	CONSTRAINT queue4conversion_timestamp_unique UNIQUE ("timestamp")
);

CREATE UNIQUE INDEX queue4conversion_order_idx ON queue4conversion USING btree (processed, "timestamp");

---------------------------------------------------------------------------------------------

CREATE TABLE public.error4conversion (
	id          uuid      NOT NULL,
	"version"   int4      NOT NULL,
	"timestamp" timestamp     NULL,
	queue_id    uuid          NULL,
	error       text          NULL,
	stack_trace text          NULL,

	CONSTRAINT error4conversion_pk PRIMARY KEY (id),
	CONSTRAINT error4conversion_queue_id_fk FOREIGN KEY (queue_id) REFERENCES queue4conversion(id)
);

---------------------------------------------------------------------------------------------

CREATE TABLE public.queue4delivery (
	id                  uuid      NOT NULL,
	"version"           int4      NOT NULL,
	"timestamp"         timestamp NOT NULL,
	message             text          NULL,
	"size"              int8      NOT NULL DEFAULT 0,
	destination_id      uuid          NULL,
	source_message_id   uuid          NULL,
	attempt             int4      NOT NULL DEFAULT 0,
	processed           bool      NOT NULL DEFAULT false,
	delivered_timestamp timestamp     NULL,
	stuck               bool      NOT NULL DEFAULT false,

	CONSTRAINT queue4output_pk PRIMARY KEY (id),
	CONSTRAINT queue4delivery_timestamp_unique UNIQUE ("timestamp"),
	CONSTRAINT queue4delivery_destination_id_fk FOREIGN KEY (destination_id) REFERENCES system_instance(id)
);

---------------------------------------------------------------------------------------------

CREATE TABLE public.error4delivery (
	id          uuid      NOT NULL,
	"version"   int4      NOT NULL,
	"timestamp" timestamp     NULL,
	queue_id    uuid          NULL,
	error       text          NULL,
	stack_trace text          NULL,

	CONSTRAINT error4delivery_pk PRIMARY KEY (id),
	CONSTRAINT error4delivery_queue_id_fk FOREIGN KEY (queue_id) REFERENCES queue4delivery(id)
);

---------------------------------------------------------------------------------------------

CREATE TABLE public.history4delivery (
	id uuid NOT NULL,
	"version" int4 NOT NULL,
	"timestamp" timestamp NOT NULL,
	queue_id uuid NULL,
	message text NULL,
	"size" int8 NOT NULL DEFAULT 0,

	CONSTRAINT history4delivery_pk PRIMARY KEY (id),
	CONSTRAINT history4delivery_timestamp_unique UNIQUE ("timestamp"),
	CONSTRAINT history4delivery_queue_id_fk FOREIGN KEY (queue_id) REFERENCES queue4delivery(id)
);

---------------------------------------------------------------------------------------------

CREATE TABLE public.queue4repeat (
	id                  uuid      NOT NULL,
	"version"           int4      NOT NULL,
	"timestamp"         timestamp NOT NULL,
	message             text          NULL,
	"size"              int8      NOT NULL DEFAULT 0,
	destination_id      uuid          NULL,
	source_message_id   uuid          NULL,
	attempt             int4      NOT NULL DEFAULT 0,
	processed           bool      NOT NULL DEFAULT false,
	delivered_timestamp timestamp     NULL,
	stuck               bool      NOT NULL DEFAULT false,

	CONSTRAINT queue4repeat_pk PRIMARY KEY (id),
	CONSTRAINT queue4repeat_timestamp_unique UNIQUE ("timestamp"),
	CONSTRAINT queue4repeat_destination_id_fk FOREIGN KEY (destination_id) REFERENCES system_instance(id)
);

---------------------------------------------------------------------------------------------

CREATE TABLE public.error4repeat (
	id          uuid      NOT NULL,
	"version"   int4      NOT NULL,
	"timestamp" timestamp     NULL,
	queue_id    uuid          NULL,
	error       text          NULL,
	stack_trace text          NULL,

	CONSTRAINT error4repeat_pk PRIMARY KEY (id),
	CONSTRAINT error4repeat_queue_id_fk FOREIGN KEY (queue_id) REFERENCES queue4repeat(id)
);

---------------------------------------------------------------------------------------------
