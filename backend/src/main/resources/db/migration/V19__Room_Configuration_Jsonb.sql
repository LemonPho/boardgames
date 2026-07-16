-- Replace the single tracking_mode column with an expandable JSONB configuration
-- blob (per-room game settings: trackingMode, advancedCards, and future options).
-- The application always writes this on room creation, so no DB default is needed.

ALTER TABLE rooms ADD COLUMN configuration JSONB NOT NULL;
ALTER TABLE rooms DROP COLUMN tracking_mode;
