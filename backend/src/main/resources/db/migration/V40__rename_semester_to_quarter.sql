-- O'zbekiston maktablari/o'quv markazlari o'quv yili 2 semestrga emas, 4 chorakka bo'linadi
-- (foydalanuvchi tomonidan aniqlangan domen tuzatishi) — "semester" atamasi "quarter"/chorak
-- bilan almashtiriladi. Ustun nomlari o'zgaradi, mavjud qiymatlar saqlanadi (faqat rename).
ALTER TABLE handwriting_profiles RENAME COLUMN reset_count_this_semester TO reset_count_this_quarter;
ALTER TABLE reports RENAME COLUMN semester TO quarter;
