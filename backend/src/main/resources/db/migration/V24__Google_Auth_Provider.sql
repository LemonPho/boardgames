-- Google sign-in. An account can now be created without a password, so
-- password_hash becomes nullable:
--   auth_provider : how the account was originally created (LOCAL | GOOGLE)
--   google_sub    : Google's immutable subject id, the real identity key
--                   (an email can change hands; the sub can't)
-- Existing rows are all password accounts, so they backfill to LOCAL.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255) UNIQUE;
