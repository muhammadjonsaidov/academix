-- Email is an optional account identifier, but when present it must identify exactly one user.
-- The expression index keeps email sign-in and password recovery case-insensitive.
CREATE UNIQUE INDEX ux_users_email_lower
    ON users (LOWER(email))
    WHERE email IS NOT NULL AND BTRIM(email) <> '';
