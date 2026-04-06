---
title: "Database Schema - Food Ordering App MVP"
description: "PostgreSQL schema design for food ordering system with Supabase"
audience: [ai-agents, developers]
tags: [database, schema, supabase, mvp]
created: 2026-04-04
status: draft
---

# Database Schema - Food Ordering App MVP

## Overview

Database schema for food ordering system with 2 Android apps (Client + Admin) using Supabase.

## Tables

### 1. users

**Purpose:** Main user table mapping to Supabase Auth

```sql
CREATE TABLE public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone TEXT,
    email TEXT NOT NULL,
    avatar_url TEXT,
    role TEXT NOT NULL CHECK (role IN ('customer', 'staff', 'admin')) DEFAULT 'customer',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for auth_id lookups
CREATE INDEX idx_users_auth_id ON public.users(auth_id);

-- Index for role-based queries
CREATE INDEX idx_users_role ON public.users(role);

-- Index for customer search
CREATE INDEX idx_users_email ON public.users(email);
CREATE INDEX idx_users_phone ON public.users(phone);
```

**RLS:** 
- Customers: R/W own profile only
- Staff: R/W assigned restaurant data
- Admin: Full access

---

### 2. user_addresses

**Purpose:** Multiple delivery addresses per user

```sql
CREATE TABLE public.user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label TEXT NOT NULL,
    full_address TEXT NOT NULL,
    district TEXT,
    city TEXT DEFAULT 'HCM',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for user address lookups
CREATE INDEX idx_user_addresses_user_id ON public.user_addresses(user_id);

-- Index for default address queries
CREATE INDEX idx_user_addresses_default ON public.user_addresses(user_id) WHERE is_default = true;
```

---

### 3. restaurants

**Purpose:** Restaurant information

```sql
CREATE TABLE public.restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    address TEXT NOT NULL,
    phone TEXT,
    image_url TEXT,
    rating DECIMAL(3,2) DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_open BOOLEAN NOT NULL DEFAULT true,
    open_time TIME DEFAULT '08:00:00',
    close_time TIME DEFAULT '22:00:00',
    delivery_fee INTEGER DEFAULT 0,
    min_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for restaurant browsing
CREATE INDEX idx_restaurants_active ON public.restaurants(is_active, is_open);
CREATE INDEX idx_restaurants_rating ON public.restaurants(rating DESC);
CREATE INDEX idx_restaurants_name ON public.restaurants USING gin(name gin_trgm_ops);
```

---

### 4. restaurant_staff

**Purpose:** Assign staff/admin to restaurants for multi-restaurant support

```sql
CREATE TABLE public.restaurant_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_in_restaurant TEXT NOT NULL CHECK (role_in_restaurant IN ('manager', 'operator')) DEFAULT 'operator',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(restaurant_id, user_id)
);

-- Index for staff lookups
CREATE INDEX idx_restaurant_staff_user ON public.restaurant_staff(user_id);
CREATE INDEX idx_restaurant_staff_restaurant ON public.restaurant_staff(restaurant_id);
```

---

### 5. categories

**Purpose:** Food categories (appetizers, main dishes, drinks, etc.)

```sql
CREATE TABLE public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    image_url TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for ordering
CREATE INDEX idx_categories_sort_order ON public.categories(sort_order);
```

---

### 6. menu_items

**Purpose:** Menu items for each restaurant

```sql
CREATE TABLE public.menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    description TEXT,
    price INTEGER NOT NULL,
    image_url TEXT,
    is_available BOOLEAN NOT NULL DEFAULT true,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Critical indexes for menu browsing
CREATE INDEX idx_menu_items_restaurant ON public.menu_items(restaurant_id);
CREATE INDEX idx_menu_items_category ON public.menu_items(category_id);
CREATE INDEX idx_menu_items_restaurant_available ON public.menu_items(restaurant_id, is_available);
CREATE INDEX idx_menu_items_name_gin ON public.menu_items USING gin(name gin_trgm_ops);
```

---

### 7. carts (Server-side - Phase 2)

**Purpose:** Server-side cart for multi-device sync

```sql
CREATE TABLE public.carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

CREATE INDEX idx_carts_user ON public.carts(user_id);
```

---

### 8. cart_items

**Purpose:** Cart items

```sql
CREATE TABLE public.cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(cart_id, menu_item_id)
);

CREATE INDEX idx_cart_items_cart ON public.cart_items(cart_id);
```

---

### 9. orders

**Purpose:** Order header

```sql
CREATE TABLE public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE SET NULL,
    delivery_address TEXT NOT NULL,
    delivery_latitude DOUBLE PRECISION,
    delivery_longitude DOUBLE PRECISION,
    note TEXT,
    subtotal INTEGER NOT NULL,
    delivery_fee INTEGER NOT NULL DEFAULT 0,
    discount INTEGER NOT NULL DEFAULT 0,
    total INTEGER NOT NULL,
    payment_method TEXT NOT NULL CHECK (payment_method IN ('cod', 'online')) DEFAULT 'cod',
    payment_status TEXT NOT NULL CHECK (payment_status IN ('pending', 'paid', 'failed', 'refunded')) DEFAULT 'pending',
    status TEXT NOT NULL CHECK (status IN ('pending', 'confirmed', 'preparing', 'delivering', 'delivered', 'cancelled')) DEFAULT 'pending',
    cancelled_reason TEXT,
    promotion_id UUID REFERENCES promotions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Critical indexes for order queries
CREATE INDEX idx_orders_user ON public.orders(user_id);
CREATE INDEX idx_orders_restaurant ON public.orders(restaurant_id);
CREATE INDEX idx_orders_status ON public.orders(status);
CREATE INDEX idx_orders_created_at ON public.orders(created_at DESC);
CREATE INDEX idx_orders_user_status ON public.orders(user_id, status);
CREATE INDEX idx_orders_restaurant_status ON public.orders(restaurant_id, status);
```

---

### 10. order_items

**Purpose:** Order line items (snapshot of menu item at order time)

```sql
CREATE TABLE public.order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE SET NULL,
    menu_item_name TEXT NOT NULL,
    menu_item_price INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal INTEGER NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON public.order_items(order_id);
```

---

### 11. order_status_logs

**Purpose:** Audit trail for order status changes

```sql
CREATE TABLE public.order_status_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status TEXT,
    new_status TEXT NOT NULL,
    changed_by UUID NOT NULL REFERENCES users(id),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_status_logs_order ON public.order_status_logs(order_id);
CREATE INDEX idx_order_status_logs_created ON public.order_status_logs(created_at DESC);
```

---

### 12. promotions

**Purpose:** Promotion codes

```sql
CREATE TABLE public.promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    discount_type TEXT NOT NULL CHECK (discount_type IN ('percent', 'fixed')),
    discount_value INTEGER NOT NULL,
    min_order INTEGER DEFAULT 0,
    max_discount INTEGER,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    usage_limit INTEGER,
    usage_count INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promotions_code ON public.promotions(code);
CREATE INDEX idx_promotions_active ON public.promotions(is_active, start_date, end_date);
```

---

### 13. promotion_usages

**Purpose:** Track each promotion usage

```sql
CREATE TABLE public.promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    discount_amount INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(order_id)
);

CREATE INDEX idx_promotion_usages_promotion ON public.promotion_usages(promotion_id);
CREATE INDEX idx_promotion_usages_user ON public.promotion_usages(user_id);
```

---

### 14. reviews

**Purpose:** Customer reviews after delivery

```sql
CREATE TABLE public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    is_visible BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(order_id)
);

CREATE INDEX idx_reviews_restaurant ON public.reviews(restaurant_id);
CREATE INDEX idx_reviews_user ON public.reviews(user_id);
```

---

### 15. banners

**Purpose:** Home screen banners

```sql
CREATE TABLE public.banners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    image_url TEXT NOT NULL,
    link_type TEXT CHECK (link_type IN ('restaurant', 'category', 'url', 'none')),
    link_value TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_banners_active ON public.banners(is_active, start_date, end_date);
```

---

### 16. notifications

**Purpose:** In-app notifications

```sql
CREATE TABLE public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_read ON public.notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON public.notifications(created_at DESC);
```

---

### 17. admin_logs

**Purpose:** Audit admin actions

```sql
CREATE TABLE public.admin_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id),
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    old_data JSONB,
    new_data JSONB,
    ip_address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_logs_admin ON public.admin_logs(admin_id);
CREATE INDEX idx_admin_logs_entity ON public.admin_logs(entity_type, entity_id);
CREATE INDEX idx_admin_logs_created ON public.admin_logs(created_at DESC);
```

---

### 18. payment_transactions (Phase 2)

**Purpose:** Prepare for online payment

```sql
CREATE TABLE public.payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    provider TEXT NOT NULL,
    external_txn_id TEXT,
    amount INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('pending', 'success', 'failed')),
    raw_response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_transactions_order ON public.payment_transactions(order_id);
```

---

## Functions (RPC)

### 1. rpc_create_order

Atomic order creation with validation:

```sql
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
BEGIN
    -- Validate restaurant
    SELECT * INTO v_restaurant
    FROM restaurants
    WHERE id = p_restaurant_id AND is_active = true AND is_open = true;

    IF v_restaurant IS NULL THEN
        RAISE EXCEPTION 'Restaurant not available';
    END IF;

    -- Calculate subtotal from items
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        v_subtotal := v_subtotal + (v_item->>'price')::INTEGER * (v_item->>'quantity')::INTEGER;
    END LOOP;

    -- Get delivery fee
    v_delivery_fee := v_restaurant.delivery_fee;

    -- Apply promotion if provided
    IF p_promotion_code IS NOT NULL THEN
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

    v_total := v_subtotal + v_delivery_fee - v_discount;

    -- Generate order code
    v_order_code := 'ORD' || TO_CHAR(NOW(), 'YYYYMMDD') || LPAD((EXTRACT(EPOCH FROM NOW()))::BIGINT % 100000, 5, '0');

    -- Insert order
    INSERT INTO orders (order_code, user_id, restaurant_id, delivery_address, delivery_latitude, delivery_longitude, note, subtotal, delivery_fee, discount, total, payment_method, promotion_id)
    VALUES (v_order_code, p_user_id, p_restaurant_id, p_delivery_address, p_delivery_latitude, p_delivery_longitude, p_note, v_subtotal, v_delivery_fee, v_discount, v_total, p_promotion_code, p_promotion.id)
    RETURNING id INTO v_order_id;

    -- Insert order items
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        INSERT INTO order_items (order_id, menu_item_id, menu_item_name, menu_item_price, quantity, subtotal, note)
        VALUES (
            v_order_id,
            (v_item->>'menu_item_id')::UUID,
            v_item->>'name',
            (v_item->>'price')::INTEGER,
            (v_item->>'quantity')::INTEGER,
            (v_item->>'price')::INTEGER * (v_item->>'quantity')::INTEGER,
            v_item->>'note'
        );
    END LOOP;

    -- Insert status log
    INSERT INTO order_status_logs (order_id, new_status, changed_by)
    VALUES (v_order_id, 'pending', p_user_id);

    -- Create notification
    INSERT INTO notifications (user_id, title, body, type, data)
    VALUES (
        p_user_id,
        'Order Confirmed',
        'Your order ' || v_order_code || ' has been placed successfully',
        'order',
        jsonb_build_object('order_id', v_order_id)
    );

    RETURN jsonb_build_object('order_id', v_order_id, 'order_code', v_order_code, 'total', v_total);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 2. rpc_update_order_status

```sql
CREATE OR REPLACE FUNCTION rpc_update_order_status(
    p_order_id UUID,
    p_new_status TEXT,
    p_changed_by UUID,
    p_note TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    v_current_status TEXT;
    v_old_status TEXT;
BEGIN
    -- Get current status
    SELECT status INTO v_current_status FROM orders WHERE id = p_order_id;

    -- Validate transition
    IF v_current_status = 'pending' AND p_new_status NOT IN ('confirmed', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from pending';
    ELSIF v_current_status = 'confirmed' AND p_new_status NOT IN ('preparing', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from confirmed';
    ELSIF v_current_status = 'preparing' AND p_new_status NOT IN ('delivering', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from preparing';
    ELSIF v_current_status = 'delivering' AND p_new_status NOT IN ('delivered', 'cancelled') THEN
        RAISE EXCEPTION 'Invalid transition from delivering';
    ELSIF v_current_status = 'delivered' THEN
        RAISE EXCEPTION 'Cannot change delivered order';
    ELSIF v_current_status = 'cancelled' AND p_new_status != 'cancelled' THEN
        RAISE EXCEPTION 'Cannot change cancelled order';
    END IF;

    v_old_status := v_current_status;

    -- Update status
    UPDATE orders 
    SET status = p_new_status, 
        updated_at = NOW(),
        cancelled_reason = CASE WHEN p_new_status = 'cancelled' THEN p_note ELSE cancelled_reason END
    WHERE id = p_order_id;

    -- Insert status log
    INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, note)
    VALUES (p_order_id, v_old_status, p_new_status, p_changed_by, p_note);

    -- Create notification
    INSERT INTO notifications (user_id, title, body, type, data)
    SELECT 
        user_id,
        'Order Status Updated',
        'Your order status has been updated to ' || p_new_status,
        'order',
        jsonb_build_object('order_id', p_order_id)
    FROM orders WHERE id = p_order_id;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

## Views for Reports

### Daily Stats View

```sql
CREATE VIEW v_daily_stats AS
SELECT 
    DATE(orders.created_at) as date,
    COUNT(*) as total_orders,
    SUM(total) as total_revenue,
    COUNT(CASE WHEN status = 'delivered' THEN 1 END) as completed_orders,
    COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled_orders
FROM orders
GROUP BY DATE(orders.created_at)
ORDER BY DATE(orders.created_at) DESC;
```

---

## RLS Policies (Summary)

| Table | Customer | Staff | Admin |
|-------|----------|-------|-------|
| users | Own | Assigned restaurant | All |
| user_addresses | Own | - | All |
| restaurants | Read | Read assigned | All |
| menu_items | Read | Read assigned | All |
| orders | Own | Assigned restaurant | All |
| order_items | Own | Assigned restaurant | All |
| promotions | Read | Read | All |
| reviews | Own | Read | All |
| notifications | Own | Own | All |
| admin_logs | - | Read | All |

---

## Migration Files

Store in: `supabase/migrations/`
- `001_initial_schema.sql`
- `002_rpc_functions.sql`
- `003_rls_policies.sql`
- `004_seed_data.sql`

---

## Seed Data

```sql
-- Categories
INSERT INTO categories (name, sort_order) VALUES 
    ('Món chính', 1),
    ('Món phụ', 2),
    ('Đồ uống', 3),
    ('Tráng miệng', 4);

-- Admin user (change auth_id after creating auth user)
INSERT INTO users (auth_id, full_name, email, role) 
VALUES ('00000000-0000-0000-0000-000000000001', 'Admin', 'admin@foodapp.com', 'admin');
```

---

## Next Steps

1. Run migrations in Supabase dashboard
2. Add RLS policies
3. Create seed data
4. Test RPC functions
5. Verify indexes with EXPLAIN
