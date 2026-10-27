-- filepath: /backend/src/main/resources/db/migration/V4__category_fuel_parking.sql
-- Kategori pengeluaran baru "Bensin & Parkir", diletakkan tepat setelah "Transportasi".
-- Migrasi baru (bukan mengubah V3) agar database yang sudah menjalankan V3 tetap valid.

-- Geser urutan kategori pengeluaran setelah Transportasi (sort_order 3) satu langkah ke bawah
UPDATE categories
SET sort_order = sort_order + 1
WHERE type = 'EXPENSE' AND user_id IS NULL AND sort_order >= 4;

INSERT INTO categories (code, name, type, kind, color, icon, sort_order)
VALUES ('FUEL_PARKING', 'Bensin & Parkir', 'EXPENSE', 'SPENDING', '#BE185D', '⛽', 4)
ON CONFLICT (code) DO NOTHING;
