-- ============================================================================
-- AcademiX AI — fresh-DB bootstrap ("pre-existing dev data" demo-seed.sql needs)
-- ============================================================================
-- demo-seed.sql deliberately builds ON TOP of base data it assumes already exists
-- (admin, school, the two teachers, psychologist, parent, Aziz/Jasur students,
-- 7-A class, Matematika/Fizika subjects, the badge catalog with FIXED ids). On a
-- fresh Postgres volume none of that exists, so Flyway-created tables are empty and
-- demo-seed.sql would fail on FKs. This file creates exactly that base layer.
--
-- Runs AFTER backend boot (Flyway must have created the tables first) — see the
-- `seed` service in docker-compose.yml, which depends on backend being healthy.
--
-- Idempotent: every statement is ON CONFLICT ... DO NOTHING, safe to re-run.
-- All passwords: Test1234!  (bcrypt hash below is copied from demo-seed.sql).
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Admin + school (admin resolves its school via schools.admin_id — V6 note)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, phone, email, password_hash, role, is_active, created_at)
VALUES ('a0000000-0000-4000-8000-000000000000', 'Test', 'Admin', '+998901234567',
        'admin@academixai.uz',
        '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS',
        'ADMIN', true, now() - interval '90 days')
ON CONFLICT (phone) DO NOTHING;

INSERT INTO schools (id, name, address, region, district, phone, email, admin_id,
                     total_classes, is_active, subscribed_at, subscription_ends_at,
                     monthly_ai_call_limit, current_month_ai_usage)
VALUES ('cc021f70-256f-45c0-be19-ab7beddab1b3', 'Ibn Sino xususiy maktabi',
        'Toshkent sh., Chilonzor tumani, Bunyodkor shoh koʻchasi 12', 'Toshkent', 'Chilonzor',
        '+998712345678', 'info@ibnsino.uz', 'a0000000-0000-4000-8000-000000000000',
        2, true, now() - interval '120 days', now() + interval '245 days',
        5000, 0)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Teachers + psychologist + parent (users.school_id for TEACHER/PSYCHOLOGIST)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, phone, password_hash, role, is_active, school_id, created_at) VALUES
  ('7c0adf23-9276-431a-bd18-5b4873eab945', 'Gulnora',  'Yusupova',     '+998911112233',
   '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'TEACHER', true,
   'cc021f70-256f-45c0-be19-ab7beddab1b3', now() - interval '80 days'),  -- Matematika, 7-A rahbari
  ('609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'Malika',   'Shodiyeva',    '+998912345678',
   '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'TEACHER', true,
   'cc021f70-256f-45c0-be19-ab7beddab1b3', now() - interval '78 days'),  -- Fizika, 8-B rahbari
  ('a31324b4-0bfa-4117-8dc7-d9679a9f3d7c', 'Nilufar',  'Psixolog',     '+998955501234',
   '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'PSYCHOLOGIST', true,
   'cc021f70-256f-45c0-be19-ab7beddab1b3', now() - interval '70 days'),
  ('a3ae17d5-c6ed-4014-809b-1e5fc8e292f3', 'Olim',     'Toshpulatov',  '+998977001122',
   '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'PARENT', true,
   NULL, now() - interval '75 days')  -- Aziz & Jasur ning ota-onasi
ON CONFLICT (phone) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Pre-existing students (Aziz/Jasur) + their profiles in 7-A
-- ---------------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, phone, password_hash, role, is_active, created_at) VALUES
  ('f2245ba6-6ee5-4161-bcc5-c22ac742459f', 'Aziz',  'Karimov',      '+998933334455',
   '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true,
   now() - interval '80 days'),
  ('0969224c-1348-4d49-91c3-034dcea56490', 'Jasur', 'Toshpulatov',  '+998971110033',
   '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true,
   now() - interval '80 days')
ON CONFLICT (phone) DO NOTHING;

-- 7-A sinf (Gulnora rahbarligida) — demo-seed buni UPDATE qiladi, INSERT emas
INSERT INTO school_classes (id, school_id, grade, letter, full_name, class_teacher_id,
                            student_count, academic_year, is_active)
VALUES ('1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
        7, 'A', '7-A', '7c0adf23-9276-431a-bd18-5b4873eab945', 5, '2025-2026', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO student_profiles (id, user_id, class_id, school_id, student_number, total_xp,
                              current_streak, max_streak, last_submission_date, is_active) VALUES
  ('b3000000-0000-4000-8000-000000000001', 'f2245ba6-6ee5-4161-bcc5-c22ac742459f',
   '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   '7A-001', 1450, 7, 10, current_date, true),   -- Aziz (demo-seed yanada oshiradi)
  ('b3000000-0000-4000-8000-000000000002', '0969224c-1348-4d49-91c3-034dcea56490',
   '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   '7A-002', 640, 3, 7, current_date - 1, true)  -- Jasur
ON CONFLICT (user_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Subjects (fixed ids demo-seed.sql's homework assignments reference)
-- ---------------------------------------------------------------------------
INSERT INTO subjects (id, school_id, name, type, icon) VALUES
  ('4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   'Matematika', 'MATH', '📐'),
  ('ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   'Fizika', 'PHYSICS', '⚡')
ON CONFLICT (id) DO NOTHING;

-- 7-A fan-o'qituvchi bog'lanishlari (demo-seed faqat 8-B/7-A Fizika uchun INSERT qiladi)
INSERT INTO class_subject_teachers (id, school_id, class_id, subject_id, teacher_id, academic_year) VALUES
  ('a4000000-0000-4000-8000-000000000004', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea',
   '7c0adf23-9276-431a-bd18-5b4873eab945', '2025-2026'),   -- Gulnora → 7-A Matematika
  ('a4000000-0000-4000-8000-000000000005', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd',
   '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', '2025-2026')    -- Malika → 7-A Fizika
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Parent → students (Aziz & Jasur) — SchoolContextResolver PARENT via links
-- ---------------------------------------------------------------------------
INSERT INTO parent_student_links (id, parent_user_id, student_user_id, relation, is_active,
                                  biometric_consent_given, consent_given_at) VALUES
  ('b4000000-0000-4000-8000-000000000001', 'a3ae17d5-c6ed-4014-809b-1e5fc8e292f3',
   'f2245ba6-6ee5-4161-bcc5-c22ac742459f', 'FATHER', true, false, NULL),
  ('b4000000-0000-4000-8000-000000000002', 'a3ae17d5-c6ed-4014-809b-1e5fc8e292f3',
   '0969224c-1348-4d49-91c3-034dcea56490', 'FATHER', true, false, NULL)
ON CONFLICT (parent_user_id, student_user_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Badge catalog with the FIXED ids demo-seed.sql's student_badges reference.
--    (The dev-profile BadgeSeedRunner would use random uuids — this pre-seeds
--    the exact catalog, and BadgeSeedRunner skips when the table isn't empty.)
-- ---------------------------------------------------------------------------
INSERT INTO badges (id, name, description, icon, criteria_type, criteria_value) VALUES
  ('9f5cfa48-92ac-455a-861c-05593908fcc1', 'Birinchi qadam',  'Birinchi 10 XP toʻplandi',        '🌱', 'TOTAL_XP',    10),
  ('44cc3b1f-b28b-4f80-beb9-84f7f33151f4', 'Faol oʻquvchi',   '100 XP toʻplandi',                '⭐', 'TOTAL_XP',   100),
  ('e437cc85-92ac-455a-861c-05593908fcc1', 'XP ustasi',       '500 XP toʻplandi',                '🏆', 'TOTAL_XP',   500),
  ('f6ca1486-aed3-4e1e-8fb1-79c9e455ae02', '3 kunlik seriya', '3 kun ketma-ket vazifa bajarildi', '🔥', 'STREAK_DAYS', 3),
  ('89ea4a2f-560c-43aa-9887-a04ac128f35e', 'Haftalik seriya', '7 kun ketma-ket vazifa bajarildi', '🔥🔥', 'STREAK_DAYS', 7),
  ('e45d709e-aed3-4e1e-8fb1-79c9e455ae02', 'Chidamli',        '30 kun ketma-ket vazifa bajarildi','💎', 'STREAK_DAYS', 30)
ON CONFLICT (id) DO NOTHING;

COMMIT;
