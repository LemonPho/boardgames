ALTER TABLE rooms_users DROP COLUMN is_anonymous;
ALTER TABLE rooms_users DROP CONSTRAINT IF EXISTS user_or_anonymous;
ALTER TABLE rooms_users ADD CONSTRAINT user_or_anonymous CHECK (
    (user_id IS NOT NULL AND role != 'ANONYMOUS') OR
    (user_id IS NULL AND role = 'ANONYMOUS' AND display_name IS NOT NULL)
);