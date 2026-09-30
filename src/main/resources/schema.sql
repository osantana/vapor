CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'administrator')),
    created_at TIMESTAMPTZ NOT NULL,
    failed_logins INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_logins INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    description TEXT DEFAULT '',
    system_requirements TEXT DEFAULT '',
    cover_url VARCHAR(500) DEFAULT '',
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    release_date DATE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    review_notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS genres (
    id UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS platforms (
    id UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS game_genres (
    game_id UUID NOT NULL REFERENCES games(id),
    genre_id UUID NOT NULL REFERENCES genres(id),
    PRIMARY KEY (game_id, genre_id)
);

CREATE TABLE IF NOT EXISTS game_platforms (
    game_id UUID NOT NULL REFERENCES games(id),
    platform_id UUID NOT NULL REFERENCES platforms(id),
    PRIMARY KEY (game_id, platform_id)
);

CREATE TABLE IF NOT EXISTS game_assets (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games(id),
    platform_id UUID NOT NULL REFERENCES platforms(id),
    file_size BIGINT NOT NULL,
    download_url VARCHAR(500) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    game_id UUID NOT NULL REFERENCES games(id),
    unit_price NUMERIC(10,2) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, game_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    total NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('credit_card', 'pix')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'paid', 'cancelled')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    game_id UUID NOT NULL REFERENCES games(id),
    price_paid NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS library_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    game_id UUID NOT NULL REFERENCES games(id),
    acquired_at TIMESTAMPTZ NOT NULL,
    minutes_played INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, game_id)
);

CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL UNIQUE REFERENCES order_items(id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('requested', 'approved', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL
);

INSERT INTO genres (id, name) VALUES
    (gen_random_uuid(), 'Action'),
    (gen_random_uuid(), 'Adventure'),
    (gen_random_uuid(), 'Puzzle'),
    (gen_random_uuid(), 'Racing'),
    (gen_random_uuid(), 'RPG'),
    (gen_random_uuid(), 'Simulation'),
    (gen_random_uuid(), 'Sports'),
    (gen_random_uuid(), 'Strategy')
ON CONFLICT (name) DO NOTHING;

INSERT INTO platforms (id, name) VALUES
    (gen_random_uuid(), 'Windows'),
    (gen_random_uuid(), 'macOS'),
    (gen_random_uuid(), 'Linux')
ON CONFLICT (name) DO NOTHING;
