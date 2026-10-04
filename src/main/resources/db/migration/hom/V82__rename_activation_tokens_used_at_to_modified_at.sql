-- A token is "used" once modified_at is set (it stays NULL until the token is consumed).
ALTER TABLE activation_tokens RENAME COLUMN used_at TO modified_at;
