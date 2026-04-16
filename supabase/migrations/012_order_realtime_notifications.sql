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
        WHEN 'pending' THEN 'dang cho xac nhan'
        WHEN 'confirmed' THEN 'da duoc xac nhan'
        WHEN 'preparing' THEN 'dang duoc chuan bi'
        WHEN 'ready_for_pickup' THEN 'da san sang lay hang'
        WHEN 'delivering' THEN 'dang duoc giao'
        WHEN 'delivered' THEN 'da giao thanh cong'
        WHEN 'cancelled' THEN 'da bi huy'
        ELSE COALESCE(p_status, 'da cap nhat')
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE SET search_path = public;

CREATE OR REPLACE FUNCTION public.delivery_status_label(p_status text)
RETURNS text AS $$
BEGIN
    RETURN CASE p_status
        WHEN 'unassigned' THEN 'chua co tai xe'
        WHEN 'searching' THEN 'dang tim tai xe'
        WHEN 'assigned' THEN 'da co tai xe nhan don'
        WHEN 'arriving_pickup' THEN 'tai xe dang den nha hang'
        WHEN 'waiting_pickup' THEN 'tai xe dang cho lay hang'
        WHEN 'picked_up' THEN 'tai xe da lay hang'
        WHEN 'delivering' THEN 'tai xe dang giao hang'
        WHEN 'delivered' THEN 'da giao hang'
        WHEN 'failed' THEN 'giao hang khong thanh cong'
        ELSE COALESCE(p_status, 'da cap nhat')
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

    v_title := 'Cap nhat don hang ' || COALESCE(NEW.order_code, '');

    IF COALESCE(OLD.status, '') <> COALESCE(NEW.status, '') THEN
        v_body := 'Don hang cua ban ' || public.order_status_label(NEW.status) || '.';
    ELSE
        v_body := 'Trang thai giao hang ' || public.delivery_status_label(NEW.delivery_status) || '.';
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
