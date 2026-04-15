CREATE TABLE snacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    image VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'active'
);

CREATE TABLE snack_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snack_id UUID NOT NULL REFERENCES snacks(id),
    label VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE snack_choices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    option_id UUID NOT NULL REFERENCES snack_options(id),
    choice VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

-- Seed snacks
INSERT INTO snacks (id, name, description, category, price) VALUES
('c0000000-0000-0000-0000-000000000001', 'Canchita Clásica', 'Canchita salada con mantequilla, recién hecha.', 'popcorn', 12.00),
('c0000000-0000-0000-0000-000000000002', 'Canchita Caramelizada', 'Canchita dulce con caramelo dorado.', 'popcorn', 14.00),
('c0000000-0000-0000-0000-000000000003', 'Gaseosa', 'Coca-Cola, Inca Kola, Sprite o Fanta.', 'drinks', 10.00),
('c0000000-0000-0000-0000-000000000004', 'Agua mineral', 'Agua sin gas 500 ml.', 'drinks', 6.00),
('c0000000-0000-0000-0000-000000000005', 'Cerveza', 'Cerveza fría 355 ml.', 'drinks', 15.00),
('c0000000-0000-0000-0000-000000000006', 'Combo Dúo', 'Canchita mediana + 2 gaseosas medianas.', 'combos', 28.00),
('c0000000-0000-0000-0000-000000000007', 'Combo Familiar', 'Canchita grande + 4 gaseosas medianas.', 'combos', 52.00),
('c0000000-0000-0000-0000-000000000008', 'Chocolates Surtidos', 'Selección de chocolates variados 150 g.', 'sweets', 16.00),
('c0000000-0000-0000-0000-000000000009', 'Gomitas', 'Bolsa de gomitas de frutas 100 g.', 'sweets', 8.00),
('c0000000-0000-0000-0000-000000000010', 'Nachos con salsa', 'Nachos crujientes con salsa cheddar.', 'extras', 18.00),
('c0000000-0000-0000-0000-000000000011', 'Hot dog', 'Salchicha con pan y mostaza.', 'extras', 14.00);

-- Snack options
INSERT INTO snack_options (id, snack_id, label, display_order) VALUES
('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Tamaño', 0),
('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'Tamaño', 0),
('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'Sabor', 0),
('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'Tamaño', 1);

INSERT INTO snack_choices (option_id, choice, display_order) VALUES
('d0000000-0000-0000-0000-000000000001', 'Pequeño', 0),
('d0000000-0000-0000-0000-000000000001', 'Mediano', 1),
('d0000000-0000-0000-0000-000000000001', 'Grande', 2),
('d0000000-0000-0000-0000-000000000002', 'Mediano', 0),
('d0000000-0000-0000-0000-000000000002', 'Grande', 1),
('d0000000-0000-0000-0000-000000000003', 'Coca-Cola', 0),
('d0000000-0000-0000-0000-000000000003', 'Inca Kola', 1),
('d0000000-0000-0000-0000-000000000003', 'Sprite', 2),
('d0000000-0000-0000-0000-000000000003', 'Fanta', 3),
('d0000000-0000-0000-0000-000000000004', 'Mediano', 0),
('d0000000-0000-0000-0000-000000000004', 'Grande', 1);
