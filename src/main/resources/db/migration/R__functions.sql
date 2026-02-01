CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
    $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_order_total()
    RETURNS TRIGGER AS $$
DECLARE
    target_order_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_order_id := OLD.order_id;
    ELSE
        target_order_id := NEW.order_id;
    END IF;

    UPDATE orders
    SET total_amount = (
        SELECT COALESCE(SUM(quantity * price_per_unit), 0)
        FROM order_items
        WHERE order_id = target_order_id
    )
    WHERE id = target_order_id;

    RETURN COALESCE(NEW, OLD);
END;
    $$ LANGUAGE plpgsql;
