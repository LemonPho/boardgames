-- V3__seed_games.sql
INSERT INTO games (id, name, type, game_config, scoring_config, min_players, max_players, description, created_at)
VALUES (
    gen_random_uuid(),
    'Skull King',
    'rounds',
    '{"total_rounds": 10, "deck_size": 66}',
    '{"hit_bid": 20, "miss_bid": -10, "zero_bid_success": 10, "zero_bid_miss": -10, "skull_king_bonus": 50, "pirate_bonus": 30, "mermaid_bonus": 20}',
    2,
    8,
    'A trick-taking game where players bid on the number of tricks they will win each round.',
    NOW()
);