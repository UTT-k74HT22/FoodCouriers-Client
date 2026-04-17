-- =====================================================
-- Enable realtime order updates and create customer notifications on order changes
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'orders'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'notifications'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
    END IF;
END $$;

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
        WHEN 'delivered' THEN 'đã gaio thành công'
        WHEN 'cancelled' THEN 'đã bị hủy'
        ELSE COALESCE(p_status, 'đã cập nhật')
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE SET search_path = public;

CREATE OR REPLACE FUNCTION public.delivery_status_label(p_status text)
RETURNS text AS $$
BEGIN
    RETURN CASE p_status
        WHEN 'unassigned' THEN 'chưa có tài xế'
        WHEN 'searching' THEN 'đang tìm tài xế'
        WHEN 'assigned' THEN 'đã có tài xế nhận đơn'
        WHEN 'arriving_pickup' THEN 'tài xế đang đến nhà hàng'
        WHEN 'waiting_pickup' THEN 'tài xế đang chờ lấy hàng'
        WHEN 'picked_up' THEN 'tài xế đã lấy hàng'
        WHEN 'delivering' THEN 'tài xế đang giao hàng'
        WHEN 'delivered' THEN 'đã giao hàng thành công'
        WHEN 'failed' THEN 'giao hàng thất bại'
        ELSE COALESCE(p_status, 'đã cập nhật')
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE SET search_path = public;

CREATE OR REPLACE FUNCTION public.notify_customer_on_order_update()
RETURNS trigger AS $$
DECLARE
    v_title text;
    v_body text;
BEGIN
    IF TG_OP <> 'UPDATE' THEN
        RETURN NEW;
    END IF;

    IF COALESCE(OLD.status, '') = COALESCE(NEW.status, '')
       AND COALESCE(OLD.delivery_status, '') = COALESCE(NEW.delivery_status, '') THEN
        RETURN NEW;
    END IF;

    IF NEW.user_id IS NULL THEN
        RETURN NEW;
    END IF;

    v_title := 'Cập nhật đơn hàng ' || COALESCE(NEW.order_code, '');

    IF COALESCE(OLD.status, '') <> COALESCE(NEW.status, '') THEN
        v_body := 'Đơn hàng của bạn ' || public.order_status_label(NEW.status) || '.';
    ELSE
        v_body := 'Trạng thái giao hàng ' || public.delivery_status_label(NEW.delivery_status) || '.';
    END IF;

    INSERT INTO public.notifications (user_id, title, body, type, data)
    VALUES (
        NEW.user_id,
        v_title,
        v_body,
        'order',
        jsonb_build_object(
            'order_id', NEW.id,
            'order_code', NEW.order_code,
            'status', NEW.status,
            'delivery_status', NEW.delivery_status
        )
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_order_status_changed_notify_customer ON public.orders;
CREATE TRIGGER on_order_status_changed_notify_customer
AFTER UPDATE OF status, delivery_status ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.notify_customer_on_order_update();
