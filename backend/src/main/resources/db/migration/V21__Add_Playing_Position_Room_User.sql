-- Explicit turn/seat order for a room's players. Until now order was derived
-- from joined_at; this lets the admin reorder players, and it's the single
-- ordering key the game uses (first-round leader, round rotation, display).

-- 1. Add nullable so existing rows can be backfilled before the NOT NULL flip.
ALTER TABLE rooms_users ADD COLUMN playing_position INT;

-- 2. Backfill: 0-based rank within each room, seeding the order from join time
--    (ties broken by id for determinism), so current rooms keep their order.
UPDATE rooms_users ru
SET playing_position = ranked.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY room_id ORDER BY joined_at ASC, id ASC
    ) - 1 AS rn
    FROM rooms_users
) AS ranked
WHERE ru.id = ranked.id;

-- 3. Now that every row has a value, enforce presence (app sets it on insert).
ALTER TABLE rooms_users ALTER COLUMN playing_position SET NOT NULL;
