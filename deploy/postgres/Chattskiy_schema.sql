-- public.chat definition

-- Drop table

-- DROP TABLE public.chat;

CREATE TABLE IF NOT EXISTS public.chat (
                             id uuid NOT NULL,
                             "name" varchar NULL,
                             CONSTRAINT chat_pk PRIMARY KEY (id)
);

-- public."user" definition

-- Drop table

-- DROP TABLE public."user";

CREATE TABLE IF NOT EXISTS public."user" (
                               id uuid NOT NULL,
                               username varchar NOT NULL,
                               "password" varchar NULL,
                               "role" varchar NULL,
                               CONSTRAINT user_pk PRIMARY KEY (id),
                               CONSTRAINT user_unique UNIQUE (username)
);

-- public.chat_user definition

-- Drop table

-- DROP TABLE public.chat_user;

CREATE TABLE IF NOT EXISTS public.chat_user (
                                  chat_id uuid NOT NULL,
                                  user_id uuid NOT NULL,
                                  CONSTRAINT chat_user_pk PRIMARY KEY (chat_id, user_id)
);


-- public.chat_user foreign keys

ALTER TABLE public.chat_user ADD CONSTRAINT chat_user_chat_fk FOREIGN KEY (chat_id) REFERENCES public.chat(id);
ALTER TABLE public.chat_user ADD CONSTRAINT chat_user_user_fk FOREIGN KEY (user_id) REFERENCES public."user"(id);
