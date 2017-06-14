SET synchronous_commit = OFF;

DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS vote;
DROP TABLE IF EXISTS thread;
DROP TABLE IF EXISTS forum;
DROP TABLE IF EXISTS users;

DROP INDEX IF EXISTS indexUserNicknamelower;
DROP INDEX IF EXISTS indexUserEmaillower;
DROP INDEX IF EXISTS indexForumSlug;
DROP INDEX IF EXISTS indexForumSluglower;
DROP INDEX IF EXISTS indexThreadSlug;
DROP INDEX IF EXISTS indexPostThread;
DROP INDEX IF EXISTS indexVoteIdNickname;
DROP INDEX IF EXISTS indexThreadForum;
DROP INDEX IF EXISTS indexUserEmail;
DROP INDEX IF EXISTS indexForumONFU;
DROP INDEX IF EXISTS indexUserONFU;
DROP INDEX IF EXISTS indexPostParentThread;
CREATE EXTENSION IF NOT EXISTS citext;


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
    "user" text NOT NULL,
    slug text NOT NULL unique,
    posts INTEGER,
    threads INTEGER
);
CREATE INDEX indexForumSluglower ON forum (Lower(slug));
CREATE INDEX indexForumSlug ON forum (slug);
CREATE TABLE users
(
    id INTEGER DEFAULT nextval('users_id_seq'::regclass) PRIMARY KEY NOT NULL,
    nickname CITEXT collate ucs_basic NOT NULL UNIQUE,
    fullname varchar(255) NOT NULL,
    about TEXT,
    email varchar(255) unique not null
);
CREATE INDEX indexUserEmaillower ON users (LOWER(email));
CREATE INDEX indexUserNicknamelower ON users (LOWER(nickname));
CREATE TABLE thread
(
    id INTEGER DEFAULT nextval('thread_id_seq'::regclass) PRIMARY KEY NOT NULL,
    title varchar(255) not null,
    author citext collate ucs_basic REFERENCES users(nickname),
    forum Text not null references forum(slug),
    message TEXT,
    votes INTEGER,
    slug TEXT,
    created TIMESTAMPTZ
);

CREATE INDEX indexThreadForum ON thread (LOWER(forum));
CREATE INDEX indexThreadSlug ON thread (LOWER(slug));


CREATE TABLE post
(
    id INTEGER DEFAULT nextval('posts_id_seq'::regclass) PRIMARY KEY NOT NULL,
    parent INTEGER default 0,
    author varchar(255),
    message text,
    isEdited BOOLEAN,
    forum text NOT NULL,
    thread INTEGER references thread(id),

    created TIMESTAMPTZ default now(),
    forTreeSort INTEGER[] DEFAULT '{}'::INTEGER[]
);
CREATE INDEX indexPostThread ON post (thread ASC);


CREATE TABLE vote
(
    nickname VARCHAR(255) NOT NULL,
    voice INTEGER NOT NULL,
    id INTEGER
);
CREATE INDEX indexVoteIdNickname ON vote (id, LOWER(nickname));

CREATE TABLE IF NOT EXISTS forumUser (
  "user" CITEXT,
  forum CITEXT,
  UNIQUE("user", forum)
);

CREATE INDEX IF NOT EXISTS indexUserOnFU ON forumUser ("user");
CREATE INDEX IF NOT EXISTS  indexForumONFU ON forumUser (forum);

CREATE FUNCTION insertFU() RETURNS trigger AS $$
    BEGIN
    INSERT INTO forumuser("user",forum) values (NEW.author,NEW.forum) ON CONFLICT DO NOTHING;
    return null;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER emp_stamp AFTER INSERT ON post
    FOR EACH ROW EXECUTE PROCEDURE insertFU();