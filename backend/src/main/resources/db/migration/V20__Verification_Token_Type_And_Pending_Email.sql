-- Verification tokens now serve two purposes, made explicit by `type`:
--   ACCOUNT_VERIFICATION - activates a newly-registered account
--   EMAIL_CHANGE          - applies `pending_email` to the user once confirmed
-- pending_email is only set for EMAIL_CHANGE tokens.
ALTER TABLE verification_tokens ADD COLUMN type VARCHAR(255) NOT NULL DEFAULT 'ACCOUNT_VERIFICATION';
ALTER TABLE verification_tokens ADD COLUMN pending_email VARCHAR(255);
