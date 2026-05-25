ALTER TABLE orders
    ADD COLUMN stripe_checkout_session_id VARCHAR(255),
    ADD COLUMN stripe_checkout_url VARCHAR(2000);

CREATE INDEX idx_orders_stripe_checkout_session_id ON orders(stripe_checkout_session_id);
