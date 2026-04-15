-- Screenings for Moana 2 (id: 1241982)
INSERT INTO screenings (id, movie_id, room_id, date, time, format, price, status) VALUES
('f1000000-0000-0000-0000-000000000001', 1241982, 'b0000000-0000-0000-0000-000000000001', '2025-05-10', '15:30', 'standard', 22.00, 'active'),
('f1000000-0000-0000-0000-000000000002', 1241982, 'b0000000-0000-0000-0000-000000000002', '2025-05-10', '19:00', 'imax', 38.00, 'active'),
('f1000000-0000-0000-0000-000000000003', 1241982, 'b0000000-0000-0000-0000-000000000003', '2025-05-11', '14:00', '3d', 28.00, 'active'),

-- Screenings for Gladiador II
('f2000000-0000-0000-0000-000000000001', 558449, 'b0000000-0000-0000-0000-000000000001', '2025-05-10', '20:00', 'standard', 22.00, 'active'),
('f2000000-0000-0000-0000-000000000002', 558449, 'b0000000-0000-0000-0000-000000000002', '2025-05-11', '17:30', 'imax', 38.00, 'active'),
('f2000000-0000-0000-0000-000000000003', 558449, 'b0000000-0000-0000-0000-000000000004', '2025-05-12', '21:00', 'standard', 22.00, 'active'),

-- Screenings for Venom
('f3000000-0000-0000-0000-000000000001', 912649, 'b0000000-0000-0000-0000-000000000003', '2025-05-10', '16:00', 'standard', 22.00, 'active'),
('f3000000-0000-0000-0000-000000000002', 912649, 'b0000000-0000-0000-0000-000000000004', '2025-05-11', '19:30', '3d', 28.00, 'active'),

-- Screenings for Godzilla x Kong
('f4000000-0000-0000-0000-000000000001', 823464, 'b0000000-0000-0000-0000-000000000002', '2025-05-10', '14:00', 'imax', 38.00, 'active'),
('f4000000-0000-0000-0000-000000000002', 823464, 'b0000000-0000-0000-0000-000000000001', '2025-05-12', '20:30', 'standard', 22.00, 'active'),

-- Screenings for Intensamente 2
('f6000000-0000-0000-0000-000000000001', 1022789, 'b0000000-0000-0000-0000-000000000003', '2025-05-13', '15:00', 'standard', 22.00, 'active'),
('f6000000-0000-0000-0000-000000000002', 1022789, 'b0000000-0000-0000-0000-000000000004', '2025-05-14', '18:00', '3d', 28.00, 'active'),

-- Screenings for Alien: Romulus
('f9000000-0000-0000-0000-000000000001', 945961, 'b0000000-0000-0000-0000-000000000001', '2025-05-11', '22:00', 'standard', 22.00, 'active'),
('f9000000-0000-0000-0000-000000000002', 945961, 'b0000000-0000-0000-0000-000000000003', '2025-05-12', '23:00', 'standard', 22.00, 'active');

-- Create seats for all screenings
-- Standard rooms (8x10), IMAX room (10x12)
-- Rows D,E are preferential for standard; rows E,F for IMAX

DO $$
DECLARE
    screening_rec RECORD;
    row_labels TEXT[];
    col_count INTEGER;
    row_label TEXT;
    col_num INTEGER;
    seat_type TEXT;
    seat_status TEXT;
BEGIN
    FOR screening_rec IN SELECT s.id, r.rows, r.cols FROM screenings s JOIN rooms r ON r.id = s.room_id LOOP
        IF screening_rec.rows = 10 THEN
            row_labels := ARRAY['A','B','C','D','E','F','G','H','I','J'];
        ELSE
            row_labels := ARRAY['A','B','C','D','E','F','G','H'];
        END IF;
        col_count := screening_rec.cols;

        FOREACH row_label IN ARRAY row_labels LOOP
            FOR col_num IN 1..col_count LOOP
                -- Preferential rows: D,E for 8-row; E,F for 10-row
                IF screening_rec.rows = 10 THEN
                    seat_type := CASE WHEN row_label IN ('E','F') THEN 'preferential' ELSE 'standard' END;
                ELSE
                    seat_type := CASE WHEN row_label IN ('D','E') THEN 'preferential' ELSE 'standard' END;
                END IF;
                -- Pre-occupy ~15% of seats
                seat_status := CASE WHEN random() < 0.15 THEN 'occupied' ELSE 'free' END;

                INSERT INTO seats (screening_id, row_label, col_num, status, type)
                VALUES (screening_rec.id, row_label, col_num, seat_status, seat_type);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
