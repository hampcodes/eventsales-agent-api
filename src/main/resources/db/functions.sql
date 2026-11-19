-- Ejecutar en la base event_sales DESPUÉS de la primera corrida de la API
-- (las tablas events, tickets y participants las crea Hibernate con ddl-auto: update).
-- Se puede ejecutar varias veces sin error.

-- HU-06 Reporte de ventas: columnas [0] event_name, [1] tickets_sold, [2] total_revenue
DROP FUNCTION IF EXISTS report_tickets_by_event(BIGINT);

CREATE FUNCTION report_tickets_by_event(p_event_id BIGINT)
RETURNS TABLE (
    event_name    VARCHAR,
    tickets_sold  BIGINT,
    total_revenue NUMERIC
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        CAST(e.name AS VARCHAR),
        COUNT(t.id),
        CAST(COALESCE(SUM(t.price_paid), 0) AS NUMERIC)
    FROM events e
    JOIN tickets t ON t.event_id = e.id
    WHERE e.id = p_event_id
      AND t.status = 'PAID'
    GROUP BY e.name;
END;
$$;

-- HU-07 Historial de compras: columnas [0] event_name, [1] ticket_status, [2] price_paid
DROP FUNCTION IF EXISTS report_tickets_by_participant(BIGINT);

CREATE FUNCTION report_tickets_by_participant(p_participant_id BIGINT)
RETURNS TABLE (
    event_name    VARCHAR,
    ticket_status VARCHAR,
    price_paid    NUMERIC
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        CAST(e.name AS VARCHAR),
        CAST(t.status AS VARCHAR),
        CAST(t.price_paid AS NUMERIC)
    FROM tickets t
    JOIN events e ON t.event_id = e.id
    WHERE t.participant_id = p_participant_id
    ORDER BY e.date DESC;
END;
$$;

-- Prueba rápida (cambia los ids por unos existentes):
-- SELECT * FROM report_tickets_by_event(1);
-- SELECT * FROM report_tickets_by_participant(1);
