DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS vote;
DROP TABLE IF EXISTS thread;
DROP TABLE IF EXISTS forum;
DROP TABLE IF EXISTS users;

DROP INDEX IF EXISTS indexUserNickname;
DROP INDEX IF EXISTS indexForumSlug;
DROP INDEX IF EXISTS indexThreadSlug;
DROP INDEX IF EXISTS indexPostParentThread;
DROP INDEX IF EXISTS indexPostThread;
DROP INDEX IF EXISTS indexVoteIdNickname;
DROP INDEX IF EXISTS indexVoteId;
DROP INDEX IF EXISTS indexThreadForum;

CREATE EXTENSION IF NOT EXISTS citext;

SET synchronous_commit = off;

CREATE SEQUENCE IF NOT EXISTS public.forum_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.posts_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.thread_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.users_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE TABLE forum
(
    id INTEGER DEFAULT nextval('forum_id_seq'::regclass) PRIMARY KEY NOT NULL,
    title varchar(255) NOT NULL,
    "user" varchar(255) NOT NULL,
    slug varchar(255) NOT NULL,
    posts INTEGER,
    threads INTEGER
);


CREATE TABLE post
(
    id INTEGER DEFAULT nextval('posts_id_seq'::regclass) PRIMARY KEY NOT NULL,
    parent INTEGER default 0,
    author varchar(255),
    message text,
    isEdited BOOLEAN,
    forum varchar(255) NOT NULL,
    thread INTEGER,
    created TIMESTAMPTZ default now(),
    forTreeSort INTEGER[] DEFAULT '{}'::INTEGER[]
);

CREATE TABLE thread
(
    id INTEGER DEFAULT nextval('thread_id_seq'::regclass) PRIMARY KEY NOT NULL,
    title varchar(255) not null,
    author varchar(255),
    forum varchar(255) not null,
    message TEXT,
    votes INTEGER,
    slug varchar(255),
    created TIMESTAMPTZ
);

CREATE TABLE users
(
    id INTEGER DEFAULT nextval('users_id_seq'::regclass) PRIMARY KEY NOT NULL,
    nickname CITEXT collate ucs_basic NOT NULL UNIQUE,
    fullname varchar(255) NOT NULL,
    about TEXT,
    email varchar(255) unique not null
);


CREATE TABLE vote
(
    nickname VARCHAR(255) NOT NULL,
    voice INTEGER NOT NULL,
    id INTEGER
);
CREATE INDEX indexUserNickname ON users (LOWER(nickname));
CREATE INDEX indexForumSlug ON forum (Lower(slug));
CREATE INDEX indexThreadForum ON thread (LOWER(forum));
CREATE INDEX indexThreadSlug ON thread (LOWER(slug));
CREATE INDEX indexPostThread ON post (thread ASC);
CREATE INDEX indexVoteIdNickname ON vote (id ASC, LOWER(nickname));
CREATE INDEX indexVoteId ON vote (id ASC, LOWER(nickname));