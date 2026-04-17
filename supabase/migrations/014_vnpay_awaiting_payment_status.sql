-- Keep VNPAY orders out of the restaurant/admin processing queue until payment succeeds.

ALTER TABLE public.orders
DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE public.orders
ADD CONSTRAINT orders_status_check
CHECK (
    status = ANY (
        ARRAY[
            'awaiting_payment'::text,
            'pending'::text,
            'confirmed'::text,
            'preparing'::text,
            'ready_for_pickup'::text,
            'delivering'::text,
            'delivered'::text,
            'cancelled'::text
        ]
    )
);

CREATE OR REPLACE FUNCTION public.rpc_create_order(
    p_user_id UUID,
    p_restaurant_id UUID,
    p_delivery_address TEXT,
    p_delivery_latitude DOUBLE PRECISION,
    p_delivery_longitude DOUBLE PRECISION,
    p_note TEXT,
    p_payment_method TEXT,
    p_promotion_code TEXT,
    p_items JSONB,
    p_delivery_fee INTEGER DEFAULT 0
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
    v_payment_method TEXT;
    v_initial_status TEXT;
BEGIN
    v_payment_method := COALESCE(p_payment_method, 'cod');
    v_initial_status := CASE
        WHEN v_payment_method = 'vnpay' THEN 'awaiting_payment'
        ELSE 'pending'
    END;

    SELECT * INTO v_restaurant
    FROM public.restaurants
    WHERE id = p_restaurant_id AND is_active = true AND is_open = true;

    IF v_restaurant IS NULL THEN
        RAISE EXCEPTION 'Restaurant not available';
    END IF;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT * INTO v_menu_item
        FROM public.menu_items
        WHERE id = (v_item->>'menu_item_id')::UUID AND is_available = true;

        IF v_menu_item IS NULL THEN
            RAISE EXCEPTION 'Menu item not available: %', v_item->>'menu_item_id';
        END IF;

        v_subtotal := v_subtotal + (v_menu_item.price * (v_item->>'quantity')::INTEGER);
    END LOOP;

    v_delivery_fee := COALESCE(p_delivery_fee, v_restaurant.delivery_fee);

    IF p_promotion_code IS NOT NULL AND p_promotion_code != '' THEN
        SELECT * INTO v_promotion
        FROM public.promotions
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

    v_total := v_subtotal + v_delivery_fee - v_discount;
    v_order_code := 'ORD' || TO_CHAR(NOW(), 'YYYYMMDD') || LPAD(((EXTRACT(EPOCH FROM NOW()))::BIGINT % 100000)::TEXT, 5, '0');

    INSERT INTO public.orders (
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
        v_payment_method,
        v_promotion_id,
        v_initial_status,
        'unassigned',
        'pending'
    )
    RETURNING id INTO v_order_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        SELECT * INTO v_menu_item
        FROM public.menu_items
        WHERE id = (v_item->>'menu_item_id')::UUID;

        INSERT INTO public.order_items (
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

    INSERT INTO public.order_status_logs (order_id, new_status, changed_by)
    VALUES (v_order_id, v_initial_status, p_user_id);

    IF v_promotion_id IS NOT NULL THEN
        UPDATE public.promotions SET usage_count = usage_count + 1 WHERE id = v_promotion_id;

        INSERT INTO public.promotion_usages (promotion_id, order_id, user_id, discount_amount)
        VALUES (v_promotion_id, v_order_id, p_user_id, v_discount);
    END IF;

    RETURN (
        SELECT to_jsonb(o)
        FROM (
            SELECT
                v_order_id AS id,
                v_order_code AS order_code,
                v_initial_status AS status,
                'unassigned' AS delivery_status,
                v_subtotal,
                v_delivery_fee,
                v_discount,
                v_total,
                p_delivery_address AS delivery_address,
                p_note AS note,
                v_payment_method AS payment_method,
                'pending' AS payment_status
        ) o
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.order_status_label(p_status text)
RETURNS text AS $$
BEGIN
    RETURN CASE p_status
        WHEN 'awaiting_payment' THEN 'đang chờ thanh toán'
        WHEN 'pending' THEN 'đang chờ xác nhận'
        WHEN 'confirmed' THEN 'đã được xác nhận'
        WHEN 'preparing' THEN 'đang được chuẩn bị'
        WHEN 'ready_for_pickup' THEN 'đã sẵn sàng lấy hàng'
        WHEN 'delivering' THEN 'đang được giao'
        WHEN 'delivered' THEN 'đã giao thành công'
        WHEN 'cancelled' THEN 'đã bị hủy'
        ELSE COALESCE(p_status, 'đã cập nhật')
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE SET search_path = public;
