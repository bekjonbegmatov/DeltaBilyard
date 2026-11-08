CREATE TABLE players (id BIGINT PRIMARY KEY, name TEXT NOT NULL, username TEXT, avatar TEXT, bot_started BOOLEAN NOT NULL DEFAULT FALSE, updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX players_username ON players(lower(username));
CREATE TABLE relationships (sender BIGINT REFERENCES players(id), recipient BIGINT REFERENCES players(id), status TEXT NOT NULL CHECK(status IN ('pending','accepted','blocked')), PRIMARY KEY(sender,recipient), CHECK(sender<>recipient));
CREATE TABLE rooms (code VARCHAR(4) PRIMARY KEY, state JSONB NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE matches (id UUID PRIMARY KEY, room_code VARCHAR(4), state JSONB NOT NULL, finished_at TIMESTAMPTZ NOT NULL DEFAULT now());
