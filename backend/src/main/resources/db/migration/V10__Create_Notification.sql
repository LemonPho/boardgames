CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    read BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
)