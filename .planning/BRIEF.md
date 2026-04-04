# Project Brief - Food Ordering App MVP

## Vision
Build a food ordering system with 2 Android apps (Client + Admin) using Java + XML + Supabase, enabling:
- Customers to browse restaurants, order food, track orders
- Admin/Staff to manage restaurants, menus, orders, promotions

## Goals (MVP - 14 weeks)
1. Launch pilot with 1-5 pilot restaurants
2. Enable end-to-end ordering flow (browse → order → deliver)
3. Simple COD payment only
4. Basic admin operations

## Scope

### In Scope (MVP)
- Client App: Auth, Browse, Cart, COD Checkout, Order History
- Admin App: Restaurant/Menu CRUD, Order Management, Basic Reports
- Supabase: Auth, Database, Storage, RLS, Basic RPC

### Out of Scope (Phase 2+)
- Online payment
- Driver/Shipper app
- Realtime tracking
- Loyalty points
- Multi-language

## Tech Stack
- **Mobile**: Android Native Java + XML
- **Backend**: Supabase (Auth, Postgres, Storage, Edge Functions)
- **Local DB**: Room for cart cache

## Constraints
- Single city launch
- Single cart per order (one restaurant at a time)
- COD payment only in MVP
- No driver app - restaurant self-delivery or manual status update

## Success Criteria
- [ ] End-to-end order flow works
- [ ] Admin can manage restaurants and orders
- [ ] RLS properly separates customer/admin data
- [ ] MVP demo with seed data successful
- [ ] APK builds for both apps

## Timeline
- Phase 1 (Foundation): Weeks 1-3
- Phase 2 (Order Flow): Weeks 4-6
- Phase 3 (Engagement): Weeks 7-9
- Phase 4 (Polish): Weeks 10-12

## Team
- Android Devs: 2
- Backend/Supabase: 1
- QA: 1
- PO/BA: 1
