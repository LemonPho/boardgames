CREATE TYPE room_status AS ENUM (
    'WAITING',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE tracking_mode AS ENUM (
    'ADMIN',
    'SELF'
);

CREATE TYPE rooms_user_role AS ENUM (
    'ADMIN',
    'PLAYER',
    'ANONYMOUS'
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id),
    name VARCHAR(255) NOT NULL UNIQUE,
    status room_status NOT NULL DEFAULT 'WAITING',
    tracking_mode tracking_mode NOT NULL DEFAULT 'ADMIN',
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE rooms_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id),
    user_id UUID REFERENCES users(id),
    display_name VARCHAR(255),
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    role rooms_user_role NOT NULL DEFAULT 'PLAYER',
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT user_or_anonymous CHECK (
        (user_id IS NOT NULL AND is_anonymous = FALSE) OR
        (user_id IS NULL AND is_anonymous = TRUE AND display_name IS NOT NULL)
    )
);