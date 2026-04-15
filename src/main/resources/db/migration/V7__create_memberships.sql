CREATE TABLE membership_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    validity VARCHAR(50) NOT NULL,
    discount_percentage INTEGER NOT NULL,
    tickets_per_month INTEGER NOT NULL,
    recommended BOOLEAN DEFAULT false,
    color VARCHAR(20)
);

CREATE TABLE membership_plan_benefits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES membership_plans(id),
    label VARCHAR(100) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE active_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id),
    plan_id UUID NOT NULL REFERENCES membership_plans(id),
    expires_at DATE NOT NULL,
    tickets_used INTEGER NOT NULL DEFAULT 0,
    discount_used DECIMAL(10,2) NOT NULL DEFAULT 0
);

-- Seed plans
INSERT INTO membership_plans (id, name, price, validity, discount_percentage, tickets_per_month, recommended, color) VALUES
('e0000000-0000-0000-0000-000000000001', 'Plata', 29.00, '1 mes', 10, 2, false, '#9090A8'),
('e0000000-0000-0000-0000-000000000002', 'Oro', 59.00, '1 mes', 20, 4, true, '#F59E0B'),
('e0000000-0000-0000-0000-000000000003', 'Black', 99.00, '1 mes', 35, 8, false, '#7C3AED');

INSERT INTO membership_plan_benefits (plan_id, label, description, display_order) VALUES
('e0000000-0000-0000-0000-000000000001', '2 entradas al mes', 'Canjea hasta 2 entradas gratis por mes', 0),
('e0000000-0000-0000-0000-000000000001', '10% de descuento', '10% off en todas tus compras adicionales de entradas', 1),
('e0000000-0000-0000-0000-000000000001', 'Acceso prioritario', 'Reserva con 24 h de anticipación a usuarios normales', 2),
('e0000000-0000-0000-0000-000000000002', '4 entradas al mes', 'Canjea hasta 4 entradas gratis por mes', 0),
('e0000000-0000-0000-0000-000000000002', '20% de descuento', '20% off en todas tus compras adicionales de entradas', 1),
('e0000000-0000-0000-0000-000000000002', '15% en snacks', '15% de descuento en combos y bebidas', 2),
('e0000000-0000-0000-0000-000000000002', 'Acceso prioritario', 'Reserva con 48 h de anticipación', 3),
('e0000000-0000-0000-0000-000000000003', '8 entradas al mes', 'Canjea hasta 8 entradas gratis por mes', 0),
('e0000000-0000-0000-0000-000000000003', '35% de descuento', '35% off en entradas adicionales', 1),
('e0000000-0000-0000-0000-000000000003', '25% en snacks', '25% de descuento en toda la confitería', 2),
('e0000000-0000-0000-0000-000000000003', 'Acceso VIP', 'Reserva hasta 1 semana antes + sala VIP exclusiva', 3),
('e0000000-0000-0000-0000-000000000003', 'Acompañante gratis', '1 entrada de acompañante gratis por mes', 4);
