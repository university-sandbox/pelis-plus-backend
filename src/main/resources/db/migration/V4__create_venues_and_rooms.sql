CREATE TABLE venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    active BOOLEAN DEFAULT true
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id UUID NOT NULL REFERENCES venues(id),
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL,
    rows INTEGER NOT NULL,
    cols INTEGER NOT NULL,
    active BOOLEAN DEFAULT true
);

-- Seed venues
INSERT INTO venues (id, name, address, city) VALUES
('a0000000-0000-0000-0000-000000000001', 'PelisPlus Miraflores', 'Av. Larco 345', 'Lima'),
('a0000000-0000-0000-0000-000000000002', 'PelisPlus San Isidro', 'Av. Rivera Navarrete 720', 'Lima'),
('a0000000-0000-0000-0000-000000000003', 'PelisPlus Surco', 'Av. Primavera 1256', 'Lima');

-- Seed rooms
INSERT INTO rooms (id, venue_id, name, capacity, rows, cols) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Sala 1', 80, 8, 10),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Sala IMAX', 120, 10, 12),
('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', 'Sala 1', 80, 8, 10),
('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000003', 'Sala 1', 80, 8, 10);
