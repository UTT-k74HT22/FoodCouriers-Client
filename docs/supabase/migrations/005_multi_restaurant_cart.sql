-- Switch carts to a user-scoped container that can hold items from multiple restaurants.
-- Restaurant grouping now happens when reading cart_items joined with menu_items/restaurants.

alter table public.carts
    drop constraint if exists carts_restaurant_id_fkey;

alter table public.carts
    drop column if exists restaurant_id;

comment on table public.carts is
    'One active cart per user. Items inside the cart may belong to multiple restaurants.';
