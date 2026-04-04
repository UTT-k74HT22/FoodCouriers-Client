# Phase 1: Foundation Plan

**Phase:** 1
**Duration:** Weeks 1-3
**Focus:** Setup + Auth + Catalog

## Overview

Phase 1 focuses on setting up the foundation: project infrastructure, authentication, and basic catalog browsing (restaurants + menus).

## Tasks

### Week 1: Discovery & Scope

#### Task 1.1: Scope Finalization
- **Type:** feature
- **Module:** Planning
- **Estimate:** 2d
- **Description:** Finalize MVP scope, define user roles, create assumption log
- **Files:** 
- **Action:** 
  1. Review README.md requirements
  2. Create scope document
  3. Define customer/staff/admin roles
  4. Document assumptions
- **Verify:** Scope document approved
- **Done:** Scope document created

#### Task 1.2: Order Lifecycle Design
- **Type:** feature
- **Module:** Business
- **Estimate:** 2d
- **Description:** Define order status workflow
- **Files:** 
- **Action:** 
  1. Design order state machine
  2. Define valid status transitions
  3. Document status flow
- **Verify:** Order flow diagram complete
- **Done:** Order lifecycle documented

#### Task 1.3: Risk Assessment
- **Type:** feature
- **Module:** Planning
- **Estimate:** 1d
- **Description:** Identify initial risks
- **Files:** 
- **Action:** 
  1. List potential risks
  2. Assign probability and impact
  3. Define mitigations
- **Verify:** Risk register created
- **Done:** Risk log created

### Week 2: Architecture & Database

#### Task 2.1: Database Schema Design
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 3d
- **Description:** Design ERD and database schema
- **Files:** 
- **Action:** 
  1. Create tables: users, restaurants, categories, menu_items, orders, etc.
  2. Define relationships
  3. Create indexes
  4. Add timestamps
- **Verify:** ERD matches requirements
- **Done:** Schema created

#### Task 2.2: RLS Policy Design
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 2d
- **Description:** Design Row Level Security policies
- **Files:** 
- **Action:** 
  1. Define customer policies
  2. Define staff policies
  3. Define admin policies
- **Verify:** RLS policies documented
- **Done:** Policy design complete

#### Task 2.3: App Architecture Design
- **Type:** feature
- **Module:** Architecture
- **Estimate:** 2d
- **Description:** Define app architecture
- **Files:** 
- **Action:** 
  1. Define MVVM architecture
  2. Define data layer (Repository pattern)
  3. Define API integration
- **Verify:** Architecture documented
- **Done:** Architecture ready

### Week 3: Project Setup

#### Task 3.1: Client Project Initialization
- **Type:** feature
- **Module:** Android
- **Estimate:** 2d
- **Description:** Initialize Client Android project
- **Files:** build.gradle, AndroidManifest.xml
- **Action:** 
  1. Create Android project
  2. Setup dependencies (Retrofit, Room, Glide, Supabase client)
  3. Configure build variants
- **Verify:** Project compiles
- **Done:** Empty shell builds

#### Task 3.2: Supabase Setup
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 1d
- **Description:** Setup Supabase project
- **Files:** 
- **Action:** 
  1. Create Supabase project
  2. Setup storage buckets for images
  3. Configure environment
- **Verify:** Supabase accessible
- **Done:** Supabase configured

#### Task 3.3: Authentication
- **Type:** feature
- **Module:** Client
- **Estimate:** 3d
- **Description:** Implement authentication flow
- **Files:** AuthActivity, LoginFragment, RegisterFragment
- **Action:** 
  1. Setup Supabase client in app
  2. Implement login
  3. Implement registration
  4. Implement session management
  5. Handle password reset
- **Verify:** User can login/register
- **Done:** Auth functional

#### Task 3.4: Design System
- **Type:** feature
- **Module:** Client
- **Estimate:** 2d
- **Description:** Setup base design system
- **Files:** colors.xml, themes.xml, styles.xml
- **Action:** 
  1. Define color palette
  2. Create base themes
  3. Define common styles
- **Verify:** Theme applied
- **Done:** Design system ready

## Dependencies

- Task 1.1 → Task 1.2 → Task 1.3
- Task 2.1 → Task 2.2 → Task 2.3
- Task 3.1 → Task 3.2 → Task 3.3, Task 3.4

## Checkpoints

### Week 1 Checkpoint
- [ ] Scope approved
- [ ] Order lifecycle defined
- [ ] Risks identified

### Week 2 Checkpoint
- [ ] Database schema complete
- [ ] RLS policies designed
- [ ] Architecture documented

### Week 3 Checkpoint
- [ ] Project builds
- [ ] Supabase accessible
- [ ] Auth works
- [ ] Design system ready

## Notes

- This plan runs in parallel with Admin Phase 1
- Supabase setup is shared - coordinate with backend
- Focus on foundation before browse features
