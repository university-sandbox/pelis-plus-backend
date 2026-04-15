CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    izipay_form_token VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE order_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    screening_id UUID NOT NULL REFERENCES screenings(id),
    seat_id UUID NOT NULL REFERENCES seats(id),
    movie_title VARCHAR(500) NOT NULL,
    venue_name VARCHAR(255) NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    screening_date DATE NOT NULL,
    screening_time TIME NOT NULL,
    format VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL
);

CREATE TABLE order_snacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    snack_id UUID NOT NULL REFERENCES snacks(id),
    snack_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    selected_options JSONB
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_ticket_id UUID NOT NULL UNIQUE REFERENCES order_tickets(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    booking_code VARCHAR(20) NOT NULL UNIQUE,
    qr_data VARCHAR(500) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_order_tickets_order ON order_tickets(order_id);
CREATE INDEX idx_order_snacks_order ON order_snacks(order_id);
CREATE INDEX idx_tickets_order ON tickets(order_id);
