# Roadmap - Food Ordering App MVP

## Phase Overview

| Phase | Name | Weeks | Focus |
|-------|------|-------|-------|
| 1 | Foundation | 1-3 | Setup + Auth + Catalog |
| 2 | Order Flow | 4-6 | Cart + Checkout + Order Mgmt |
| 3 | Engagement | 7-9 | Reviews + Promotions + Notifications |
| 4 | Polish | 10-12 | QA + UAT + Launch Prep |

## Phase 1: Foundation (Weeks 1-3)

### Week 1: Discovery & Scope
- [ ] Finalize MVP scope
- [ ] Define user roles (customer, staff, admin)
- [ ] Define order lifecycle workflow
- [ ] Create assumption log
- [ ] Risk assessment v1

### Week 2: Architecture & Database
- [ ] Solution architecture design
- [ ] Database schema (ERD)
- [ ] RLS policy design
- [ ] Supabase integration contracts
- [ ] Security design

### Week 3: Project Setup
- [ ] Initialize Android project
- [ ] Setup CI/CD
- [ ] Setup Supabase project
- [ ] Auth skeleton
- [ ] Design system base

**Phase 1 Deliverables:**
- Project structure
- Supabase schema with RLS
- Basic auth flow working
- Architecture document

## Phase 2: Order Flow (Weeks 4-6)

### Week 4: Auth + Profile
- [ ] Login/Register flow
- [ ] Profile management
- [ ] Address book
- [ ] Session handling

### Week 5: Browse + Menu
- [ ] Home screen with banners
- [ ] Restaurant listing
- [ ] Restaurant detail
- [ ] Menu browsing
- [ ] Search/filter

### Week 6: Cart + Checkout
- [ ] Cart management (Room DB)
- [ ] Add/remove items
- [ ] Checkout flow
- [ ] COD order submission
- [ ] Order success screen

**Phase 2 Deliverables:**
- Can browse restaurants and menus
- Can add to cart and checkout
- Orders created in database

## Phase 3: Engagement (Weeks 7-9)

### Week 7: My Orders
- [ ] My Orders list
- [ ] Order detail screen
- [ ] Order status tracking
- [ ] Reorder from history

### Week 8: Reviews + Profile
- [ ] Review after delivery
- [ ] Edit profile
- [ ] Manage addresses
- [ ] Notification inbox

### Week 9: Polish
- [ ] Error states
- [ ] Loading states
- [ ] Empty states
- [ ] Edge case handling

**Phase 3 Deliverables:**
- Full order history
- Reviews work
- Profile management

## Phase 4: Polish (Weeks 10-12)

### Week 10: QA Cycle 1
- [ ] Test case execution
- [ ] Bug fixing
- [ ] Performance testing

### Week 11: UAT
- [ ] UAT with seed data
- [ ] Demo preparation
- [ ] Bug fixes

### Week 12: Launch Prep
- [ ] Release build
- [ ] APK signing
- [ ] Final testing

**Phase 4 Deliverables:**
- Production-ready APK
- UAT sign-off

## Progress Tracking
Check `.planning/progress/[phase-name]/`
