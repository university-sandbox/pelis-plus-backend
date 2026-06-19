-- ================================================================
-- SEED: TODAY
-- Populates analytics data for today only (hourly view).
-- Safe to re-run: deletes previous seed data before inserting.
-- Usage: psql "$DATABASE_URL" -f backend/scripts/seed-today.sql
-- ================================================================

DO $$
DECLARE
  -- Period: today from midnight UTC to now
  v_period_start      TIMESTAMPTZ := date_trunc('day', NOW() AT TIME ZONE 'UTC');
  v_period_end        TIMESTAMPTZ := NOW();
  v_range_secs        BIGINT;

  -- User pool
  v_user_ids    UUID[] := ARRAY[]::UUID[];
  v_user_id     UUID;
  v_first_names TEXT[] := ARRAY['Carlos','Ana','Luis','María','Pedro','Laura','Jorge','Sofía'];
  v_last_names  TEXT[] := ARRAY['García','López','Martínez','Rodríguez','Hernández','González','Pérez','Díaz'];
  v_bcrypt      TEXT   := '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

  -- Movies (dynamic)
  v_movie_ids    UUID[] := ARRAY[]::UUID[];
  v_movie_titles TEXT[] := ARRAY[]::TEXT[];
  v_tmp_movie    RECORD;

  -- Rooms (dynamic)
  v_room_ids      UUID[] := ARRAY[]::UUID[];
  v_room_names    TEXT[] := ARRAY[]::TEXT[];
  v_venue_names   TEXT[] := ARRAY[]::TEXT[];
  v_room_rows_arr INT[]  := ARRAY[]::INT[];
  v_room_cols_arr INT[]  := ARRAY[]::INT[];
  v_tmp_room      RECORD;

  -- Snacks (dynamic)
  v_snack_ids    UUID[]    := ARRAY[]::UUID[];
  v_snack_names  TEXT[]    := ARRAY[]::TEXT[];
  v_snack_prices NUMERIC[] := ARRAY[]::NUMERIC[];
  v_tmp_snack    RECORD;

  -- Screening slot options (5 showings throughout the day)
  v_slot_times   TIME[]    := ARRAY['10:00'::TIME,'13:30'::TIME,'16:00'::TIME,'19:00'::TIME,'22:00'::TIME];
  v_slot_formats TEXT[]    := ARRAY['standard','3d','standard','imax','standard'];
  v_slot_prices  NUMERIC[] := ARRAY[18.00, 22.00, 18.00, 28.00, 18.00];

  -- Screening tracking arrays
  v_scr_ids       UUID[]    := ARRAY[]::UUID[];
  v_scr_movie_idx INT[]     := ARRAY[]::INT[];
  v_scr_room_idx  INT[]     := ARRAY[]::INT[];
  v_scr_dates     DATE[]    := ARRAY[]::DATE[];
  v_scr_times     TIME[]    := ARRAY[]::TIME[];
  v_scr_formats   TEXT[]    := ARRAY[]::TEXT[];
  v_scr_prices    NUMERIC[] := ARRAY[]::NUMERIC[];
  v_scr_id        UUID;

  -- Order variables
  v_order_id        UUID;
  v_order_ticket_id UUID;
  v_booking_code    TEXT;
  v_qr_data         TEXT;
  v_num_tickets     INT;
  v_num_snacks      INT;
  v_seat_ids        UUID[];
  v_seat_labels     TEXT[];
  v_subtotal        NUMERIC;
  v_discount        NUMERIC;
  v_snack_total     NUMERIC;
  v_total           NUMERIC;
  v_has_membership  BOOLEAN;
  v_created_at      TIMESTAMPTZ;
  v_offset_secs     BIGINT;
  v_snack_picks     INT[] := ARRAY[]::INT[];

  -- Index picks
  v_scr_idx_pick   INT;
  v_snack_idx_pick INT;
  v_movie_idx_pick INT;
  v_room_idx_pick  INT;

  -- Seat generation
  v_r         INT;
  v_c         INT;
  v_row_label TEXT;
  v_seat_type TEXT;
  v_mid_row   INT;

  -- Cleanup
  v_seed_scr_ids UUID[];

  -- Counters
  i INT; j INT; k INT;
BEGIN

  -- ────────────────────────────────────────
  -- 1. CLEANUP previous seed data
  -- ────────────────────────────────────────

  SELECT ARRAY_AGG(DISTINCT ot.screening_id)
  INTO v_seed_scr_ids
  FROM order_tickets ot
  JOIN orders o ON ot.order_id = o.id
  JOIN app_users u ON o.user_id = u.id
  WHERE u.email LIKE '%@seed.pelisplus.com';

  DELETE FROM active_memberships
  WHERE user_id IN (SELECT id FROM app_users WHERE email LIKE '%@seed.pelisplus.com');

  DELETE FROM tickets
  WHERE order_id IN (
    SELECT id FROM orders
    WHERE user_id IN (SELECT id FROM app_users WHERE email LIKE '%@seed.pelisplus.com')
  );

  DELETE FROM order_snacks
  WHERE order_id IN (
    SELECT id FROM orders
    WHERE user_id IN (SELECT id FROM app_users WHERE email LIKE '%@seed.pelisplus.com')
  );

  DELETE FROM order_tickets
  WHERE order_id IN (
    SELECT id FROM orders
    WHERE user_id IN (SELECT id FROM app_users WHERE email LIKE '%@seed.pelisplus.com')
  );

  DELETE FROM orders
  WHERE user_id IN (SELECT id FROM app_users WHERE email LIKE '%@seed.pelisplus.com');

  IF v_seed_scr_ids IS NOT NULL THEN
    DELETE FROM seats      WHERE screening_id = ANY(v_seed_scr_ids);
    DELETE FROM screenings WHERE id           = ANY(v_seed_scr_ids);
  END IF;

  DELETE FROM app_users WHERE email LIKE '%@seed.pelisplus.com';
  RAISE NOTICE '[seed-today] Cleanup complete.';

  -- ────────────────────────────────────────
  -- 2. DEMO USERS (8 users, 4 with memberships)
  --    Users registered today (for the new-users chart)
  -- ────────────────────────────────────────

  FOR i IN 1..8 LOOP
    v_user_id := gen_random_uuid();
    INSERT INTO app_users(id, email, password, first_name, last_name, role, created_at)
    VALUES (
      v_user_id,
      'u' || i || '@seed.pelisplus.com',
      v_bcrypt,
      v_first_names[i],
      v_last_names[i],
      'USER',
      -- First 5 users registered today, last 3 earlier (for realistic rate context)
      CASE WHEN i <= 5
        THEN v_period_start + (random() * (v_period_end - v_period_start))
        ELSE v_period_start - (random() * INTERVAL '7 days')
      END
    );
    v_user_ids := v_user_ids || v_user_id;
  END LOOP;

  INSERT INTO active_memberships(id, user_id, plan_id, expires_at, tickets_used, discount_used) VALUES
    (gen_random_uuid(), v_user_ids[1], 'e0000000-0000-0000-0000-000000000001'::UUID, CURRENT_DATE + 30, 0, 0),
    (gen_random_uuid(), v_user_ids[2], 'e0000000-0000-0000-0000-000000000002'::UUID, CURRENT_DATE + 60, 0, 0),
    (gen_random_uuid(), v_user_ids[3], 'e0000000-0000-0000-0000-000000000003'::UUID, CURRENT_DATE + 90, 0, 0),
    (gen_random_uuid(), v_user_ids[4], 'e0000000-0000-0000-0000-000000000001'::UUID, CURRENT_DATE + 45, 0, 0);

  RAISE NOTICE '[seed-today] 8 users created (4 with memberships).';

  -- ────────────────────────────────────────
  -- 3. LOAD MOVIES (dynamic)
  -- ────────────────────────────────────────

  FOR v_tmp_movie IN
    SELECT id, title FROM movies WHERE active = true ORDER BY popularity DESC NULLS LAST LIMIT 5
  LOOP
    v_movie_ids    := v_movie_ids    || v_tmp_movie.id;
    v_movie_titles := v_movie_titles || v_tmp_movie.title;
  END LOOP;

  IF array_length(v_movie_ids, 1) IS NULL THEN
    RAISE EXCEPTION '[seed-today] No active movies found — cannot seed.';
  END IF;

  -- ────────────────────────────────────────
  -- 4. LOAD ROOMS (dynamic)
  -- ────────────────────────────────────────

  FOR v_tmp_room IN
    SELECT r.id, r.name, r.rows, r.cols, v.name AS vname
    FROM rooms r JOIN venues v ON r.venue_id = v.id
    WHERE r.active = true ORDER BY r.id LIMIT 4
  LOOP
    v_room_ids      := v_room_ids      || v_tmp_room.id;
    v_room_names    := v_room_names    || v_tmp_room.name;
    v_venue_names   := v_venue_names   || v_tmp_room.vname;
    v_room_rows_arr := v_room_rows_arr || v_tmp_room.rows;
    v_room_cols_arr := v_room_cols_arr || v_tmp_room.cols;
  END LOOP;

  IF array_length(v_room_ids, 1) IS NULL THEN
    RAISE EXCEPTION '[seed-today] No active rooms found — cannot seed.';
  END IF;

  -- ────────────────────────────────────────
  -- 5. LOAD SNACKS (dynamic)
  -- ────────────────────────────────────────

  FOR v_tmp_snack IN SELECT id, name, price FROM snacks LIMIT 20 LOOP
    v_snack_ids    := v_snack_ids    || v_tmp_snack.id;
    v_snack_names  := v_snack_names  || v_tmp_snack.name;
    v_snack_prices := v_snack_prices || v_tmp_snack.price;
  END LOOP;

  RAISE NOTICE '[seed-today] Loaded: % movies, % rooms, % snacks.',
    array_length(v_movie_ids, 1), array_length(v_room_ids, 1), array_length(v_snack_ids, 1);

  -- ────────────────────────────────────────
  -- 6. SCREENINGS (5, all today, one per time slot)
  -- ────────────────────────────────────────

  FOR i IN 1..5 LOOP
    v_movie_idx_pick := 1 + ((i - 1) % array_length(v_movie_ids, 1));
    v_room_idx_pick  := 1 + ((i - 1) % array_length(v_room_ids, 1));

    v_scr_id := gen_random_uuid();
    INSERT INTO screenings(id, movie_id, room_id, date, time, format, price, status)
    VALUES (
      v_scr_id,
      v_movie_ids[v_movie_idx_pick],
      v_room_ids[v_room_idx_pick],
      CURRENT_DATE,
      v_slot_times[i],
      v_slot_formats[i],
      v_slot_prices[i],
      'available'
    );

    v_scr_ids       := v_scr_ids       || v_scr_id;
    v_scr_movie_idx := v_scr_movie_idx || v_movie_idx_pick;
    v_scr_room_idx  := v_scr_room_idx  || v_room_idx_pick;
    v_scr_dates     := v_scr_dates     || CURRENT_DATE;
    v_scr_times     := v_scr_times     || v_slot_times[i];
    v_scr_formats   := v_scr_formats   || v_slot_formats[i];
    v_scr_prices    := v_scr_prices    || v_slot_prices[i];
  END LOOP;

  RAISE NOTICE '[seed-today] Created 5 screenings for today.';

  -- ────────────────────────────────────────
  -- 7. GENERATE SEATS
  -- ────────────────────────────────────────

  FOR i IN 1..array_length(v_scr_ids, 1) LOOP
    v_mid_row := GREATEST(v_room_rows_arr[v_scr_room_idx[i]] / 2, 1);
    FOR v_r IN 1..v_room_rows_arr[v_scr_room_idx[i]] LOOP
      v_row_label := CHR(64 + v_r);
      FOR v_c IN 1..v_room_cols_arr[v_scr_room_idx[i]] LOOP
        v_seat_type := CASE WHEN v_r BETWEEN v_mid_row AND v_mid_row + 1 THEN 'preferential' ELSE 'standard' END;
        INSERT INTO seats(id, screening_id, row_label, col_num, status, type)
        VALUES (gen_random_uuid(), v_scr_ids[i], v_row_label, v_c, 'free', v_seat_type);
      END LOOP;
    END LOOP;
  END LOOP;

  RAISE NOTICE '[seed-today] Seats generated.';

  -- ────────────────────────────────────────
  -- 8. ORDERS (15, spread from midnight to NOW)
  --    Ensures visible hourly bars in the analytics chart.
  -- ────────────────────────────────────────

  -- Ensure at least 10 hours of range so hourly chart looks good
  v_range_secs := GREATEST(EXTRACT(EPOCH FROM (v_period_end - v_period_start))::BIGINT, 36000);

  FOR i IN 1..15 LOOP
    -- Pick a random screening
    v_scr_idx_pick := 1 + (floor(random() * array_length(v_scr_ids, 1)))::INT;
    v_scr_id       := v_scr_ids[v_scr_idx_pick];

    -- Spread orders across the available range (uniform, cap at NOW)
    v_offset_secs := FLOOR(random() * v_range_secs)::BIGINT;
    v_created_at  := LEAST(v_period_start + make_interval(secs => v_offset_secs::FLOAT), v_period_end - INTERVAL '1 second');

    -- Random user
    v_user_id := v_user_ids[1 + (floor(random() * array_length(v_user_ids, 1)))::INT];

    -- Number of tickets
    v_num_tickets := CASE
      WHEN random() < 0.65 THEN 1
      WHEN random() < 0.88 THEN 2
      ELSE 3
    END;

    -- Membership check
    SELECT EXISTS(
      SELECT 1 FROM active_memberships
      WHERE user_id = v_user_id AND expires_at >= CURRENT_DATE
    ) INTO v_has_membership;

    -- Grab N free seats
    SELECT ARRAY_AGG(sub.sid), ARRAY_AGG(sub.slabel)
    INTO v_seat_ids, v_seat_labels
    FROM (
      SELECT id AS sid, row_label || col_num::TEXT AS slabel
      FROM seats
      WHERE screening_id = v_scr_id AND status = 'free'
      ORDER BY random()
      LIMIT v_num_tickets
    ) sub;

    IF v_seat_ids IS NULL OR array_length(v_seat_ids, 1) < v_num_tickets THEN
      CONTINUE;
    END IF;

    -- Financial calculations
    v_subtotal := v_scr_prices[v_scr_idx_pick] * array_length(v_seat_ids, 1);
    v_discount := CASE WHEN v_has_membership THEN ROUND(v_subtotal * 0.10, 2) ELSE 0.00 END;

    -- Pick snacks and accumulate total (save indices for reuse)
    v_num_snacks  := CASE WHEN random() < 0.40 THEN 0 WHEN random() < 0.75 THEN 1 ELSE 2 END;
    v_snack_picks := ARRAY[]::INT[];
    v_snack_total := 0.00;
    IF v_num_snacks > 0 AND array_length(v_snack_ids, 1) > 0 THEN
      FOR j IN 1..v_num_snacks LOOP
        v_snack_idx_pick := 1 + (floor(random() * array_length(v_snack_ids, 1)))::INT;
        v_snack_total    := v_snack_total + v_snack_prices[v_snack_idx_pick];
        v_snack_picks    := v_snack_picks || v_snack_idx_pick;
      END LOOP;
    END IF;
    v_total := v_subtotal + v_snack_total - v_discount;

    -- Insert order
    v_order_id := gen_random_uuid();
    INSERT INTO orders(id, user_id, subtotal, discount, total, status, payment_status, requires_payment, membership_tickets_applied, created_at)
    VALUES (v_order_id, v_user_id, v_subtotal, v_discount, v_total, 'confirmed', 'approved', false, 0, v_created_at);

    -- Mark seats occupied
    UPDATE seats SET status = 'occupied' WHERE id = ANY(v_seat_ids);

    -- Insert order_tickets + tickets (one per seat)
    FOR k IN 1..array_length(v_seat_ids, 1) LOOP
      v_order_ticket_id := gen_random_uuid();

      INSERT INTO order_tickets(id, order_id, screening_id, seat_id, movie_title, venue_name, room_name, screening_date, screening_time, format, price)
      VALUES (
        v_order_ticket_id,
        v_order_id,
        v_scr_id,
        v_seat_ids[k],
        v_movie_titles[v_scr_movie_idx[v_scr_idx_pick]],
        v_venue_names[v_scr_room_idx[v_scr_idx_pick]],
        v_room_names[v_scr_room_idx[v_scr_idx_pick]],
        v_scr_dates[v_scr_idx_pick],
        v_scr_times[v_scr_idx_pick],
        v_scr_formats[v_scr_idx_pick],
        v_scr_prices[v_scr_idx_pick]
      );

      v_booking_code := 'PP-' || UPPER(SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', ''), 1, 8));
      v_qr_data      := v_booking_code || '|'
                        || v_movie_titles[v_scr_movie_idx[v_scr_idx_pick]] || '|'
                        || v_scr_dates[v_scr_idx_pick]::TEXT || '|'
                        || v_seat_labels[k];

      INSERT INTO tickets(id, order_ticket_id, order_id, booking_code, qr_data, issued_at)
      VALUES (
        gen_random_uuid(),
        v_order_ticket_id,
        v_order_id,
        v_booking_code,
        v_qr_data,
        v_created_at + INTERVAL '1 minute'
      );
    END LOOP;

    -- Insert order_snacks using pre-rolled indices
    IF array_length(v_snack_picks, 1) > 0 THEN
      FOR j IN 1..array_length(v_snack_picks, 1) LOOP
        v_snack_idx_pick := v_snack_picks[j];
        INSERT INTO order_snacks(id, order_id, snack_id, snack_name, unit_price, quantity)
        VALUES (
          gen_random_uuid(),
          v_order_id,
          v_snack_ids[v_snack_idx_pick],
          v_snack_names[v_snack_idx_pick],
          v_snack_prices[v_snack_idx_pick],
          1
        );
      END LOOP;
    END IF;

  END LOOP;

  RAISE NOTICE '[seed-today] Done — 15 orders seeded for today (%s).', CURRENT_DATE;

END $$;
