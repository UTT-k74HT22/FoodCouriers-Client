-- =====================================================
-- RPC Functions for Food Ordering App
-- Version: 1.0
-- Date: 2026-04-04
-- Description: Core stored procedures for order management
-- =====================================================

-- =====================================================
-- FUNCTION: rpc_create_order
-- Purpose: Atomic order creation with validation
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

    -- Generate order code
    v_order_code := 'ORD' || TO_CHAR(NOW(), 'YYYYMMDD') || lpad(((EXTRACT(EPOCH FROM NOW()))::BIGINT % 100000)::TEXT, 5, '0');

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
        promotion_id
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
        v_promotion.id
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

    -- Create notification for user
    INSERT INTO notifications (user_id, title, body, type, data)
    VALUES (
        p_user_id,
        'Order Confirmed',
        'Your order ' || v_order_code || ' has been placed successfully',
        'order',
        jsonb_build_object('order_id', v_order_id)
    );

    -- Update promotion usage count
    IF v_promotion IS NOT NULL THEN
        UPDATE promotions SET usage_count = usage_count + 1 WHERE id = v_promotion.id;
        
        INSERT INTO promotion_usages (promotion_id, order_id, user_id, discount_amount)
        VALUES (v_promotion.id, v_order_id, p_user_id, v_discount);
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'order_id', v_order_id, 
        'order_code', v_order_code, 
        'total', v_total
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- =====================================================
-- FUNCTION: rpc_update_order_status
-- Purpose: Update order status with validation and logging
-- =====================================================
CREATE OR REPLACE FUNCTION rpc_update_order_status(
    p_order_id UUID,
    p_new_status TEXT,
    p_changed_by UUID,
    p_note TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_current_status TEXT;
    v_old_status TEXT;
    v_user_id UUID;
BEGIN
    -- Get current status and user
    SELECT status, user_id INTO v_current_status, v_user_id 
    FROM orders WHERE id = p_order_id;

    IF v_current_status IS NULL THEN
        RAISE EXCEPTION 'Order not found';
    END IF;

    -- Validate status transition
    IF v_current_status = 'pending' AND p_new_status NOT IN ('confirmed', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from pending. Can only confirm or cancel.';
    ELSIF v_current_status = 'confirmed' AND p_new_status NOT IN ('preparing', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from confirmed.';
    ELSIF v_current_status = 'preparing' AND p_new_status NOT IN ('delivering', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from preparing.';
    ELSIF v_current_status = 'delivering' AND p_new_status NOT IN ('delivered', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from delivering.';
    ELSIF v_current_status = 'delivered' THEN
        RAISE EXCEPTION 'Cannot change delivered order status.';
    ELSIF v_current_status = 'cancelled' THEN
        RAISE EXCEPTION 'Cannot change cancelled order status.';
    END IF;

    v_old_status := v_current_status;

    -- Update order status
    UPDATE orders 
    SET 
        status = p_new_status, 
        updated_at = NOW(),
        cancelled_reason = CASE WHEN p_new_status = 'cancelled' THEN p_note ELSE cancelled_reason END
    WHERE id = p_order_id;

    -- Insert status log
    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_old_status, p_new_status, p_changed_by, p_note);

    -- Create notification for customer
    INSERT INTO notifications (user_id, title, body, type, data)
    VALUES (
        v_user_id,
        'Order Status Updated',
        'Your order status has been updated to ' || UPPER(p_new_status),
        'order',
        jsonb_build_object('order_id', p_order_id, 'status', p_new_status)
    );

    RETURN jsonb_build_object(
        'success', true,
        'order_id', p_order_id,
        'old_status', v_old_status,
        'new_status', p_new_status
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- =====================================================
-- FUNCTION: rpc_apply_promotion
-- Purpose: Validate and calculate promotion discount
-- =====================================================
CREATE OR REPLACE FUNCTION rpc_apply_promotion(
    p_promotion_code TEXT,
    p_subtotal INTEGER
)
RETURNS JSONB AS $$
DECLARE
    v_promotion RECORD;
    v_discount INTEGER := 0;
    v_valid BOOLEAN := FALSE;
    v_message TEXT;
BEGIN
    -- Find promotion
    SELECT * INTO v_promotion
    FROM promotions
    WHERE code = p_promotion_code;

    IF v_promotion IS NULL THEN
        v_message := 'Invalid promotion code';
    ELSIF NOT v_promotion.is_active THEN
        v_message := 'Promotion is inactive';
    ELSIF v_promotion.start_date > NOW() THEN
        v_message := 'Promotion has not started yet';
    ELSIF v_promotion.end_date < NOW() THEN
        v_message := 'Promotion has expired';
    ELSIF v_promotion.usage_limit IS NOT NULL AND v_promotion.usage_count >= v_promotion.usage_limit THEN
        v_message := 'Promotion usage limit reached';
    ELSIF v_promotion.min_order IS NOT NULL AND p_subtotal < v_promotion.min_order THEN
        v_message := 'Minimum order amount not met';
    ELSE
        v_valid := TRUE;
        v_message := 'Promotion applied successfully';
        
        -- Calculate discount
        IF v_promotion.discount_type = 'percent' THEN
            v_discount := p_subtotal * v_promotion.discount_value / 100;
            IF v_promotion.max_discount IS NOT NULL AND v_discount > v_promotion.max_discount THEN
                v_discount := v_promotion.max_discount;
            END IF;
        ELSE
            v_discount := v_promotion.discount_value;
        END IF;
    END IF;

    RETURN jsonb_build_object(
        'valid', v_valid,
        'message', v_message,
        'discount', v_discount,
        'promotion_id', v_promotion.id
    );
END;
$$ LANGUAGE plpgsql SET search_path = public;

-- =====================================================
-- FUNCTION: rpc_recompute_restaurant_rating
-- Purpose: Recalculate restaurant average rating
-- =====================================================
CREATE OR REPLACE FUNCTION rpc_recompute_restaurant_rating(p_restaurant_id UUID)
RETURNS VOID AS $$
DECLARE
    v_avg_rating DECIMAL(3,2);
    v_review_count INTEGER;
BEGIN
    SELECT 
        COALESCE(AVG(rating), 0)::DECIMAL(3,2),
        COUNT(*)
    INTO v_avg_rating, v_review_count
    FROM reviews
    WHERE restaurant_id = p_restaurant_id AND is_visible = true;

    UPDATE restaurants
    SET rating = v_avg_rating, review_count = v_review_count
    WHERE id = p_restaurant_id;
END;
$$ LANGUAGE plpgsql SET search_path = public;
