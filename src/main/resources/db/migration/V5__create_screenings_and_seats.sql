CREATE TABLE screenings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id BIGINT NOT NULL REFERENCES movies(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    date DATE NOT NULL,
    time TIME NOT NULL,
    format VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active'
);

CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screening_id UUID NOT NULL REFERENCES screenings(id),
    row_label VARCHAR(5) NOT NULL,
    col_num INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'free',
    type VARCHAR(20) NOT NULL DEFAULT 'standard'
);

CREATE TABLE seat_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seat_id UUID NOT NULL REFERENCES seats(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_seats_screening ON seats(screening_id);
CREATE INDEX idx_seat_reservations_seat ON seat_reservations(seat_id);
CREATE INDEX idx_seat_reservations_expires ON seat_reservations(expires_at);
