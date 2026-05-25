ALTER TABLE orders
    ADD COLUMN membership_tickets_applied INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN requires_payment BOOLEAN NOT NULL DEFAULT true;
