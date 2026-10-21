-- Profile of the Gravita staff allowed into the platform administration area (/admin).
-- It carries no screen permissions on purpose: the area is gated by the profile itself.
INSERT INTO profiles (id, name, active, created_at, modified_at)
VALUES (gen_random_uuid(), 'GRAVITA_ADMINISTRATOR', TRUE, now(), now())
ON CONFLICT (name) DO NOTHING;
