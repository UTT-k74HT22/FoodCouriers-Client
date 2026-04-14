-- =====================================================
-- FIX: rpc_create_order function
-- Issue: 42883 - function ipad/bigint/integer/unknown does not exist
-- Fix: Ensure proper type casting for lpad function
-- =====================================================

CREATE OR REPLACE FUNCTION rpc_create_order(
    p_user_id UUID,
    p_restaurant_id UUID,
    p_delivery_address TEXT,
    p_delivery_latitude DOUBLE PRECISION,
    p_delivery_longitude DOUBLE PRECISION,
    p_note TEXT,
    p_payment_method TEXT,
    p_promotion_code TEXT,
    p_items JSONB
)
RETURNS JSONB AS $$
DECLARE
    v_order_id UUID;
    v_order_code TEXT;
    v_subtotal INTEGER := 0;
    v_discount INTEGER := 0;
    v_delivery_fee INTEGER;
    v_total INTEGER;
    v_restaurant RECORD;
    v_promotion RECORD;
    v_promotion_id UUID := NULL;
    v_item JSONB;
    v_menu_item RECORD;
BEGIN
    -- Validate restaurant exists and is available
    SELECT * INTO v_restaurant
    FROM restaurants
    WHERE id = p_restaurant_id AND is_active = true AND is_open = true;

    IF v_restaurant IS NULL THEN
        RAISE EXCEPTION 'Restaurant not available';
    END IF;

    -- Validate cart items and calculate subtotal
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        -- Get menu item details
        SELECT * INTO v_menu_item
        FROM menu_items
        WHERE id = (v_item->>'menu_item_id')::UUID AND is_available = true;

        IF v_menu_item IS NULL THEN
            RAISE EXCEPTION 'Menu item not available: %', v_item->>'menu_item_id';
        END IF;

        v_subtotal := v_subtotal + (v_menu_item.price * (v_item->>'quantity')::INTEGER);
    END LOOP;

    -- Get delivery fee
    v_delivery_fee := v_restaurant.delivery_fee;

    -- Apply promotion if provided
    IF p_promotion_code IS NOT NULL AND p_promotion_code != '' THEN
        SELECT * INTO v_promotion
        FROM promotions
        WHERE code = p_promotion_code 
            AND is_active = true 
            AND start_date <= NOW() 
            AND end_date >= NOW()
            AND (usage_limit IS NULL OR usage_count < usage_limit)
            AND (min_order IS NULL OR v_subtotal >= min_order);

        IF v_promotion IS NOT NULL THEN
            v_promotion_id := v_promotion.id;
            IF v_promotion.discount_type = 'percent' THEN
                v_discount := v_subtotal * v_promotion.discount_value / 100;
                IF v_promotion.max_discount IS NOT NULL AND v_discount > v_promotion.max_discount THEN
                    v_discount := v_promotion.max_discount;
                END IF;
            ELSE
                v_discount := v_promotion.discount_value;
            END IF;
        END IF;
    END IF;

    -- Calculate total
    v_total := v_subtotal + v_delivery_fee - v_discount;

    -- Generate order code with PROPER TYPE CASTING (cast BIGINT to TEXT before lpad)
    v_order_code := 'ORD' || TO_CHAR(NOW(), 'YYYYMMDD') || LPAD(((EXTRACT(EPOCH FROM NOW()))::BIGINT % 100000)::TEXT, 5, '0');

    -- Insert order
    INSERT INTO orders (
        order_code, 
        user_id, 
        restaurant_id, 
        delivery_address, 
        delivery_latitude, 
        delivery_longitude, 
        note, 
        subtotal, 
        delivery_fee, 
        discount, 
        total, 
        payment_method,
        promotion_id,
        status,
        delivery_status,
        payment_status
    )
    VALUES (
        v_order_code, 
        p_user_id, 
        p_restaurant_id, 
        p_delivery_address, 
        p_delivery_latitude, 
        p_delivery_longitude, 
        p_note, 
        v_subtotal, 
        v_delivery_fee, 
        v_discount, 
        v_total, 
        COALESCE(p_payment_method, 'cod'),
        v_promotion_id,
        'pending',
        'unassigned',
        'pending'
    )
    RETURNING id INTO v_order_id;

    -- Insert order items (snapshot)
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT * INTO v_menu_item
        FROM menu_items
        WHERE id = (v_item->>'menu_item_id')::UUID;

        INSERT INTO order_items (
            order_id, 
            menu_item_id, 
            menu_item_name, 
            menu_item_price, 
            quantity, 
            subtotal, 
            note
        )
        VALUES (
            v_order_id,
            (v_item->>'menu_item_id')::UUID,
            v_menu_item.name,
            v_menu_item.price,
            (v_item->>'quantity')::INTEGER,
            v_menu_item.price * (v_item->>'quantity')::INTEGER,
            v_item->>'note'
        );
    END LOOP;

    -- Insert status log
    INSERT INTO order_status_logs (order_id, new_status, changed_by)
    VALUES (v_order_id, 'pending', p_user_id);

    -- Return the created order
    RETURN (
        SELECT to_jsonb(o)
        FROM (
            SELECT 
                v_order_id AS id,
                v_order_code AS order_code,
                'pending' AS status,
                'unassigned' AS delivery_status,
                v_subtotal,
                v_delivery_fee,
                v_discount,
                v_total,
                p_delivery_address AS delivery_address,
                p_note AS note,
                COALESCE(p_payment_method, 'cod') AS payment_method,
                'pending' AS payment_status
        ) o
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
