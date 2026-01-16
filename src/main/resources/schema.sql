-- StudyFlow Veritabanı
-- 1. CONSTRAINT'LER (Silme Kısıtı ve Sayı Kısıtı)
-- Reservations tablosuna silme kısıtı: iptal edildikten sonra silinemesin diye
-- status sütunu ekliyoruz ve CHECK constraint ile durumları sınırlıyoruz
DO $$
    BEGIN
        -- Reservation status sütunu ekle (yoksa)
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'reservations' AND column_name = 'status') THEN
            ALTER TABLE reservations ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
        END IF;

        -- Cancellation reason sütunu ekle
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'reservations' AND column_name = 'cancellation_reason') THEN
            ALTER TABLE reservations ADD COLUMN cancellation_reason VARCHAR(255);
        END IF;

        -- Cancelled_at timestamp sütunu ekle
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'reservations' AND column_name = 'cancelled_at') THEN
            ALTER TABLE reservations ADD COLUMN cancelled_at TIMESTAMP;
        END IF;
    END $$;

-- Status CHECK constraint (sadece geçerli durumlar)
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_reservation_status') THEN
            ALTER TABLE reservations
                ADD CONSTRAINT chk_reservation_status
                    CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'));
        END IF;
    END $$;

-- Kullanıcı aynı anda maksimum 3 aktif rezervasyona sahip olabilir (Trigger ile kontrol edilecek)
-- Kullanıcı aynı zaman diliminde birden fazla rezervasyon yapamaz (Trigger ile kontrol edilecek)

-- 2. INDEX OLUŞTURMA (Arama Performansı İçin)

-- Rezervasyon aramaları için composite index
CREATE INDEX IF NOT EXISTS idx_reservation_search
    ON reservations (reservation_date, start_time, end_time, seat_id);

-- Kullanıcı rezervasyonları için index
CREATE INDEX IF NOT EXISTS idx_reservation_user_date
    ON reservations (user_id, reservation_date);

-- Facility name üzerinde index (tesis arama için)
CREATE INDEX IF NOT EXISTS idx_facility_name
    ON facilities (LOWER(name));

-- Rezervasyon status için index
CREATE INDEX IF NOT EXISTS idx_reservation_status
    ON reservations (status, reservation_date);

-- 3. SEQUENCE OLUŞTURMA (Otomatik ID Atama)
-- Reservation log için sequence
CREATE SEQUENCE IF NOT EXISTS reservation_log_seq START WITH 1 INCREMENT BY 1;

-- Reservation audit log tablosu
CREATE TABLE IF NOT EXISTS reservation_audit_log (
                                                     id BIGINT PRIMARY KEY DEFAULT nextval('reservation_log_seq'),
                                                     reservation_id BIGINT,
                                                     action_type VARCHAR(20) NOT NULL,
                                                     action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     user_id BIGINT,
                                                     old_status VARCHAR(20),
                                                     new_status VARCHAR(20),
                                                     details TEXT
);

-- 4. VIEW OLUŞTURMA (Aktif Rezervasyonlar Görünümü)

-- Aktif Rezervasyonlar View'ı (Arayüzden çağrılacak)
CREATE OR REPLACE VIEW vw_active_reservations AS
SELECT
    r.id AS reservation_id,
    r.reservation_date,
    r.start_time,
    r.end_time,
    r.status,
    u.id AS user_id,
    u.first_name || ' ' || u.last_name AS user_full_name,
    u.email AS user_email,
    f.id AS facility_id,
    f.name AS facility_name,
    fb.id AS block_id,
    fb.name AS block_name,
    d.id AS desk_id,
    d.id_range AS desk_range,
    s.id AS seat_id,
    s.seat_number
FROM reservations r
         JOIN users u ON r.user_id = u.id
         JOIN facilities f ON r.facility_id = f.id
         JOIN facility_blocks fb ON r.facility_block_id = fb.id
         JOIN desks d ON r.desk_id = d.id
         JOIN seats s ON r.seat_id = s.id
WHERE r.status = 'ACTIVE'
  AND (r.reservation_date > CURRENT_DATE
    OR (r.reservation_date = CURRENT_DATE AND r.end_time > CURRENT_TIME));

-- Tesis İstatistikleri View'ı
CREATE OR REPLACE VIEW vw_facility_statistics AS
SELECT
    f.id AS facility_id,
    f.name AS facility_name,
    COUNT(DISTINCT fb.id) AS total_blocks,
    COUNT(DISTINCT d.id) AS total_desks,
    COUNT(DISTINCT s.id) AS total_seats,
    COUNT(DISTINCT CASE WHEN r.status = 'ACTIVE'
        AND (r.reservation_date > CURRENT_DATE
            OR (r.reservation_date = CURRENT_DATE AND r.end_time > CURRENT_TIME))
                            THEN r.id END) AS active_reservations,
    COUNT(DISTINCT CASE WHEN r.status = 'COMPLETED' THEN r.id END) AS completed_reservations,
    COUNT(DISTINCT CASE WHEN r.status = 'CANCELLED' THEN r.id END) AS cancelled_reservations
FROM facilities f
         LEFT JOIN facility_blocks fb ON fb.facility_id = f.id
         LEFT JOIN desks d ON d.facility_block_id = fb.id
         LEFT JOIN seats s ON s.desk_id = d.id
         LEFT JOIN reservations r ON r.seat_id = s.id
GROUP BY f.id, f.name;

-- Kullanıcı Profil İstatistikleri View'ı
CREATE OR REPLACE VIEW vw_user_profile_stats AS
SELECT
    u.id AS user_id,
    u.first_name,
    u.last_name,
    u.email,
    COUNT(DISTINCT r.id) AS total_reservations,
    COUNT(DISTINCT CASE WHEN r.status = 'COMPLETED' THEN r.id END) AS completed_reservations,
    COUNT(DISTINCT CASE WHEN r.status = 'CANCELLED' THEN r.id END) AS cancelled_reservations,
    COALESCE(SUM(CASE WHEN r.status = 'COMPLETED'
                          THEN EXTRACT(EPOCH FROM (r.end_time - r.start_time))/3600 END), 0) AS total_study_hours,
    COUNT(DISTINCT r.facility_id) AS visited_facilities
FROM users u
         LEFT JOIN reservations r ON r.user_id = u.id
GROUP BY u.id, u.first_name, u.last_name, u.email;

-- 5. SQL FONKSİYONLARI

-- FONKSİYON 1: Tesis Doluluk Oranını Hesapla (Basit Fonksiyon)
CREATE OR REPLACE FUNCTION fn_calculate_facility_occupancy(
    p_facility_id BIGINT,
    p_date DATE,
    p_start_time TIME,
    p_end_time TIME
) RETURNS TABLE (
                    total_seats INTEGER,
                    occupied_seats INTEGER,
                    occupancy_rate DECIMAL(5,2)
                ) AS $$
DECLARE
    v_total INTEGER;
    v_occupied INTEGER;
BEGIN
    -- Toplam koltuk sayısı
    SELECT COUNT(s.id) INTO v_total
    FROM seats s
             JOIN desks d ON s.desk_id = d.id
             JOIN facility_blocks fb ON d.facility_block_id = fb.id
    WHERE fb.facility_id = p_facility_id;

    -- Dolu koltuk sayısı
    SELECT COUNT(DISTINCT r.seat_id) INTO v_occupied
    FROM reservations r
             JOIN seats s ON r.seat_id = s.id
             JOIN desks d ON s.desk_id = d.id
             JOIN facility_blocks fb ON d.facility_block_id = fb.id
    WHERE fb.facility_id = p_facility_id
      AND r.reservation_date = p_date
      AND r.status = 'ACTIVE'
      AND r.start_time < p_end_time
      AND r.end_time > p_start_time;

    RETURN QUERY SELECT
                     v_total,
                     v_occupied,
                     CASE WHEN v_total > 0 THEN ROUND((v_occupied::DECIMAL / v_total) * 100, 2) ELSE 0 END;
END;
$$ LANGUAGE plpgsql;

-- FONKSİYON 2: En Uygun Zaman Önerisi (CURSOR)
CREATE OR REPLACE FUNCTION fn_suggest_best_time_slot(
    p_facility_id BIGINT,
    p_date DATE,
    p_duration_hours INTEGER
) RETURNS TABLE (
                    suggested_start TIME,
                    suggested_end TIME,
                    available_seats INTEGER,
                    occupancy_percentage DECIMAL(5,2)
                ) AS $$
DECLARE
    v_open_time TIME;
    v_close_time TIME;
    v_day_of_week VARCHAR(10);
    v_current_slot TIME;
    v_slot_end TIME;
    v_total_seats INTEGER;
    v_occupied INTEGER;
    v_best_start TIME := NULL;
    v_best_available INTEGER := 0;
    v_current_available INTEGER;

    -- CURSOR: Tüm zaman dilimlerini taramak için
    time_slot_cursor CURSOR FOR
        SELECT generate_series(
                       v_open_time,
                       v_close_time - (p_duration_hours || ' hours')::INTERVAL,
                       '30 minutes'::INTERVAL
               )::TIME AS slot_time;

    slot_rec RECORD;
BEGIN
    -- Günün adını al
    v_day_of_week := UPPER(TO_CHAR(p_date, 'DAY'));
    v_day_of_week := TRIM(v_day_of_week);

    -- Açılış kapanış saatlerini al
    SELECT fcd.open_time, fcd.close_time
    INTO v_open_time, v_close_time
    FROM facility_calendar_days fcd
             JOIN facility_calendars fc ON fcd.calendar_id = fc.id
    WHERE fc.facility_id = p_facility_id
      AND fcd.day_of_week = v_day_of_week
      AND (fcd.is_closed IS NULL OR fcd.is_closed = FALSE);

    IF v_open_time IS NULL THEN
        RETURN; -- Kapalı gün
    END IF;

    -- Toplam koltuk sayısı
    SELECT COUNT(s.id) INTO v_total_seats
    FROM seats s
             JOIN desks d ON s.desk_id = d.id
             JOIN facility_blocks fb ON d.facility_block_id = fb.id
    WHERE fb.facility_id = p_facility_id;

    -- CURSOR ile zaman dilimlerini tara
    OPEN time_slot_cursor;
    LOOP
        FETCH time_slot_cursor INTO slot_rec;
        EXIT WHEN NOT FOUND;

        v_current_slot := slot_rec.slot_time;
        v_slot_end := v_current_slot + (p_duration_hours || ' hours')::INTERVAL;

        -- Bu zaman diliminde dolu koltuk sayısı
        SELECT COUNT(DISTINCT r.seat_id) INTO v_occupied
        FROM reservations r
                 JOIN seats s ON r.seat_id = s.id
                 JOIN desks d ON s.desk_id = d.id
                 JOIN facility_blocks fb ON d.facility_block_id = fb.id
        WHERE fb.facility_id = p_facility_id
          AND r.reservation_date = p_date
          AND r.status = 'ACTIVE'
          AND r.start_time < v_slot_end
          AND r.end_time > v_current_slot;

        v_current_available := v_total_seats - v_occupied;

        -- En iyi dilimi güncelle
        IF v_current_available > v_best_available THEN
            v_best_available := v_current_available;
            v_best_start := v_current_slot;
        END IF;
    END LOOP;
    CLOSE time_slot_cursor;

    -- Sonuç döndür
    IF v_best_start IS NOT NULL THEN
        RETURN QUERY SELECT
                         v_best_start,
                         (v_best_start + (p_duration_hours || ' hours')::INTERVAL)::TIME,
                         v_best_available,
                         CASE WHEN v_total_seats > 0
                                  THEN ROUND(((v_total_seats - v_best_available)::DECIMAL / v_total_seats) * 100, 2)
                              ELSE 0 END;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- FONKSİYON 3: Kullanıcı Rezervasyon Geçmişi Raporu (RECORD)
CREATE OR REPLACE FUNCTION fn_get_user_reservation_report(
    p_user_id BIGINT,
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
) RETURNS TABLE (
                    reservation_id BIGINT,
                    facility_name VARCHAR,
                    block_name VARCHAR,
                    seat_number INTEGER,
                    reservation_date DATE,
                    start_time TIME,
                    end_time TIME,
                    duration_minutes INTEGER,
                    status VARCHAR,
                    is_cancellable BOOLEAN
                ) AS $$
DECLARE
    v_record RECORD;
    v_now TIMESTAMP := CURRENT_TIMESTAMP;
    v_cancel_deadline TIMESTAMP;
BEGIN
    FOR v_record IN
        SELECT
            r.id,
            f.name AS f_name,
            fb.name AS fb_name,
            s.seat_number AS s_num,
            r.reservation_date AS r_date,
            r.start_time AS r_start,
            r.end_time AS r_end,
            EXTRACT(EPOCH FROM (r.end_time - r.start_time))/60 AS duration,
            r.status AS r_status
        FROM reservations r
                 JOIN facilities f ON r.facility_id = f.id
                 JOIN facility_blocks fb ON r.facility_block_id = fb.id
                 JOIN seats s ON r.seat_id = s.id
        WHERE r.user_id = p_user_id
          AND (p_start_date IS NULL OR r.reservation_date >= p_start_date)
          AND (p_end_date IS NULL OR r.reservation_date <= p_end_date)
        ORDER BY r.reservation_date DESC, r.start_time DESC
        LOOP
            -- İptal edilebilirlik kontrolü (1 saat öncesine kadar)
            v_cancel_deadline := (v_record.r_date || ' ' || v_record.r_start)::TIMESTAMP - INTERVAL '1 hour';

            RETURN QUERY SELECT
                             v_record.id,
                             v_record.f_name::VARCHAR,
                             v_record.fb_name::VARCHAR,
                             v_record.s_num,
                             v_record.r_date,
                             v_record.r_start,
                             v_record.r_end,
                             v_record.duration::INTEGER,
                             v_record.r_status::VARCHAR,
                             (v_record.r_status = 'ACTIVE' AND v_now < v_cancel_deadline);
        END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 6. TRIGGER'LAR (2 Adet)

-- TRIGGER 1: Rezervasyon Olusturuldugunda - Cakisma ve Limit Kontrolu
-- Bu trigger BEFORE INSERT'te calisir ve:
-- 1. Kullanicinin ayni zaman diliminde baska rezervasyonu var mi kontrol eder
-- 2. Maksimum 3 aktif rezervasyon limitini kontrol eder
-- NOT: Sadece ACTIVE status'lu INSERT'ler icin kontrol yapar (seed data icin COMPLETED/CANCELLED bypass)
-- TRIGGER 1A: Rezervasyon Zaman Cakismasi Kontrolu
CREATE OR REPLACE FUNCTION trg_check_time_conflict()
    RETURNS TRIGGER AS $$
DECLARE
    v_conflict_count INTEGER;
BEGIN
    -- Sadece ACTIVE status'lu rezervasyonlar icin kontrol yap
    IF NEW.status != 'ACTIVE' THEN
        RETURN NEW;
    END IF;

    -- Kullanici ayni tarih ve saatte baska bir rezervasyona sahip mi?
    SELECT COUNT(*) INTO v_conflict_count
    FROM reservations
    WHERE user_id = NEW.user_id
      AND reservation_date = NEW.reservation_date
      AND status = 'ACTIVE'
      AND (start_time < NEW.end_time AND end_time > NEW.start_time);

    IF v_conflict_count > 0 THEN
        RAISE EXCEPTION 'TRIGGER_ERROR:TIME_CONFLICT:Bu zaman diliminde zaten bir rezervasyonunuz bulunmaktadir. Ayni anda birden fazla rezervasyon yapamazsiniz.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reservation_time_conflict ON reservations;
CREATE TRIGGER trg_reservation_time_conflict
    BEFORE INSERT ON reservations
    FOR EACH ROW
EXECUTE FUNCTION trg_check_time_conflict();

-- TRIGGER 1B: Maksimum Rezervasyon Limiti Kontrolu
CREATE OR REPLACE FUNCTION trg_check_reservation_limit()
    RETURNS TRIGGER AS $$
DECLARE
    v_active_count INTEGER;
    v_max_allowed INTEGER := 3;
BEGIN
    -- Sadece ACTIVE status'lu rezervasyonlar icin kontrol yap
    IF NEW.status != 'ACTIVE' THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*) INTO v_active_count
    FROM reservations
    WHERE user_id = NEW.user_id
      AND status = 'ACTIVE'
      AND (reservation_date > CURRENT_DATE
        OR (reservation_date = CURRENT_DATE AND end_time > CURRENT_TIME));

    IF v_active_count >= v_max_allowed THEN
        RAISE EXCEPTION 'TRIGGER_ERROR:MAX_RESERVATION_LIMIT:Maksimum % aktif rezervasyona sahip olabilirsiniz. Mevcut aktif rezervasyon sayiniz: %. Yeni rezervasyon yapabilmek icin mevcut rezervasyonlarinizdan birini iptal edin.',
            v_max_allowed, v_active_count;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reservation_limit ON reservations;
CREATE TRIGGER trg_reservation_limit
    BEFORE INSERT ON reservations
    FOR EACH ROW
EXECUTE FUNCTION trg_check_reservation_limit();

-- TRIGGER 1B: INSERT sonrası audit log kaydı
CREATE OR REPLACE FUNCTION trg_reservation_audit_insert()
    RETURNS TRIGGER AS $$
BEGIN
    -- Audit log kaydı
    INSERT INTO reservation_audit_log (reservation_id, action_type, user_id, new_status, details)
    VALUES (NEW.id, 'INSERT', NEW.user_id, 'ACTIVE',
            'Yeni rezervasyon oluşturuldu. Tarih: ' || NEW.reservation_date ||
            ', Saat: ' || NEW.start_time || '-' || NEW.end_time);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reservation_insert ON reservations;
CREATE TRIGGER trg_reservation_insert
    AFTER INSERT ON reservations
    FOR EACH ROW
EXECUTE FUNCTION trg_reservation_audit_insert();

-- TRIGGER 2: Rezervasyon İptal Edildiğinde - İptal Kuralı Kontrolü ve Bildirim
CREATE OR REPLACE FUNCTION trg_check_cancellation_rules()
    RETURNS TRIGGER AS $$
DECLARE
    v_reservation_datetime TIMESTAMP;
    v_cancel_deadline TIMESTAMP;
    v_now TIMESTAMP := CURRENT_TIMESTAMP;
BEGIN
    -- Sadece status değişikliğinde çalış
    IF OLD.status = 'ACTIVE' AND NEW.status = 'CANCELLED' THEN
        -- Rezervasyon başlangıç zamanını hesapla
        v_reservation_datetime := OLD.reservation_date + OLD.start_time;
        v_cancel_deadline := v_reservation_datetime - INTERVAL '1 hour';

        -- 1 saat kuralını kontrol et
        IF v_now > v_cancel_deadline THEN
            RAISE EXCEPTION 'TRIGGER_ERROR:CANCELLATION_TOO_LATE:Rezervasyon başlangıcına 1 saatten az kaldığı için iptal edilemez. Son iptal zamanı: %',
                TO_CHAR(v_cancel_deadline, 'DD.MM.YYYY HH24:MI');
        END IF;

        -- İptal zamanını kaydet
        NEW.cancelled_at := v_now;

        -- Audit log kaydı
        INSERT INTO reservation_audit_log (reservation_id, action_type, user_id, old_status, new_status, details)
        VALUES (OLD.id, 'CANCEL', OLD.user_id, 'ACTIVE', 'CANCELLED',
                'Rezervasyon iptal edildi. İptal nedeni: ' || COALESCE(NEW.cancellation_reason, 'Belirtilmedi'));

        -- Başarılı iptal bildirimi (Exception ile değil, NOTICE ile)
        RAISE NOTICE 'TRIGGER_SUCCESS:RESERVATION_CANCELLED:Rezervasyon başarıyla iptal edildi. ID: %', OLD.id;
    END IF;

    -- Tamamlanan rezervasyonlar için log
    IF OLD.status = 'ACTIVE' AND NEW.status = 'COMPLETED' THEN
        INSERT INTO reservation_audit_log (reservation_id, action_type, user_id, old_status, new_status, details)
        VALUES (OLD.id, 'COMPLETE', OLD.user_id, 'ACTIVE', 'COMPLETED',
                'Rezervasyon tamamlandı.');
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reservation_update ON reservations;
CREATE TRIGGER trg_reservation_update
    BEFORE UPDATE ON reservations
    FOR EACH ROW
EXECUTE FUNCTION trg_check_cancellation_rules();

-- 7. AGGREGATE QUERY İÇİN STORED PROCEDURE
-- En popüler tesisleri getir (HAVING ile)
CREATE OR REPLACE FUNCTION fn_get_popular_facilities()
    RETURNS TABLE (
                      facility_id BIGINT,
                      facility_name VARCHAR,
                      total_reservations BIGINT,
                      completed_reservations BIGINT
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            f.id,
            f.name::VARCHAR,
            COUNT(r.id) AS total_res,
            COUNT(CASE WHEN r.status = 'COMPLETED' THEN 1 END) AS completed_res
        FROM facilities f
                 LEFT JOIN facility_blocks fb ON fb.facility_id = f.id
                 LEFT JOIN desks d ON d.facility_block_id = fb.id
                 LEFT JOIN seats s ON s.desk_id = d.id
                 LEFT JOIN reservations r ON r.seat_id = s.id
        GROUP BY f.id, f.name
        HAVING COUNT(r.id) > (
            SELECT AVG(reservation_count)
            FROM (
                     SELECT COUNT(r2.id) AS reservation_count
                     FROM facilities f2
                              LEFT JOIN facility_blocks fb2 ON fb2.facility_id = f2.id
                              LEFT JOIN desks d2 ON d2.facility_block_id = fb2.id
                              LEFT JOIN seats s2 ON s2.desk_id = d2.id
                              LEFT JOIN reservations r2 ON r2.seat_id = s2.id
                     GROUP BY f2.id
                 ) AS avg_calc
        )
        ORDER BY total_res DESC;
END;
$$ LANGUAGE plpgsql;
-- 8. UNION / INTERSECT / EXCEPT SORGULARI İÇİN FONKSİYON
-- UNION: Tüm kullanıcı aktivitelerini birleştir
CREATE OR REPLACE FUNCTION fn_get_user_all_activities(
    p_user_id BIGINT
) RETURNS TABLE (
                    activity_type VARCHAR,
                    activity_date DATE,
                    activity_time TIME,
                    description VARCHAR
                ) AS $$
BEGIN
    RETURN QUERY
        -- Aktif rezervasyonlar
        SELECT
            'ACTIVE_RESERVATION'::VARCHAR,
            r.reservation_date,
            r.start_time,
            ('Aktif rezervasyon: ' || f.name || ' - ' || fb.name)::VARCHAR
        FROM reservations r
                 JOIN facilities f ON r.facility_id = f.id
                 JOIN facility_blocks fb ON r.facility_block_id = fb.id
        WHERE r.user_id = p_user_id AND r.status = 'ACTIVE'

        UNION ALL

        -- Tamamlanan rezervasyonlar
        SELECT
            'COMPLETED_RESERVATION'::VARCHAR,
            r.reservation_date,
            r.start_time,
            ('Tamamlanan rezervasyon: ' || f.name || ' - ' || fb.name)::VARCHAR
        FROM reservations r
                 JOIN facilities f ON r.facility_id = f.id
                 JOIN facility_blocks fb ON r.facility_block_id = fb.id
        WHERE r.user_id = p_user_id AND r.status = 'COMPLETED'

        UNION ALL

        -- İptal edilen rezervasyonlar
        SELECT
            'CANCELLED_RESERVATION'::VARCHAR,
            r.reservation_date,
            r.start_time,
            ('İptal edilen rezervasyon: ' || f.name || ' - ' || COALESCE(r.cancellation_reason, 'Sebep belirtilmedi'))::VARCHAR
        FROM reservations r
                 JOIN facilities f ON r.facility_id = f.id
        WHERE r.user_id = p_user_id AND r.status = 'CANCELLED'

        ORDER BY activity_date DESC, activity_time DESC;
END;
$$ LANGUAGE plpgsql;

-- EXCEPT: Hiç rezervasyon yapılmamış tesisler
CREATE OR REPLACE FUNCTION fn_get_facilities_without_reservations()
    RETURNS TABLE (
                      facility_id BIGINT,
                      facility_name VARCHAR,
                      address VARCHAR
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT f.id, f.name::VARCHAR, f.address::VARCHAR
        FROM facilities f

        EXCEPT

        SELECT DISTINCT f.id, f.name::VARCHAR, f.address::VARCHAR
        FROM facilities f
                 JOIN facility_blocks fb ON fb.facility_id = f.id
                 JOIN desks d ON d.facility_block_id = fb.id
                 JOIN seats s ON s.desk_id = d.id
                 JOIN reservations r ON r.seat_id = s.id;
END;
$$ LANGUAGE plpgsql;

-- 9. MEVCUT VERİLERİ GÜNCELLEME (Trigger uyumluluğu için)

-- Mevcut rezervasyonlara status ekleme
UPDATE reservations SET status = 'ACTIVE' WHERE status IS NULL;

-- Geçmiş rezervasyonları COMPLETED yap
UPDATE reservations
SET status = 'COMPLETED'
WHERE status = 'ACTIVE'
  AND (reservation_date < CURRENT_DATE
    OR (reservation_date = CURRENT_DATE AND end_time <= CURRENT_TIME));

-- 10. YARDIMCI FONKSİYON: Trigger Mesajı Kontrolü

CREATE OR REPLACE FUNCTION fn_get_last_trigger_message(
    p_reservation_id BIGINT
) RETURNS VARCHAR AS $$
DECLARE
    v_message VARCHAR;
BEGIN
    SELECT details INTO v_message
    FROM reservation_audit_log
    WHERE reservation_id = p_reservation_id
    ORDER BY action_timestamp DESC
    LIMIT 1;

    RETURN COALESCE(v_message, 'İşlem kaydı bulunamadı.');
END;
$$ LANGUAGE plpgsql;