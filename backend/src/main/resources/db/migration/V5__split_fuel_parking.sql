-- filepath: /backend/src/main/resources/db/migration/V5__split_fuel_parking.sql
-- Pisahkan "Bensin & Parkir" (V4) menjadi dua kategori: "Bensin" dan "Parkir".
-- Kategori lama diubah menjadi "Bensin" (id tetap), jadi transaksi yang sudah tercatat di
-- "Bensin & Parkir" otomatis menjadi "Bensin" dan bisa dipindah manual ke "Parkir" bila perlu.

UPDATE categories
SET code = 'FUEL', name = 'Bensin', icon = '⛽'
WHERE code = 'FUEL_PARKING';

-- Sisipkan "Parkir" tepat setelah "Bensin" (sort_order 4)
UPDATE categories
SET sort_order = sort_order + 1
WHERE type = 'EXPENSE' AND user_id IS NULL AND sort_order >= 5;

INSERT INTO categories (code, name, type, kind, color, icon, sort_order)
VALUES ('PARKING', 'Parkir', 'EXPENSE', 'SPENDING', '#854D0E', '🅿️', 5)
ON CONFLICT (code) DO NOTHING;
