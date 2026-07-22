-- ============================================================================
-- AcademiX AI — demo seed data ("Ibn Sino xususiy maktabi")
-- ============================================================================
-- Re-runnable: all rows use fixed literal UUIDs (or md5-derived deterministic
-- UUIDs) + ON CONFLICT DO NOTHING / idempotent UPDATEs. Safe to re-apply.
-- Relative dates (now() - interval) are evaluated on FIRST application only;
-- re-runs skip existing rows, so the timeline freezes at first-run time.
--
-- Apply with:
--   docker exec -i infra-postgres-1 psql -U academix -d academix < infra/seed/demo-seed.sql
--
-- What it creates (on top of the pre-existing dev data):
--   * renames the school to 'Ibn Sino xususiy maktabi'
--   * class 8-B (2025-2026) + class-teacher wiring for 7-A/8-B
--   * teacher assignments: Gulnora -> 7-A/8-B Matematika, Malika Shodiyeva ->
--     7-A/8-B Fizika
--   * 8 new students (3 into 7-A, 5 into 8-B) + Aziz/Jasur moved into 7-A;
--     varied XP/streaks; student_count kept in sync
--   * 12 homework assignments (6 per class, last 6 weeks, 2 with future
--     deadlines), ~45 submissions (GRADED / AI_DONE / one AI_SKIPPED / one
--     SUBMITTED), grades, ai_feedbacks with real criteriaScores/stepAnalyses
--     JSON; ONE submission flagged plagiarism_type=AI_GENERATED (score 78)
--   * 2 exams ("1-chorak nazorat ishi", ~2 weeks ago) + exam_submissions,
--     exam_grades, exam_ai_feedbacks (school_id set — RLS table)
--   * xp_history rows derived from graded work + streak bonuses
--   * badge awards (student_badges) for the more active students
--   * 4 psychological signals (LOW resolved / MEDIUM open / HIGH open /
--     CRITICAL resolved) — NOTE: psychological_signals has NO school_id column
--   * notifications for teachers/parent/students/psychologist
--
-- DEMO LOGINS (all passwords: Test1234!):
--   ADMIN        +998901234567  (Test Admin)
--   TEACHER      +998911112233  (Gulnora Yusupova — Matematika, 7-A rahbari)
--   TEACHER      +998912345678  (Malika Shodiyeva — Fizika, 8-B rahbari)
--   PSYCHOLOGIST +998955501234  (Nilufar Psixolog)
--   PARENT       +998977001122  (Aziz & Jasur's parent)
--   STUDENT 7-A  +998933334455  (Aziz Karimov)
--   STUDENT 7-A  +998971110033  (Jasur Toshpulatov)
--   STUDENT 7-A  +998903000001  (Malika Rahimova)
--   STUDENT 7-A  +998903000002  (Sardor Alimov)
--   STUDENT 7-A  +998903000003  (Nilufar Qodirova)
--   STUDENT 8-B  +998903000004  (Bekzod Tursunov)
--   STUDENT 8-B  +998903000005  (Dilnoza Ergasheva)
--   STUDENT 8-B  +998903000006  (Timur Yusupov)
--   STUDENT 8-B  +998903000007  (Zarina Islomova)
--   STUDENT 8-B  +998903000008  (Otabek Nazarov)
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. School rename
-- ---------------------------------------------------------------------------
UPDATE schools
SET name = 'Ibn Sino xususiy maktabi'
WHERE id = 'cc021f70-256f-45c0-be19-ab7beddab1b3';

-- Make sure teacher2's users.school_id is set (needed for SchoolContextResolver)
UPDATE users SET school_id = 'cc021f70-256f-45c0-be19-ab7beddab1b3'
WHERE id = '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3' AND school_id IS NULL;

-- ---------------------------------------------------------------------------
-- 2. Class 8-B + class teachers
-- ---------------------------------------------------------------------------
INSERT INTO school_classes (id, school_id, grade, letter, full_name, class_teacher_id, academic_year, is_active)
VALUES ('a3000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
        8, 'B', '8-B', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', '2025-2026', true)
ON CONFLICT (id) DO NOTHING;

UPDATE school_classes SET class_teacher_id = '7c0adf23-9276-431a-bd18-5b4873eab945'
WHERE id = '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39' AND class_teacher_id IS NULL;

-- ---------------------------------------------------------------------------
-- 3. class_subject_teachers (Gulnora=Matematika, Malika Shodiyeva=Fizika)
-- ---------------------------------------------------------------------------
INSERT INTO class_subject_teachers (id, school_id, class_id, subject_id, teacher_id, academic_year) VALUES
  ('a4000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   'a3000000-0000-4000-8000-000000000001', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea',
   '7c0adf23-9276-431a-bd18-5b4873eab945', '2025-2026'),
  ('a4000000-0000-4000-8000-000000000002', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd',
   '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', '2025-2026'),
  ('a4000000-0000-4000-8000-000000000003', 'cc021f70-256f-45c0-be19-ab7beddab1b3',
   'a3000000-0000-4000-8000-000000000001', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd',
   '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', '2025-2026')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Students: 8 new users + profiles; Aziz/Jasur moved into 7-A
--    (password hash copied from admin => Test1234! for everyone)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, phone, password_hash, role, is_active, created_at) VALUES
  ('a1000000-0000-4000-8000-000000000001', 'Malika',  'Rahimova',  '+998903000001', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '80 days'),
  ('a1000000-0000-4000-8000-000000000002', 'Sardor',  'Alimov',    '+998903000002', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '80 days'),
  ('a1000000-0000-4000-8000-000000000003', 'Nilufar', 'Qodirova',  '+998903000003', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '79 days'),
  ('a1000000-0000-4000-8000-000000000004', 'Bekzod',  'Tursunov',  '+998903000004', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '78 days'),
  ('a1000000-0000-4000-8000-000000000005', 'Dilnoza', 'Ergasheva', '+998903000005', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '78 days'),
  ('a1000000-0000-4000-8000-000000000006', 'Timur',   'Yusupov',   '+998903000006', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '77 days'),
  ('a1000000-0000-4000-8000-000000000007', 'Zarina',  'Islomova',  '+998903000007', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '77 days'),
  ('a1000000-0000-4000-8000-000000000008', 'Otabek',  'Nazarov',   '+998903000008', '$2b$12$uABL4n3SJzrm90ns7Td5o.NIXpo7L6G5OfgSSbaPnO537w3zTOzlS', 'STUDENT', true, now() - interval '76 days')
ON CONFLICT DO NOTHING;

INSERT INTO student_profiles (id, user_id, class_id, school_id, student_number, total_xp, current_streak, max_streak, last_submission_date, is_active) VALUES
  ('a2000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '7A-003', 2380, 21, 21, current_date, true),
  ('a2000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000002', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '7A-004',  890,  4,  9, current_date - 2, true),
  ('a2000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000003', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '7A-005', 1720, 12, 14, current_date, true),
  ('a2000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000004', 'a3000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '8B-001',  310,  0,  5, current_date - 9, true),
  ('a2000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000005', 'a3000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '8B-002', 1980, 14, 16, current_date, true),
  ('a2000000-0000-4000-8000-000000000006', 'a1000000-0000-4000-8000-000000000006', 'a3000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '8B-003',  560,  2,  6, current_date - 1, true),
  ('a2000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000007', 'a3000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '8B-004', 1240,  8, 11, current_date, true),
  ('a2000000-0000-4000-8000-000000000008', 'a1000000-0000-4000-8000-000000000008', 'a3000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '8B-005',  430,  1,  4, current_date - 3, true)
ON CONFLICT DO NOTHING;

-- Move the two pre-existing students into 7-A and give them believable stats
UPDATE student_profiles SET class_id = '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39',
       school_id = 'cc021f70-256f-45c0-be19-ab7beddab1b3',
       total_xp = GREATEST(total_xp, 1450), current_streak = 7, max_streak = GREATEST(max_streak, 10),
       last_submission_date = current_date
WHERE user_id = 'f2245ba6-6ee5-4161-bcc5-c22ac742459f';   -- Aziz Karimov

UPDATE student_profiles SET class_id = '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39',
       school_id = 'cc021f70-256f-45c0-be19-ab7beddab1b3',
       total_xp = GREATEST(total_xp, 640), current_streak = 3, max_streak = GREATEST(max_streak, 7),
       last_submission_date = current_date - 1
WHERE user_id = '0969224c-1348-4d49-91c3-034dcea56490';   -- Jasur Toshpulatov

-- ---------------------------------------------------------------------------
-- 5. Keep student_count in sync with real enrollment
-- ---------------------------------------------------------------------------
UPDATE school_classes sc
SET student_count = (SELECT count(*) FROM student_profiles sp
                     WHERE sp.class_id = sc.id AND sp.is_active)
WHERE sc.school_id = 'cc021f70-256f-45c0-be19-ab7beddab1b3';

-- ---------------------------------------------------------------------------
-- 6. Homework assignments (6 per class over the last 6 weeks; 2 still open)
--    subjects: 4b3d36ff.. = Matematika (Gulnora), ed03c4fe.. = Fizika (Malika Sh.)
-- ---------------------------------------------------------------------------
INSERT INTO homework_assignments (id, school_id, class_id, subject_id, teacher_id, title, description, type, assigned_at, deadline_at, max_score, is_active, tasks_published) VALUES
  -- 7-A Matematika
  ('b1000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Kvadrat tenglamalar', 'Diskriminant yordamida kvadrat tenglamalarni yechish: 12-15 misollar, 48-bet.', 'STANDARD', now() - interval '40 days', now() - interval '33 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000002', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Chiziqli funksiya grafigi', 'y = kx + b funksiya grafigini yasash va xossalarini aniqlash. 5 ta topshiriq.', 'STANDARD', now() - interval '33 days', now() - interval '26 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000003', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Foizlar va nisbatlar', 'Amaliy masalalar: chegirma, ustama va aralashma masalalari (6 ta masala).', 'STANDARD', now() - interval '19 days', now() - interval '12 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000004', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Koʻphadlarni koʻpaytirish', 'Qisqa koʻpaytirish formulalari boʻyicha 10 ta mashq, 61-bet.', 'STANDARD', now() - interval '5 days', now() + interval '2 days', 100, true, true),
  -- 7-A Fizika
  ('b1000000-0000-4000-8000-000000000005', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'Nyuton qonunlari masalalari', 'Nyutonning II qonuni boʻyicha masalalar: F = ma. 4 ta masala, 35-bet.', 'STANDARD', now() - interval '26 days', now() - interval '19 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000006', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'Zichlik va bosim', 'Zichlik va bosimga oid amaliy masalalar (5 ta), jadval bilan ishlash.', 'STANDARD', now() - interval '12 days', now() - interval '5 days', 100, true, true),
  -- 8-B Matematika
  ('b1000000-0000-4000-8000-000000000007', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Kvadrat ildizlar', 'Kvadrat ildiz xossalari va soddalashtirish: 20 ta mashq, 27-bet.', 'STANDARD', now() - interval '38 days', now() - interval '31 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000008', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Algebraik kasrlar', 'Algebraik kasrlarni qisqartirish va umumiy maxrajga keltirish (8 ta mashq).', 'STANDARD', now() - interval '24 days', now() - interval '17 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000009', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', 'Tenglamalar sistemasi', 'Ikki nomaʼlumli chiziqli tenglamalar sistemasini yechish (6 ta sistema).', 'STANDARD', now() - interval '10 days', now() - interval '3 days', 100, true, true),
  -- 8-B Fizika
  ('b1000000-0000-4000-8000-000000000010', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'Elektr zanjirlari', 'Om qonuni asosida zanjir hisobi: I = U/R. 5 ta masala, 52-bet.', 'STANDARD', now() - interval '31 days', now() - interval '24 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000011', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'Issiqlik miqdori', 'Q = cmΔt formulasi boʻyicha issiqlik hisoblari (4 ta masala).', 'STANDARD', now() - interval '17 days', now() - interval '10 days', 100, true, true),
  ('b1000000-0000-4000-8000-000000000012', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'Yorugʻlik hodisalari', 'Yorugʻlikning qaytishi va sinishi: chizmalar bilan 5 ta topshiriq.', 'STANDARD', now() - interval '4 days', now() + interval '3 days', 100, true, true)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Submissions (from a parameter table), grades, ai_feedbacks
--    Students:
--      7-A: Aziz f2245ba6.., Jasur 0969224c.., Malika R a1..01, Sardor a1..02, Nilufar Q a1..03
--      8-B: Bekzod a1..04, Dilnoza a1..05, Timur a1..06, Zarina a1..07, Otabek a1..08
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE _seed_subs (
  sid uuid, aid uuid, stid uuid, status text, score int, late boolean, subj char(1), off_days int
) ON COMMIT DROP;

INSERT INTO _seed_subs VALUES
  -- A1 Kvadrat tenglamalar (7-A Math)
  ('b2000000-0000-4000-8000-000000000001','b1000000-0000-4000-8000-000000000001','f2245ba6-6ee5-4161-bcc5-c22ac742459f','GRADED',92,false,'M',4),
  ('b2000000-0000-4000-8000-000000000002','b1000000-0000-4000-8000-000000000001','0969224c-1348-4d49-91c3-034dcea56490','GRADED',74,false,'M',5),
  ('b2000000-0000-4000-8000-000000000003','b1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000001','GRADED',96,false,'M',2),
  ('b2000000-0000-4000-8000-000000000004','b1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000003','GRADED',85,false,'M',3),
  ('b2000000-0000-4000-8000-000000000005','b1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000002','GRADED',61,true ,'M',8),
  -- A2 Chiziqli funksiya grafigi (7-A Math) — Jasur missed
  ('b2000000-0000-4000-8000-000000000006','b1000000-0000-4000-8000-000000000002','f2245ba6-6ee5-4161-bcc5-c22ac742459f','GRADED',88,false,'M',3),
  ('b2000000-0000-4000-8000-000000000007','b1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000001','GRADED',94,false,'M',2),
  ('b2000000-0000-4000-8000-000000000008','b1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000002','GRADED',72,false,'M',6),
  ('b2000000-0000-4000-8000-000000000009','b1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000003','GRADED',90,false,'M',4),
  -- A3 Foizlar va nisbatlar (7-A Math) — Sardor missed
  ('b2000000-0000-4000-8000-000000000010','b1000000-0000-4000-8000-000000000003','f2245ba6-6ee5-4161-bcc5-c22ac742459f','GRADED',79,false,'M',4),
  ('b2000000-0000-4000-8000-000000000011','b1000000-0000-4000-8000-000000000003','0969224c-1348-4d49-91c3-034dcea56490','GRADED',66,false,'M',6),
  ('b2000000-0000-4000-8000-000000000012','b1000000-0000-4000-8000-000000000003','a1000000-0000-4000-8000-000000000001','GRADED',91,false,'M',2),
  ('b2000000-0000-4000-8000-000000000013','b1000000-0000-4000-8000-000000000003','a1000000-0000-4000-8000-000000000003','GRADED',83,false,'M',3),
  -- A4 Ko'phadlar (7-A Math, deadline in the FUTURE — fresh AI results, not yet graded)
  ('b2000000-0000-4000-8000-000000000014','b1000000-0000-4000-8000-000000000004','a1000000-0000-4000-8000-000000000001','AI_DONE',89,false,'M',2),
  ('b2000000-0000-4000-8000-000000000015','b1000000-0000-4000-8000-000000000004','f2245ba6-6ee5-4161-bcc5-c22ac742459f','AI_DONE',81,false,'M',3),
  ('b2000000-0000-4000-8000-000000000016','b1000000-0000-4000-8000-000000000004','a1000000-0000-4000-8000-000000000003','SUBMITTED',NULL,false,'M',3),
  -- A5 Nyuton qonunlari (7-A Fizika)
  ('b2000000-0000-4000-8000-000000000017','b1000000-0000-4000-8000-000000000005','f2245ba6-6ee5-4161-bcc5-c22ac742459f','GRADED',84,false,'F',3),
  ('b2000000-0000-4000-8000-000000000018','b1000000-0000-4000-8000-000000000005','0969224c-1348-4d49-91c3-034dcea56490','GRADED',58,false,'F',6),
  ('b2000000-0000-4000-8000-000000000019','b1000000-0000-4000-8000-000000000005','a1000000-0000-4000-8000-000000000001','GRADED',93,false,'F',2),
  ('b2000000-0000-4000-8000-000000000020','b1000000-0000-4000-8000-000000000005','a1000000-0000-4000-8000-000000000002','GRADED',70,false,'F',5),
  ('b2000000-0000-4000-8000-000000000021','b1000000-0000-4000-8000-000000000005','a1000000-0000-4000-8000-000000000003','GRADED',87,false,'F',4),
  -- A6 Zichlik va bosim (7-A Fizika) — Jasur late & AI unavailable (AI_SKIPPED)
  ('b2000000-0000-4000-8000-000000000022','b1000000-0000-4000-8000-000000000006','f2245ba6-6ee5-4161-bcc5-c22ac742459f','GRADED',76,false,'F',4),
  ('b2000000-0000-4000-8000-000000000023','b1000000-0000-4000-8000-000000000006','a1000000-0000-4000-8000-000000000001','GRADED',89,false,'F',2),
  ('b2000000-0000-4000-8000-000000000024','b1000000-0000-4000-8000-000000000006','a1000000-0000-4000-8000-000000000003','AI_DONE',82,false,'F',5),
  ('b2000000-0000-4000-8000-000000000025','b1000000-0000-4000-8000-000000000006','0969224c-1348-4d49-91c3-034dcea56490','AI_SKIPPED',NULL,true,'F',8),
  -- A7 Kvadrat ildizlar (8-B Math) — Otabek missed
  ('b2000000-0000-4000-8000-000000000026','b1000000-0000-4000-8000-000000000007','a1000000-0000-4000-8000-000000000004','GRADED',62,false,'M',6),
  ('b2000000-0000-4000-8000-000000000027','b1000000-0000-4000-8000-000000000007','a1000000-0000-4000-8000-000000000005','GRADED',95,false,'M',2),
  ('b2000000-0000-4000-8000-000000000028','b1000000-0000-4000-8000-000000000007','a1000000-0000-4000-8000-000000000006','GRADED',71,false,'M',5),
  ('b2000000-0000-4000-8000-000000000029','b1000000-0000-4000-8000-000000000007','a1000000-0000-4000-8000-000000000007','GRADED',88,false,'M',3),
  -- A8 Algebraik kasrlar (8-B Math) — Bekzod missed, Otabek late
  ('b2000000-0000-4000-8000-000000000030','b1000000-0000-4000-8000-000000000008','a1000000-0000-4000-8000-000000000005','GRADED',92,false,'M',2),
  ('b2000000-0000-4000-8000-000000000031','b1000000-0000-4000-8000-000000000008','a1000000-0000-4000-8000-000000000006','GRADED',67,false,'M',6),
  ('b2000000-0000-4000-8000-000000000032','b1000000-0000-4000-8000-000000000008','a1000000-0000-4000-8000-000000000007','GRADED',84,false,'M',4),
  ('b2000000-0000-4000-8000-000000000033','b1000000-0000-4000-8000-000000000008','a1000000-0000-4000-8000-000000000008','GRADED',59,true ,'M',8),
  -- A9 Tenglamalar sistemasi (8-B Math) — full class
  ('b2000000-0000-4000-8000-000000000034','b1000000-0000-4000-8000-000000000009','a1000000-0000-4000-8000-000000000004','GRADED',55,false,'M',6),
  ('b2000000-0000-4000-8000-000000000035','b1000000-0000-4000-8000-000000000009','a1000000-0000-4000-8000-000000000005','GRADED',97,false,'M',2),
  ('b2000000-0000-4000-8000-000000000036','b1000000-0000-4000-8000-000000000009','a1000000-0000-4000-8000-000000000006','GRADED',64,false,'M',5),
  ('b2000000-0000-4000-8000-000000000037','b1000000-0000-4000-8000-000000000009','a1000000-0000-4000-8000-000000000007','GRADED',90,false,'M',3),
  ('b2000000-0000-4000-8000-000000000038','b1000000-0000-4000-8000-000000000009','a1000000-0000-4000-8000-000000000008','GRADED',73,false,'M',4),
  -- A10 Elektr zanjirlari (8-B Fizika) — Otabek missed
  ('b2000000-0000-4000-8000-000000000039','b1000000-0000-4000-8000-000000000010','a1000000-0000-4000-8000-000000000004','GRADED',57,false,'F',6),
  ('b2000000-0000-4000-8000-000000000040','b1000000-0000-4000-8000-000000000010','a1000000-0000-4000-8000-000000000005','GRADED',91,false,'F',2),
  ('b2000000-0000-4000-8000-000000000041','b1000000-0000-4000-8000-000000000010','a1000000-0000-4000-8000-000000000006','GRADED',69,false,'F',5),
  ('b2000000-0000-4000-8000-000000000042','b1000000-0000-4000-8000-000000000010','a1000000-0000-4000-8000-000000000007','GRADED',82,false,'F',3),
  -- A11 Issiqlik miqdori (8-B Fizika) — Timur's work is AI-GENERATED (plagiarism demo), pending teacher review
  ('b2000000-0000-4000-8000-000000000043','b1000000-0000-4000-8000-000000000011','a1000000-0000-4000-8000-000000000005','GRADED',94,false,'F',2),
  ('b2000000-0000-4000-8000-000000000044','b1000000-0000-4000-8000-000000000011','a1000000-0000-4000-8000-000000000007','GRADED',86,false,'F',3),
  ('b2000000-0000-4000-8000-000000000045','b1000000-0000-4000-8000-000000000011','a1000000-0000-4000-8000-000000000008','GRADED',65,false,'F',5),
  ('b2000000-0000-4000-8000-000000000046','b1000000-0000-4000-8000-000000000011','a1000000-0000-4000-8000-000000000006','AI_DONE',78,false,'F',4),
  -- A12 Yorug'lik hodisalari (8-B Fizika, deadline in the FUTURE)
  ('b2000000-0000-4000-8000-000000000047','b1000000-0000-4000-8000-000000000012','a1000000-0000-4000-8000-000000000005','AI_DONE',88,false,'F',1),
  ('b2000000-0000-4000-8000-000000000048','b1000000-0000-4000-8000-000000000012','a1000000-0000-4000-8000-000000000004','AI_DONE',63,false,'F',2);

-- 7a. homework_submissions
INSERT INTO homework_submissions (id, school_id, assignment_id, student_id, type, text_content, status, is_late, submitted_at, xp_earned)
SELECT s.sid, ha.school_id, s.aid, s.stid, 'TEXT',
       CASE WHEN s.subj = 'M'
            THEN 'Berilgan: x² − 5x + 6 = 0. D = b² − 4ac = 25 − 24 = 1. x₁ = (5+1)/2 = 3, x₂ = (5−1)/2 = 2. Javob: x ∈ {2; 3}.'
            ELSE 'Berilgan: m = 2 kg, a = 3 m/s². Nyutonning II qonuniga koʻra F = m·a = 2 · 3 = 6 N. Javob: F = 6 N.' END,
       s.status, s.late,
       ha.assigned_at + (s.off_days || ' days')::interval,
       CASE WHEN s.status = 'GRADED' THEN
              (CASE WHEN s.score >= 90 THEN 50 WHEN s.score >= 75 THEN 35 ELSE 20 END)
              / CASE WHEN s.late THEN 2 ELSE 1 END
            ELSE 0 END
FROM _seed_subs s JOIN homework_assignments ha ON ha.id = s.aid
ON CONFLICT (id) DO NOTHING;

-- 7b. grades for GRADED submissions (deterministic ids: md5(sid||'grade'))
INSERT INTO grades (id, submission_id, teacher_id, score, five_point_grade, teacher_comment, teacher_overrode_ai, ai_original_score, graded_at)
SELECT md5(s.sid::text || 'grade')::uuid, s.sid, ha.teacher_id, s.score,
       CASE WHEN s.score >= 86 THEN 5 WHEN s.score >= 71 THEN 4 WHEN s.score >= 55 THEN 3 ELSE 2 END,
       CASE WHEN s.score >= 90 THEN 'Ajoyib ish! Barcha qadamlar toʻgʻri va tartibli bajarilgan.'
            WHEN s.score >= 75 THEN 'Yaxshi bajarilgan. Kichik hisob xatolariga eʼtibor bering.'
            WHEN s.score >= 60 THEN 'Qoniqarli. Yechim usulini yana bir bor mustahkamlab oling.'
            ELSE 'Mavzuni qayta koʻrib chiqing — qoʻshimcha mashq talab etiladi.' END,
       s.sid IN ('b2000000-0000-4000-8000-000000000008','b2000000-0000-4000-8000-000000000036'),
       CASE WHEN s.sid IN ('b2000000-0000-4000-8000-000000000008','b2000000-0000-4000-8000-000000000036')
            THEN s.score - 6 ELSE s.score END::real,
       ha.assigned_at + (s.off_days || ' days')::interval + interval '1 day'
FROM _seed_subs s JOIN homework_assignments ha ON ha.id = s.aid
WHERE s.status = 'GRADED'
ON CONFLICT (submission_id) DO NOTHING;

-- 7c. ai_feedbacks for AI_DONE + GRADED submissions
--     JSON shapes match domain records exactly:
--       criteria_scores: [{name, weightPercent, score}]
--       step_analyses:   [{stepNumber, stepContent, isCorrect, errorDescription, suggestion}]
INSERT INTO ai_feedbacks (id, submission_id, extracted_text, ocr_confidence, step_analyses, criteria_scores, ai_score_percent, feedback, plagiarism_score, plagiarism_type, handwriting_match_score, processed_at)
SELECT md5(s.sid::text || 'aif')::uuid, s.sid,
       CASE WHEN s.subj = 'M'
            THEN 'x² − 5x + 6 = 0; D = 25 − 24 = 1; x₁ = 3; x₂ = 2; Javob: {2; 3}'
            ELSE 'm = 2 kg; a = 3 m/s²; F = m·a = 6 N; Javob: 6 N' END,
       (0.85 + (s.score % 10) / 100.0)::real,
       jsonb_build_array(
         jsonb_build_object('stepNumber', 1, 'stepContent', 'Formula toʻgʻri tanlangan', 'isCorrect', true, 'errorDescription', NULL, 'suggestion', NULL),
         jsonb_build_object('stepNumber', 2, 'stepContent', 'Hisob-kitob bosqichi',
                            'isCorrect', s.score >= 75,
                            'errorDescription', CASE WHEN s.score < 75 THEN 'Hisoblashda arifmetik xatolikka yoʻl qoʻyilgan' END,
                            'suggestion',       CASE WHEN s.score < 75 THEN 'Amallarni bosqichma-bosqich qayta tekshiring' END),
         jsonb_build_object('stepNumber', 3, 'stepContent', 'Yakuniy javob',
                            'isCorrect', s.score >= 60,
                            'errorDescription', CASE WHEN s.score < 60 THEN 'Yakuniy javob notoʻgʻri chiqarilgan' END,
                            'suggestion',       CASE WHEN s.score < 60 THEN 'Javobni shartga qoʻyib tekshirib koʻring' END)
       ),
       jsonb_build_array(
         jsonb_build_object('name', 'Toʻgʻrilik',    'weightPercent', 50, 'score', LEAST(100, s.score + 4)),
         jsonb_build_object('name', 'Yechim usuli',  'weightPercent', 30, 'score', s.score),
         jsonb_build_object('name', 'Tartiblilik',   'weightPercent', 20, 'score', GREATEST(0, s.score - 8))
       ),
       ((LEAST(100, s.score + 4) * 50 + s.score * 30 + GREATEST(0, s.score - 8) * 20) / 100.0)::real,
       CASE WHEN s.score >= 90 THEN 'Yechim toʻliq va izchil. Barcha bosqichlar toʻgʻri bajarilgan, javob aniq.'
            WHEN s.score >= 75 THEN 'Yechim asosan toʻgʻri. Ayrim oraliq hisoblarda ehtiyotkorlik talab etiladi.'
            WHEN s.score >= 60 THEN 'Yondashuv toʻgʻri, ammo hisoblashda xatolar bor. Qadamlarni qayta koʻring.'
            ELSE 'Yechimda jiddiy xatolar mavjud. Mavzuni takrorlash tavsiya etiladi.' END,
       ((s.score * 7) % 13)::real,
       'CLEAN',
       (80 + (s.score % 18))::real,
       ha.assigned_at + (s.off_days || ' days')::interval + interval '5 minutes'
FROM _seed_subs s JOIN homework_assignments ha ON ha.id = s.aid
WHERE s.status IN ('AI_DONE', 'GRADED')
ON CONFLICT (submission_id) DO NOTHING;

-- 7d. The plagiarism demo: Timur's "Issiqlik miqdori" work detected as AI-generated
UPDATE ai_feedbacks
SET plagiarism_score = 78, plagiarism_type = 'AI_GENERATED', handwriting_match_score = 44,
    feedback = 'DIQQAT: matn uslubi sunʼiy intellekt tomonidan yozilganlikka juda oʻxshash (78%). Yozuv uslubi oʻquvchining avvalgi ishlaridan keskin farq qiladi. Oʻqituvchi tekshiruvi talab etiladi.'
WHERE submission_id = 'b2000000-0000-4000-8000-000000000046';

-- ---------------------------------------------------------------------------
-- 8. Exams: "1-chorak nazorat ishi" per class (~2 weeks ago)
-- ---------------------------------------------------------------------------
INSERT INTO exams (id, school_id, class_id, subject_id, teacher_id, title, exam_date, max_score, created_at) VALUES
  ('c1000000-0000-4000-8000-000000000001', 'cc021f70-256f-45c0-be19-ab7beddab1b3', '1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39', '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea', '7c0adf23-9276-431a-bd18-5b4873eab945', '1-chorak nazorat ishi — Matematika', (now() - interval '14 days')::date, 100, now() - interval '16 days'),
  ('c1000000-0000-4000-8000-000000000002', 'cc021f70-256f-45c0-be19-ab7beddab1b3', 'a3000000-0000-4000-8000-000000000001', 'ed03c4fe-ef5f-4dd3-9455-0e1a4db00bdd', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', '1-chorak nazorat ishi — Fizika', (now() - interval '15 days')::date, 100, now() - interval '17 days')
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE _seed_exam_subs (esid uuid, eid uuid, stid uuid, score int, n int) ON COMMIT DROP;
INSERT INTO _seed_exam_subs VALUES
  -- 7-A Matematika
  ('c2000000-0000-4000-8000-000000000001','c1000000-0000-4000-8000-000000000001','f2245ba6-6ee5-4161-bcc5-c22ac742459f',90,1),
  ('c2000000-0000-4000-8000-000000000002','c1000000-0000-4000-8000-000000000001','0969224c-1348-4d49-91c3-034dcea56490',68,2),
  ('c2000000-0000-4000-8000-000000000003','c1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000001',97,3),
  ('c2000000-0000-4000-8000-000000000004','c1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000002',74,4),
  ('c2000000-0000-4000-8000-000000000005','c1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000003',86,5),
  -- 8-B Fizika
  ('c2000000-0000-4000-8000-000000000006','c1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000004',60,1),
  ('c2000000-0000-4000-8000-000000000007','c1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000005',96,2),
  ('c2000000-0000-4000-8000-000000000008','c1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000006',66,3),
  ('c2000000-0000-4000-8000-000000000009','c1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000007',89,4),
  ('c2000000-0000-4000-8000-000000000010','c1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000008',71,5);

INSERT INTO exam_submissions (id, school_id, exam_id, student_id, image_url, status, flagged_for_review, uploaded_at)
SELECT es.esid, e.school_id, es.eid, es.stid,
       'exams/demo/' || es.eid || '/' || es.n || '.jpg', 'GRADED', false,
       e.exam_date::timestamp + interval '4 hours'
FROM _seed_exam_subs es JOIN exams e ON e.id = es.eid
ON CONFLICT (id) DO NOTHING;

INSERT INTO exam_grades (id, exam_submission_id, teacher_id, score, five_point_grade, teacher_comment, graded_at)
SELECT md5(es.esid::text || 'eg')::uuid, es.esid, e.teacher_id, es.score,
       CASE WHEN es.score >= 86 THEN 5 WHEN es.score >= 71 THEN 4 WHEN es.score >= 55 THEN 3 ELSE 2 END,
       CASE WHEN es.score >= 86 THEN 'Nazorat ishi aʼlo darajada bajarildi.'
            WHEN es.score >= 71 THEN 'Yaxshi natija, ayrim mavzularni mustahkamlash lozim.'
            ELSE 'Qoniqarli. Chorak mavzularini takrorlash tavsiya etiladi.' END,
       e.exam_date::timestamp + interval '2 days'
FROM _seed_exam_subs es JOIN exams e ON e.id = es.eid
ON CONFLICT (exam_submission_id) DO NOTHING;

INSERT INTO exam_ai_feedbacks (id, school_id, exam_submission_id, extracted_text, step_analyses, criteria_scores, ai_score_percent, feedback, handwriting_match_score, processed_at)
SELECT md5(es.esid::text || 'eaif')::uuid, e.school_id, es.esid,
       CASE WHEN e.subject_id = '4b3d36ff-cd25-4f2f-903f-5a9cb9fd44ea'
            THEN '1) x² − 7x + 12 = 0; D = 49 − 48 = 1; x = 3; 4. 2) 25% · 480 = 120. 3) sistema: x = 2, y = −1.'
            ELSE '1) I = U/R = 12/4 = 3 A. 2) Q = cmΔt = 4200 · 0.5 · 20 = 42 000 J. 3) F = ma = 5 · 2 = 10 N.' END,
       jsonb_build_array(
         jsonb_build_object('stepNumber', 1, 'stepContent', '1-topshiriq yechimi', 'isCorrect', true, 'errorDescription', NULL, 'suggestion', NULL),
         jsonb_build_object('stepNumber', 2, 'stepContent', '2-topshiriq yechimi',
                            'isCorrect', es.score >= 75,
                            'errorDescription', CASE WHEN es.score < 75 THEN 'Oraliq hisoblashda xatolik' END,
                            'suggestion',       CASE WHEN es.score < 75 THEN 'Formulani qoʻllashni qayta mashq qiling' END),
         jsonb_build_object('stepNumber', 3, 'stepContent', '3-topshiriq yechimi',
                            'isCorrect', es.score >= 65,
                            'errorDescription', CASE WHEN es.score < 65 THEN '3-topshiriq toʻliq yechilmagan' END,
                            'suggestion',       CASE WHEN es.score < 65 THEN 'Shu turdagi masalalardan qoʻshimcha ishlang' END)
       ),
       jsonb_build_array(
         jsonb_build_object('name', 'Toʻgʻrilik',   'weightPercent', 50, 'score', LEAST(100, es.score + 3)),
         jsonb_build_object('name', 'Yechim usuli', 'weightPercent', 30, 'score', es.score),
         jsonb_build_object('name', 'Tartiblilik',  'weightPercent', 20, 'score', GREATEST(0, es.score - 6))
       ),
       ((LEAST(100, es.score + 3) * 50 + es.score * 30 + GREATEST(0, es.score - 6) * 20) / 100.0)::real,
       CASE WHEN es.score >= 86 THEN 'Nazorat ishi juda yaxshi bajarilgan, barcha topshiriqlar toʻgʻri.'
            WHEN es.score >= 71 THEN 'Umumiy natija yaxshi, ayrim topshiriqlarda kichik xatolar bor.'
            ELSE 'Bir nechta topshiriqda xatoliklar aniqlandi, mavzularni takrorlash zarur.' END,
       (82 + (es.score % 15))::real,
       e.exam_date::timestamp + interval '5 hours'
FROM _seed_exam_subs es JOIN exams e ON e.id = es.eid
ON CONFLICT (exam_submission_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 9. XP history: one row per graded homework + streak/activity bonuses
-- ---------------------------------------------------------------------------
INSERT INTO xp_history (id, student_id, xp, reason, occurred_at)
SELECT md5(s.sid::text || 'xp')::uuid, s.stid,
       (CASE WHEN s.score >= 90 THEN 50 WHEN s.score >= 75 THEN 35 ELSE 20 END)
         / CASE WHEN s.late THEN 2 ELSE 1 END,
       'Uy vazifasi baholandi: ' || ha.title,
       ha.assigned_at + (s.off_days || ' days')::interval + interval '1 day'
FROM _seed_subs s JOIN homework_assignments ha ON ha.id = s.aid
WHERE s.status = 'GRADED'
ON CONFLICT (id) DO NOTHING;

INSERT INTO xp_history (id, student_id, xp, reason, occurred_at) VALUES
  ('e1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 70, 'Seriya bonusi: 21 kunlik seriya', now() - interval '1 day'),
  ('e1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', 30, 'Seriya bonusi: 7 kunlik seriya',  now() - interval '15 days'),
  ('e1000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000005', 40, 'Seriya bonusi: 14 kunlik seriya', now() - interval '2 days'),
  ('e1000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000005', 30, 'Seriya bonusi: 7 kunlik seriya',  now() - interval '9 days'),
  ('e1000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000003', 30, 'Seriya bonusi: 7 kunlik seriya',  now() - interval '5 days'),
  ('e1000000-0000-4000-8000-000000000006', 'f2245ba6-6ee5-4161-bcc5-c22ac742459f', 30, 'Seriya bonusi: 7 kunlik seriya',  now() - interval '1 day'),
  ('e1000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000007', 25, 'Seriya bonusi: 5 kunlik seriya',  now() - interval '3 days'),
  ('e1000000-0000-4000-8000-000000000008', '0969224c-1348-4d49-91c3-034dcea56490', 15, 'Seriya bonusi: 3 kunlik seriya',  now() - interval '2 days'),
  ('e1000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000006', 10, 'Kunlik faollik bonusi',           now() - interval '1 day'),
  ('e1000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000004', 10, 'Kunlik faollik bonusi',           now() - interval '9 days')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 10. Badge awards for the more active students (catalog seeded by the app)
--     badges: 9f5cfa48=Birinchi qadam(10XP) 44cc3b1f=Faol o'quvchi(100XP)
--             e437cc85=XP ustasi(500XP) f6ca1486=3 kunlik seriya
--             89ea4a2f=Haftalik seriya  e45d709e=Chidamli(30 kun)
-- ---------------------------------------------------------------------------
INSERT INTO student_badges (id, student_id, badge_id, awarded_at) VALUES
  ('e2000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 'e437cc85-92ac-455a-861c-05593908fcc1', now() - interval '20 days'), -- Malika R: XP ustasi
  ('e2000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', '89ea4a2f-560c-43aa-9887-a04ac128f35e', now() - interval '15 days'), -- Malika R: Haftalik seriya
  ('e2000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000001', '44cc3b1f-b28b-4f80-beb9-84f7f33151f4', now() - interval '35 days'), -- Malika R: Faol o'quvchi
  ('e2000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000005', 'e437cc85-92ac-455a-861c-05593908fcc1', now() - interval '12 days'), -- Dilnoza: XP ustasi
  ('e2000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000005', '89ea4a2f-560c-43aa-9887-a04ac128f35e', now() - interval '9 days'),  -- Dilnoza: Haftalik seriya
  ('e2000000-0000-4000-8000-000000000006', 'a1000000-0000-4000-8000-000000000003', '44cc3b1f-b28b-4f80-beb9-84f7f33151f4', now() - interval '30 days'), -- Nilufar Q: Faol o'quvchi
  ('e2000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003', '89ea4a2f-560c-43aa-9887-a04ac128f35e', now() - interval '5 days'),  -- Nilufar Q: Haftalik seriya
  ('e2000000-0000-4000-8000-000000000008', 'f2245ba6-6ee5-4161-bcc5-c22ac742459f', '44cc3b1f-b28b-4f80-beb9-84f7f33151f4', now() - interval '28 days'), -- Aziz: Faol o'quvchi
  ('e2000000-0000-4000-8000-000000000009', 'f2245ba6-6ee5-4161-bcc5-c22ac742459f', '89ea4a2f-560c-43aa-9887-a04ac128f35e', now() - interval '1 day'),   -- Aziz: Haftalik seriya
  ('e2000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007', '44cc3b1f-b28b-4f80-beb9-84f7f33151f4', now() - interval '18 days'), -- Zarina: Faol o'quvchi
  ('e2000000-0000-4000-8000-000000000011', 'a1000000-0000-4000-8000-000000000007', 'f6ca1486-aed3-4e1e-8fb1-79c9e455ae02', now() - interval '25 days'), -- Zarina: 3 kunlik seriya
  ('e2000000-0000-4000-8000-000000000012', '0969224c-1348-4d49-91c3-034dcea56490', 'f6ca1486-aed3-4e1e-8fb1-79c9e455ae02', now() - interval '2 days')   -- Jasur: 3 kunlik seriya
ON CONFLICT (student_id, badge_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 11. Psychological signals (NOTE: table has NO school_id column — not RLS)
--     Notify matrix: LOW=log only; MEDIUM/HIGH=class teacher+psychologist;
--     CRITICAL=+parent.
-- ---------------------------------------------------------------------------
INSERT INTO psychological_signals (id, student_id, type, severity, description, raw_evidence, is_manipulation, notified_class_teacher, notified_parent, notified_psychologist, resolved, detected_at, resolved_at, resolution_notes, action_taken) VALUES
  ('d1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000002', 'LATE_NIGHT_ACTIVITY', 'LOW',
   'Sardor soʻnggi haftada vazifalarni asosan 23:00 dan keyin topshirmoqda.',
   '{"lateNightSubmissions": 4, "windowDays": 7}'::jsonb,
   false, false, false, false, true,
   now() - interval '20 days', now() - interval '16 days',
   'Ota-onasi bilan suhbatdan soʻng uyqu tartibi yoʻlga qoʻyildi.', 'Kuzatuv yakunlandi'),
  ('d1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000006', 'MOTIVATION_DROP', 'MEDIUM',
   'Timurning javoblarida motivatsiya pasayishi belgilari: qisqa, eʼtiborsiz yechimlar, natijalar barqaror pasaymoqda.',
   '{"avgScoreTrend": [-12, -8], "windowDays": 21}'::jsonb,
   false, true, false, true, false,
   now() - interval '6 days', NULL, NULL, NULL),
  ('d1000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004', 'SUBMISSION_STOP', 'HIGH',
   'Bekzod soʻnggi 9 kun davomida birorta ham uy vazifasi topshirmadi — avvalgi faolligidan keskin farq.',
   '{"daysSinceLastSubmission": 9, "previousWeeklyAvg": 3}'::jsonb,
   false, true, false, true, false,
   now() - interval '3 days', NULL, NULL, NULL),
  ('d1000000-0000-4000-8000-000000000004', '0969224c-1348-4d49-91c3-034dcea56490', 'SUDDEN_PERFORMANCE_DROP', 'CRITICAL',
   'Jasurning natijalari 2 hafta ichida keskin pasaydi (85% dan 58% gacha), javoblarida salbiy kayfiyat ifodalari kuzatildi.',
   '{"scoreDrop": 27, "negativePhrases": 3, "windowDays": 14}'::jsonb,
   false, true, true, true, true,
   now() - interval '25 days', now() - interval '18 days',
   'Psixolog 2 marta individual suhbat oʻtkazdi, ota-ona xabardor qilindi. Holat barqarorlashdi.',
   'Individual suhbatlar va haftalik kuzatuv rejasi'),
  -- bonus: manipulation attempt flag for the psychologist view
  ('d1000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000006', 'MANIPULATION_ATTEMPT', 'LOW',
   'AI-tyutordan toʻgʻridan-toʻgʻri javob olishga urinish aniqlandi ("faqat javobni ayt" soʻrovi).',
   '{"blockedChatRequests": 2}'::jsonb,
   true, false, false, false, true,
   now() - interval '11 days', now() - interval '10 days',
   'Oʻquvchi bilan AI-tyutordan foydalanish qoidalari tushuntirildi.', 'Tushuntirish suhbati')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 12. Notifications (data column must be valid JSON)
-- ---------------------------------------------------------------------------
INSERT INTO notifications (id, user_id, type, title, body, data, is_read, created_at) VALUES
  -- Gulnora (7-A class teacher): critical psych alert about Jasur + progress report
  ('d2000000-0000-4000-8000-000000000001', '7c0adf23-9276-431a-bd18-5b4873eab945', 'PSYCHOLOGICAL_ALERT',
   'Psixologik signal: Jasur Toshpulatov', 'Jasur Toshpulatovda KRITIK darajadagi signal aniqlandi: natijalarning keskin pasayishi. Psixolog xabardor qilindi.',
   '{"studentId": "0969224c-1348-4d49-91c3-034dcea56490", "severity": "CRITICAL", "signalId": "d1000000-0000-4000-8000-000000000004"}'::jsonb, true, now() - interval '25 days'),
  ('d2000000-0000-4000-8000-000000000002', '7c0adf23-9276-431a-bd18-5b4873eab945', 'CLASS_PROGRESS_REPORT',
   '7-A sinf haftalik hisoboti', '7-A sinfida oʻtgan hafta 12 ta vazifa topshirildi, oʻrtacha ball: 82%.',
   '{"classId": "1bc5be58-cdfd-4a41-a150-b8f3a1cc0e39", "avgScore": 82}'::jsonb, false, now() - interval '2 days'),
  -- Malika Shodiyeva (8-B class teacher): HIGH psych alert about Bekzod + MEDIUM about Timur
  ('d2000000-0000-4000-8000-000000000003', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'PSYCHOLOGICAL_ALERT',
   'Psixologik signal: Bekzod Tursunov', 'Bekzod Tursunov 9 kundan beri vazifa topshirmayapti (YUQORI daraja). Eʼtibor talab etiladi.',
   '{"studentId": "a1000000-0000-4000-8000-000000000004", "severity": "HIGH", "signalId": "d1000000-0000-4000-8000-000000000003"}'::jsonb, false, now() - interval '3 days'),
  ('d2000000-0000-4000-8000-000000000004', '609d8ca9-dfa8-4482-8b37-e2e5a1308ac3', 'PSYCHOLOGICAL_ALERT',
   'Psixologik signal: Timur Yusupov', 'Timur Yusupovda motivatsiya pasayishi belgilari (OʻRTA daraja) aniqlandi.',
   '{"studentId": "a1000000-0000-4000-8000-000000000006", "severity": "MEDIUM", "signalId": "d1000000-0000-4000-8000-000000000002"}'::jsonb, false, now() - interval '6 days'),
  -- Psychologist
  ('d2000000-0000-4000-8000-000000000005', 'a31324b4-0bfa-4117-8dc7-d9679a9f3d7c', 'PSYCHOLOGICAL_ALERT',
   'Yangi signal: Bekzod Tursunov (YUQORI)', 'Bekzod Tursunov boʻyicha SUBMISSION_STOP signali: 9 kun faollik yoʻq.',
   '{"studentId": "a1000000-0000-4000-8000-000000000004", "severity": "HIGH"}'::jsonb, false, now() - interval '3 days'),
  -- Parent (linked to Aziz & Jasur)
  ('d2000000-0000-4000-8000-000000000006', 'a3ae17d5-c6ed-4014-809b-1e5fc8e292f3', 'HOMEWORK_GRADED',
   'Aziz: yangi baho', 'Aziz "Zichlik va bosim" vazifasidan 76 ball (4) oldi.',
   '{"studentId": "f2245ba6-6ee5-4161-bcc5-c22ac742459f", "score": 76, "fivePointGrade": 4}'::jsonb, false, now() - interval '6 days'),
  ('d2000000-0000-4000-8000-000000000007', 'a3ae17d5-c6ed-4014-809b-1e5fc8e292f3', 'PSYCHOLOGICAL_ALERT',
   'Jasur boʻyicha muhim xabar', 'Jasurning oʻqishdagi holati boʻyicha maktab psixologi siz bilan bogʻlanadi.',
   '{"studentId": "0969224c-1348-4d49-91c3-034dcea56490", "severity": "CRITICAL"}'::jsonb, true, now() - interval '25 days'),
  -- Students
  ('d2000000-0000-4000-8000-000000000008', 'a1000000-0000-4000-8000-000000000001', 'BADGE_EARNED',
   'Yangi nishon: XP ustasi', 'Tabriklaymiz! Siz 500 XP toʻplab "XP ustasi" nishonini qoʻlga kiritdingiz.',
   '{"badgeId": "e437cc85-92ac-455a-861c-05593908fcc1"}'::jsonb, true, now() - interval '20 days'),
  ('d2000000-0000-4000-8000-000000000009', 'f2245ba6-6ee5-4161-bcc5-c22ac742459f', 'HOMEWORK_GRADED',
   'Vazifangiz baholandi', '"Zichlik va bosim" vazifangiz 76 ball (4) bilan baholandi.',
   '{"submissionId": "b2000000-0000-4000-8000-000000000022", "score": 76}'::jsonb, false, now() - interval '6 days'),
  ('d2000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000005', 'BADGE_EARNED',
   'Yangi nishon: XP ustasi', 'Tabriklaymiz! Siz 500 XP toʻplab "XP ustasi" nishonini qoʻlga kiritdingiz.',
   '{"badgeId": "e437cc85-92ac-455a-861c-05593908fcc1"}'::jsonb, false, now() - interval '12 days'),
  ('d2000000-0000-4000-8000-000000000011', 'a1000000-0000-4000-8000-000000000004', 'DEADLINE_REMINDER',
   'Muddat yaqinlashmoqda', '"Yorugʻlik hodisalari" vazifasini topshirishga 3 kun qoldi.',
   '{"assignmentId": "b1000000-0000-4000-8000-000000000012"}'::jsonb, false, now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

COMMIT;
