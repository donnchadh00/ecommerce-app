INSERT INTO users (email, password, role)
SELECT
  'guest@demo.local',
  '$2b$12$XH99AOFxPaWNF74hE4M7D.3UiX8q9OXHlsGtPGdmhSSd3emCQsw7a',
  'USER'
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE email = 'guest@demo.local'
);
