-- Seed data: the two hardcoded users + a default category list.
-- Idempotent: safe to re-run against a fresh dev database.

insert into users (name, email) values
  ('Hemil', 'hemil@expensesplitter.local'),
  ('Heenal', 'heenal@expensesplitter.local')
on conflict (email) do nothing;

insert into categories (name, icon, is_default) values
  ('Groceries', '🛒', true),
  ('Eating Out', '🍽️', true),
  ('Rent/Utilities', '🏠', true),
  ('Transport', '🚗', true),
  ('Entertainment', '🎬', true),
  ('Shopping', '🛍️', true),
  ('Travel', '✈️', true),
  ('Health', '💊', true),
  ('Subscriptions', '🔁', true),
  ('Other', '🗂️', true)
on conflict (name) do nothing;
