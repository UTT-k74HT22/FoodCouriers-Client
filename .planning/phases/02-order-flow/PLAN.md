# Phase 2: Order Flow Plan

**Phase:** 2
**Duration:** Weeks 4-6
**Focus:** Cart + Checkout + Order Management

## Overview

Phase 2 implements the core order flow: cart management, COD checkout, and order history.

## Tasks

### Week 4: Auth + Profile

#### Task 4.1: Login Flow Refinement
- **Type:** feature
- **Module:** Client
- **Estimate:** 1d
- **Description:** Refine login flow with error handling
- **Files:** LoginActivity, LoginViewModel
- **Action:** 
  1. Add validation
  2. Add loading states
  3. Add error handling
  4. Add remember me
- **Verify:** Login handles all cases
- **Done:** Login polished

#### Task 4.2: Registration
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** Full registration flow
- **Files:** RegisterActivity, RegisterFragment, RegisterViewModel
- **Action:** 
  1. Create registration form
  2. Add validation (email, phone, password)
  3. Implement Supabase sign up
  4. Add email/phone verification
  5. Handle errors
- **Verify:** User can register successfully
- **Done:** Registration works

#### Task 4.3: Profile Management
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** View and edit user profile
- **Files:** ProfileActivity, ProfileFragment, ProfileViewModel
- **Action:** 
  1. Create profile screen
  2. Display user info
  3. Edit name, phone, avatar
  4. Upload avatar to Supabase Storage
- **Verify:** User can view and edit profile
- **Done:** Profile management works

#### Task 4.4: Address Book
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** Manage delivery addresses
- **Files:** AddressActivity, AddressFragment, AddressAdapter
- **Action:** 
  1. Create address list
  2. Add new address
  3. Edit address
  4. Delete address
  5. Set default address
  6. Store in user_addresses table
- **Verify:** User can manage addresses
- **Done:** Address book works

### Week 5: Restaurant + Menu Browse

#### Task 5.1: Home Screen
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** Main home screen with banners and categories
- **Files:** HomeActivity, HomeFragment, HomeViewModel
- **Action:** 
  1. Create home layout
  2. Display banners (horizontal)
  3. Display categories (grid)
  4. Display featured restaurants
  5. Pull to refresh
- **Verify:** Home displays correctly
- **Done:** Home screen works

#### Task 5.2: Restaurant List
- **Type:** feature
- **Module:** Client
- **Estimate:** 1d
- **Description:** Browse all restaurants
- **Files:** RestaurantListActivity, RestaurantListFragment
- **Action:** 
  1. Create restaurant list
  2. Display restaurant cards (image, name, rating, delivery fee)
  3. Filter by category
  4. Filter by open status
  5. Sort by rating/distance
- **Verify:** Restaurant list displays correctly
- **Done:** List works

#### Task 5.3: Restaurant Detail
- **Type:** feature
- **Module:** Client
- **Estimate:** 1d
- **Description:** View restaurant details and menu
- **Files:** RestaurantDetailActivity, RestaurantDetailFragment
- **Action:** 
  1. Display restaurant info (image, name, rating, time, delivery fee)
  2. Display categories with tabs
  3. Display menu items by category
  4. Show unavailable items
- **Verify:** Restaurant detail displays correctly
- **Done:** Detail works

#### Task 5.4: Menu Item Detail
- **Type:** feature
- **Module:** Client
- **Estimate:** 1d
- **Description:** View item details and add to cart
- **Files:** MenuItemDetailActivity, MenuItemDetailFragment
- **Action:** 
  1. Display item image, name, description, price
  2. Add to cart button
  3. Quantity selector
  4. Special instructions input
- **Verify:** Item detail works
- **Done:** Menu item detail works

### Week 6: Cart + Checkout

#### Task 6.1: Cart Management (Room DB)
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** Local cart using Room database
- **Files:** CartActivity, CartFragment, CartViewModel, CartDatabase, CartRepository
- **Action:** 
  1. Create Room database for cart
  2. Create CartDao
  3. Implement CartRepository
  4. Display cart items
  5. Update quantity
  6. Remove item
  7. Calculate totals
- **Verify:** Cart persists locally
- **Done:** Cart works

#### Task 6.2: Checkout Flow
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** Complete checkout process
- **Files:** CheckoutActivity, CheckoutFragment, CheckoutViewModel
- **Action:** 
  1. Select delivery address
  2. Apply promotion code
  3. Review order summary
  4. Select COD payment
  5. Confirm order
  6. Call rpc_create_order
- **Verify:** Order created successfully
- **Done:** Checkout works

#### Task 6.3: Order Success
- **Type:** feature
- **Module:** Client
- **Estimate:** 1d
- **Description:** Order confirmation screen
- **Files:** OrderSuccessActivity
- **Action:** 
  1. Display order code
  2. Display order summary
  3. Display estimated time
  4. Navigate to order detail
- **Verify:** Success screen displays correctly
- **Done:** Order success works

## Dependencies

- Task 4.1 → Task 4.2 → Task 4.3 → Task 4.4
- Task 5.1 → Task 5.2 → Task 5.3 → Task 5.4
- Task 6.1 → Task 6.2 → Task 6.3
- Task 6.2 depends on Supabase RPC (Task 5.1 in Admin Phase 2)

## Checkpoints

### Week 4 Checkpoint
- [ ] Login refined
- [ ] Registration works
- [ ] Profile management works
- [ ] Address book works

### Week 5 Checkpoint
- [ ] Home screen works
- [ ] Restaurant list works
- [ ] Restaurant detail works
- [ ] Menu item detail works

### Week 6 Checkpoint
- [ ] Cart works (local)
- [ ] Checkout works
- [ ] Order success displays

## Notes

- This phase implements Client-side while Admin does order management
- Sync with Admin on RPC contracts in Week 5
- Cart uses local Room DB - syncs to server on checkout
- Week 6 depends on Admin's rpc_create_order
