-- Expense Splitter — Phase 1 schema
-- Source of truth for entities/relationships: project-plan/03_SCHEMA.md
-- Loans/IOU tables intentionally excluded (Phase 2).

create extension if not exists pgcrypto;

create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

-- ============================================================== users ====
create table users (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  email text not null unique,
  avatar_url text,
  created_at timestamptz not null default now(),
  -- bcrypt hash — never store the raw PIN. Login is otherwise just
  -- "pick your name," so this is the only thing standing between the
  -- public API and full account access.
  pin_hash text,
  failed_login_attempts integer not null default 0,
  locked_until timestamptz
);

-- ========================================================= categories ====
create table categories (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  icon text,
  is_default boolean not null default false
);

-- ============================================================ budgets ====
create table budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references users(id) on delete cascade,
  category_id uuid not null references categories(id) on delete cascade,
  month int not null check (month between 1 and 12),
  year int not null check (year between 2000 and 2100),
  target_amount numeric(12, 2) not null check (target_amount >= 0)
);

-- user_id is nullable: null = group-level target. A plain UNIQUE constraint
-- treats NULLs as distinct, so it wouldn't stop two group-level rows for the
-- same category/month/year — use partial unique indexes instead.
create unique index budgets_personal_unique
  on budgets (user_id, category_id, month, year)
  where user_id is not null;

create unique index budgets_group_unique
  on budgets (category_id, month, year)
  where user_id is null;

-- =========================================================== expenses ====
create table expenses (
  id uuid primary key default gen_random_uuid(),
  amount numeric(12, 2) not null check (amount >= 0),
  currency varchar(3) not null default 'USD',
  date date not null,
  description text,
  notes text,
  category_id uuid references categories(id),
  paid_by uuid not null references users(id),
  is_shared boolean not null default false,
  receipt_photo_url text,
  source text not null default 'manual'
    check (source in ('manual', 'statement_upload', 'receipt_scan', 'recurring')),
  -- FK to statement_transactions added below (after that table exists) to
  -- break the circular reference between expenses <-> statement_transactions.
  source_transaction_id uuid,
  created_by uuid not null references users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create trigger expenses_set_updated_at
  before update on expenses
  for each row execute function set_updated_at();

create index expenses_date_idx on expenses (date);
create index expenses_category_idx on expenses (category_id);
create index expenses_paid_by_idx on expenses (paid_by);
create index expenses_is_shared_idx on expenses (is_shared);
create index expenses_deleted_at_idx on expenses (deleted_at);

-- ===================================================== expense_splits ====
-- Only populated when expenses.is_shared = true. Drives both the running
-- balance calculation and which users' personal budgets get debited.
create table expense_splits (
  id uuid primary key default gen_random_uuid(),
  expense_id uuid not null references expenses(id) on delete cascade,
  user_id uuid not null references users(id),
  split_type text not null
    check (split_type in ('equal', 'percentage', 'exact', 'shares')),
  amount_owed numeric(12, 2) not null check (amount_owed >= 0),
  unique (expense_id, user_id)
);

create index expense_splits_expense_idx on expense_splits (expense_id);
create index expense_splits_user_idx on expense_splits (user_id);

-- =================================================== expense_comments ====
create table expense_comments (
  id uuid primary key default gen_random_uuid(),
  expense_id uuid not null references expenses(id) on delete cascade,
  user_id uuid not null references users(id),
  comment text not null,
  created_at timestamptz not null default now()
);

create index expense_comments_expense_idx on expense_comments (expense_id);

-- ============================================== expense_edit_history ====
create table expense_edit_history (
  id uuid primary key default gen_random_uuid(),
  expense_id uuid not null references expenses(id) on delete cascade,
  edited_by uuid not null references users(id),
  field_changed text not null,
  old_value text,
  new_value text,
  edited_at timestamptz not null default now()
);

create index expense_edit_history_expense_idx on expense_edit_history (expense_id);

-- =================================================== recurring_expenses ====
create table recurring_expenses (
  id uuid primary key default gen_random_uuid(),
  amount numeric(12, 2) not null check (amount >= 0),
  currency varchar(3) not null default 'USD',
  category_id uuid references categories(id),
  description text,
  paid_by uuid not null references users(id),
  is_shared boolean not null default false,
  default_split_config jsonb,
  frequency text not null
    check (frequency in ('weekly', 'fortnightly', 'monthly', 'yearly')),
  next_run_date date not null,
  is_active boolean not null default true,
  created_by uuid not null references users(id),
  created_at timestamptz not null default now()
);

create index recurring_expenses_next_run_idx on recurring_expenses (next_run_date)
  where is_active = true;

-- ===================================================== statement_uploads ====
create table statement_uploads (
  id uuid primary key default gen_random_uuid(),
  uploaded_by uuid not null references users(id),
  file_url text not null,
  bank_name text,
  card_last4 varchar(4),
  upload_date timestamptz not null default now(),
  status text not null default 'processing'
    check (status in ('processing', 'needs_review', 'completed', 'failed')),
  -- Set right after parsing determines the transaction count, before the
  -- per-row categorization loop starts — lets clients show "X of Y" progress
  -- while status is still 'processing' instead of a bare spinner.
  expected_transaction_count integer
);

-- ================================================= statement_transactions ====
create table statement_transactions (
  id uuid primary key default gen_random_uuid(),
  statement_upload_id uuid not null references statement_uploads(id) on delete cascade,
  raw_date date,
  raw_description text,
  raw_amount numeric(12, 2),
  matched_category_id uuid references categories(id),
  needs_clarification boolean not null default false,
  user_clarification_note text,
  is_duplicate_of uuid references expenses(id),
  resolved_expense_id uuid references expenses(id),
  created_at timestamptz not null default now()
);

create index statement_transactions_upload_idx
  on statement_transactions (statement_upload_id);

-- Close the circular reference now that statement_transactions exists.
alter table expenses
  add constraint expenses_source_transaction_fk
  foreign key (source_transaction_id) references statement_transactions(id);
