CREATE TABLE room_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE room_layouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    rows INTEGER NOT NULL,
    cols INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    seat_map TEXT,
    active BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO room_types (id, code, name, description) VALUES
('c0000000-0000-0000-0000-000000000001', 'standard', '2D', 'Sala tradicional para funciones en formato 2D'),
('c0000000-0000-0000-0000-000000000002', '3d', '3D', 'Sala equipada para proyecciones 3D'),
('c0000000-0000-0000-0000-000000000003', 'imax', 'IMAX', 'Sala premium con pantalla y sonido de gran formato')
ON CONFLICT (code) DO NOTHING;

INSERT INTO room_layouts (id, name, rows, cols, capacity, seat_map) VALUES
('d0000000-0000-0000-0000-000000000001', 'Distribucion 8x10', 8, 10, 80, NULL),
('d0000000-0000-0000-0000-000000000002', 'Distribucion 10x12', 10, 12, 120, NULL)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE rooms ADD COLUMN room_type_id UUID;
ALTER TABLE rooms ADD COLUMN room_layout_id UUID;

UPDATE rooms
SET room_type_id = CASE
    WHEN LOWER(name) LIKE '%imax%' THEN 'c0000000-0000-0000-0000-000000000003'::uuid
    ELSE 'c0000000-0000-0000-0000-000000000001'::uuid
END;

UPDATE rooms
SET room_layout_id = CASE
    WHEN rows = 10 AND cols = 12 THEN 'd0000000-0000-0000-0000-000000000002'::uuid
    ELSE 'd0000000-0000-0000-0000-000000000001'::uuid
END;

ALTER TABLE rooms ALTER COLUMN room_type_id SET NOT NULL;
ALTER TABLE rooms ALTER COLUMN room_layout_id SET NOT NULL;

ALTER TABLE rooms
    ADD CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(id),
    ADD CONSTRAINT fk_rooms_room_layout FOREIGN KEY (room_layout_id) REFERENCES room_layouts(id);

CREATE INDEX idx_rooms_room_type ON rooms(room_type_id);
CREATE INDEX idx_rooms_room_layout ON rooms(room_layout_id);
