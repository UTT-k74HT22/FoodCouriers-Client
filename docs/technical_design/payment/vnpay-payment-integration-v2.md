---
title: "VNPAY Payment Integration - Business & Technical Specification"
description: "Tài liệu nghiệp vụ và kỹ thuật chi tiết về tích hợp thanh toán VNPAY cho FoodCouriers"
audience: [ai-agents, developers, product-managers, qa]
tags: [payment, vnpay, integration, backend, android]
created: 2026-04-15
updated: 2026-04-15
status: published
---

# VNPAY Payment Integration - Business & Technical Specification

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
3. [Flow nghiệp vụ End-to-End](#3-flow-nghiệp-vụ-end-to-end)
4. [Sequence Diagram chi tiết](#4-sequence-diagram-chi-tiết)
5. [Thiết kế Database](#5-thiết-kế-database)
6. [API Contracts](#6-api-contracts)
7. [Edge Functions Specification](#7-edge-functions-specification)
8. [Client Implementation](#8-client-implementation)
9. [State Machines](#9-state-machines)
10. [Error Handling](#10-error-handling)
11. [Security Considerations](#11-security-considerations)
12. [File Structure](#12-file-structure)
13. [Cấu hình môi trường](#13-cấu-hình-môi-trường)
14. [Testing Checklist](#14-testing-checklist)
15. [Deployment Guide](#15-deployment-guide)

---

## 1. Tổng quan

### 1.1 Mục tiêu

Tích hợp cổng thanh toán VNPAY vào hệ thống FoodCouriers để hỗ trợ người dùng thanh toán đơn hàng online qua:
- Thẻ ATM nội địa (Internet Banking)
- Thẻ quốc tế (Visa, MasterCard)
- Ví điện tử VNPAY

### 1.2 Phạm vi

**In Scope:**
- Thanh toán VNPAY cho đơn hàng từ 1 nhà hàng
- Tích hợp đầy đủ: checkout → payment → callback → confirmation
- Hỗ trợ COD (Cash on Delivery) song song
- Đồng bộ trạng thái với Admin app

**Out of Scope (Phase 1):**
- Refund VNPAY
- Thanh toán multi-restaurant trong 1 order
- Nhiều cổng thanh toán cùng lúc (Momo, ZaloPay)
- Đối soát kế toán tự động

### 1.3 Nguyên tắc thiết kế

```
┌─────────────────────────────────────────────────────────────────┐
│                    THIẾT KẾ NGUYÊN TẮC                         │
├─────────────────────────────────────────────────────────────────┤
│ 1. Server-side signing: Hash secret KHÔNG BAO GIỜ trong app   │
│ 2. IPN là source of truth: Server-to-server callback           │
│ 3. Idempotency: Không tạo duplicate transactions               │
│ 4. Dual verification: Return URL (UX) + IPN (Truth)           │
│ 5. State isolation: Order status ≠ Payment status             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Kiến trúc hệ thống

### 2.1 Component Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           FOODCOURIERS ECOSYSTEM                         │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌─────────────┐          ┌─────────────────┐         ┌────────────┐ │
│   │             │          │                 │         │            │ │
│   │   Android   │──────────│    Supabase     │─────────│   VNPAY    │ │
│   │   Client    │  REST    │   Edge Funcs   │  HTTPS  │   Gateway  │ │
│   │             │    +    │                 │         │            │ │
│   └─────────────┘  Edge   └─────────────────┘         └────────────┘ │
│        │                  │        │                              │      │
│        │                  │        │                              │      │
│        ▼                  ▼        ▼                              ▼      │
│   ┌─────────┐       ┌─────────┐ ┌─────────┐                ┌─────────┐│
│   │ Checkout │       │  Edge   │ │PostgreSQL│               │ Payment ││
│   │   UI    │       │Functions│ │ Database │               │  Page   ││
│   └─────────┘       └─────────┘ └─────────┘                └─────────┘│
│                                                                          │
│   ┌─────────────┐                          ┌─────────────────────────┐  │
│   │ Admin App   │◄────────────────────────│ Payment Transactions    │  │
│   │             │     REST / Realtime      │ Orders Table            │  │
│   └─────────────┘                          └─────────────────────────┘  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PAYMENT DATA FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. USER ACTION                                                     │
│     Android Client ──► Create Order (RPC) ──► Supabase DB           │
│                                   │                                  │
│  2. PAYMENT INIT                                                  │
│     Client ──► Edge Function ──► Sign URL ──► VNPAY Gateway         │
│                (create-vnpay-payment)     │                         │
│                              │            │                         │
│                              ▼            ▼                         │
│  3. USER PAYS              ┌─────────────────┐                      │
│     Browser/Tab ─────────────────────────────► VNPAY Payment Page    │
│                              │                                         │
│  4. CALLBACKS              │                                         │
│     ┌───────────────────────┴───────────────────────┐               │
│     │                                               │               │
│     ▼                                               ▼               │
│  ┌──────────────┐                          ┌──────────────┐         │
│  │ Return URL   │                          │ IPN URL      │         │
│  │ (vnpay-return)│                         │(vnpay-ipn)  │         │
│  │ User-facing   │                          │ Server-to-   │         │
│  │ Redirect to   │                          │ Server      │         │
│  │ App Deep Link │                          │ Source of   │         │
│  └──────────────┘                          │ Truth       │         │
│     │                                     └──────────────┘         │
│     │                                           │                    │
│     ▼                                           ▼                    │
│  ┌──────────────┐                      ┌──────────────┐            │
│  │ App Deep Link│◄─────────────────────│ Update DB    │            │
│  │ (Callback    │                       │ orders.status│            │
│  │  Activity)   │                       │ payment_tx   │            │
│  └──────────────┘                       └──────────────┘            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 Security Layer

```
┌─────────────────────────────────────────────────────────────────────┐
│                      SECURITY ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    ANDROID CLIENT                             │   │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │   │
│   │  │ Session     │  │ Auth Token  │  │ No Secret   │        │   │
│   │  │ Manager     │  │ (JWT)       │  │ Key stored │        │   │
│   │  └─────────────┘  └─────────────┘  └─────────────┘        │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              │                                        │
│                              ▼                                        │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    SUPABASE EDGE FUNCTIONS                    │   │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │   │
│   │  │ JWT Verify  │  │ HMAC SHA512  │  │ RLS Policies│        │   │
│   │  │ (User Auth) │  │ (VNPAY Sign) │  │ (Data Sec)  │        │   │
│   │  └─────────────┘  └─────────────┘  └─────────────┘        │   │
│   │                                                                 │   │
│   │  ┌─────────────────────────────────────────────────────────┐ │   │
│   │  │ ENV VARIABLES (Server-only)                             │ │   │
│   │  │ • VNPAY_TMN_CODE                                        │ │   │
│   │  │ • VNPAY_HASH_SECRET  ◄── NEVER exposed to client        │ │   │
│   │  │ • VNPAY_IPN_URL                                         │ │   │
│   │  │ • SUPABASE_SERVICE_ROLE_KEY                             │ │   │
│   │  └─────────────────────────────────────────────────────────┘ │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Flow nghiệp vụ End-to-End

### 3.1 COD Flow (Baseline - Không đổi)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         COD PAYMENT FLOW                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User        Client              Supabase              Admin         │
│   │             │                    │                    │          │
│   │  1. Checkout & chọn COD        │                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  2. POST /rpc/rpc_create_order  │                    │          │
│   │             │──── RPC ─────────►│                    │          │
│   │             │                    │                    │          │
│   │             │    order_id + status: pending           │          │
│   │             │◄─── Created ──────│                    │          │
│   │             │                    │                    │          │
│   │  3. Order Success Screen        │                    │          │
│   │◄────────────│                    │                    │          │
│   │             │                    │                    │          │
│   │             │                    │  4. Realtime ────► │          │
│   │             │                    │     New Order      │          │
│   │             │                    │                    │          │
│   │             │                    │                    │◄─ Confirm│
│   │             │                    │                    │          │
│   │             │                    │◄─ Update status ────│          │
│   │             │                    │                    │          │
│   │  5. Track order                 │                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  6. Receive & Pay cash           │                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│                                                                      │
│  ORDER STATUS: pending → confirmed → preparing → delivering → delivered │
│  PAYMENT STATUS: pending (thu tien khi giao hang)                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 VNPAY Flow thành công

```
┌─────────────────────────────────────────────────────────────────────┐
│                    VNPAY PAYMENT - SUCCESS FLOW                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User        Client              Supabase              VNPAY        │
│   │             │                    │                    │          │
│   │  1. Checkout & chọn VNPAY        │                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  2. POST /rpc/rpc_create_order  │                    │          │
│   │             │──── RPC ─────────►│                    │          │
│   │             │                    │                    │          │
│   │             │    order_id        │                    │          │
│   │             │    payment_method: vnpay               │          │
│   │             │    payment_status: pending               │          │
│   │             │◄─── Created ──────│                    │          │
│   │             │                    │                    │          │
│   │  3. POST /functions/            │                    │          │
│   │     create-vnpay-payment       │                    │          │
│   │             │──── Edge ───────►│                    │          │
│   │             │                    │                    │          │
│   │             │  • Verify user    │                    │          │
│   │             │  • Sign URL       │                    │          │
│   │             │  • Insert tx      │                    │          │
│   │             │                    │                    │          │
│   │  4. payment_url                 │                    │          │
│   │◄────────────│◄─── Response ────│                    │          │
│   │             │                    │                    │          │
│   │  5. Open browser with payment_url                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  6. User completes payment on VNPAY page             │          │
│   │────────────►│───────────────────►│───────────────────►│          │
│   │             │                    │                    │          │
│   │  7a. Return URL (vnpay-return)  │                    │          │
│   │             │◄───────────────────────────────────────│          │
│   │             │   Verify checksum   │                    │          │
│   │             │                    │                    │          │
│   │  7b. IPN URL (vnpay-ipn) ────────────────────────────►│          │
│   │             │◄────────────────────────────────────────│          │
│   │             │   Server verifies & updates DB          │          │
│   │             │                    │                    │          │
│   │             │                    │ • tx.status = success            │
│   │             │                    │ • order.payment_status = paid    │
│   │             │                    │                    │          │
│   │  8. Redirect to app deep link    │                    │          │
│   │◄────────────────────────────────│                    │          │
│   │             │                    │                    │          │
│   │  9. PaymentCallbackActivity     │                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  10. Navigate to Order Detail   │                    │          │
│   │◄────────────│                    │                    │          │
│   │             │                    │                    │          │
│   │  11. Order Ready for Processing │                    │          │
│   │             │                    │──── Realtime ────► │          │
│   │             │                    │                    │          │
│                                                                      │
│  ORDER STATUS: pending → confirmed → preparing → ...                 │
│  PAYMENT STATUS: pending → paid ✓                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.3 VNPAY Flow thất bại / Hủy

```
┌─────────────────────────────────────────────────────────────────────┐
│                   VNPAY PAYMENT - FAILURE FLOW                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User        Client              Supabase              VNPAY        │
│   │             │                    │                    │          │
│   │  1-5. Same as success flow until payment_url         │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  6. User cancels / payment fails / timeout            │          │
│   │────────────►│───────────────────►│───────────────────►│          │
│   │             │                    │                    │          │
│   │  7. IPN with response_code ≠ 00 │                    │          │
│   │             │◄────────────────────────────────────────│          │
│   │             │                    │                    │          │
│   │             │                    │ • tx.status = failed           │
│   │             │                    │ • order.payment_status = failed│
│   │             │                    │ • failure_reason logged        │
│   │             │                    │                    │          │
│   │  8. Redirect to app deep link    │                    │          │
│   │◄────────────────────────────────│                    │          │
│   │             │                    │                    │          │
│   │  9. PaymentCallbackActivity     │                    │          │
│   │────────────►│                    │                    │          │
│   │             │                    │                    │          │
│   │  10. Show failure message + retry button              │          │
│   │◄────────────│                    │                    │          │
│   │             │                    │                    │          │
│   │  11. (Optional) User retries payment                  │          │
│   │────────────►│                    │                    │          │
│   │             │──── Step 3-6 ─────►│───────────────────►│          │
│   │             │                    │                    │          │
│                                                                      │
│  ORDER STATUS: pending (user can retry or cancel)                  │
│  PAYMENT STATUS: pending → failed                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.4 Retry Payment Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      RETRY PAYMENT FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User        Client              Supabase                            │
│   │             │                    │                               │
│   │  1. User on Order Detail (payment_status: failed)               │
│   │────────────►│                    │                               │
│   │             │                    │                               │
│   │  2. Tap "Thanh toán lại"         │                               │
│   │────────────►│                    │                               │
│   │             │                    │                               │
│   │  3. POST /functions/create-vnpay-payment                        │
│   │             │──── Edge ─────────►│                               │
│   │             │                    │                               │
│   │             │  Check existing tx │                               │
│   │             │  ┌─────────────┐   │                               │
│   │             │  │ tx pending?│───┼── No  ──► Create new tx      │
│   │             │  │ tx expired?│   │        │                      │
│   │             │  └─────────────┘   │        ▼                      │
│   │             │                    │   ┌─────────────┐             │
│   │             │◄── Same/reused ────┼──│ Return     │             │
│   │             │    payment_url     │   │ existing tx│             │
│   │             │                    │   └─────────────┘             │
│   │             │                    │                               │
│   │  4. New payment_url (or same if not expired)                    │
│   │◄────────────│                    │                               │
│   │             │                    │                               │
│   │  5. Open browser → User pays again                              │
│   │────────────►│───────────────────►│                               │
│   │             │                    │                               │
│   │  6. Same callback flow as initial payment                       │
│   │◄────────────│                    │                               │
│   │             │                    │                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Sequence Diagram chi tiết

### 4.1 Complete Payment Sequence

```text
┌────────┐     ┌────────┐     ┌────────────┐     ┌──────────────┐     ┌─────────┐
│  User  │     │ Client │     │  Supabase  │     │ VNPAY Server │     │  Admin  │
└───┬────┘     └───┬────┘     └──────┬─────┘     └──────┬───────┘     └───┬─────┘
    │             │             │           │             │             │
    │ Checkout    │             │           │             │             │
    │────────────►│             │           │             │             │
    │             │             │           │             │             │
    │ Create Order│             │           │             │             │
    │────────────►│──rpc───────►│           │             │             │
    │             │             │           │             │             │
    │             │◄─order_id──│           │             │             │
    │             │             │           │             │             │
    │             │ Init Payment│           │             │             │
    │             │──edge──────►│           │             │             │
    │             │             │           │             │             │
    │             │ 1. Verify JWT           │             │             │
    │             │ 2. Check order          │             │             │
    │             │ 3. Sign URL             │             │             │
    │             │ 4. Insert tx            │             │             │
    │             │             │           │             │             │
    │             │◄─payment_url─│           │             │             │
    │ payment_url │             │           │             │             │
    │◄────────────│             │           │             │             │
    │             │             │           │             │             │
    │ Open Browser│             │           │             │             │
    │────────────►│─────────────────────────►│           │             │
    │             │             │           │             │             │
    │ Pay on VNPAY│             │           │             │             │
    │────────────►│────────────►│──────────►│             │             │
    │             │             │           │             │             │
    │◄────────────│◄────────────────────────│             │             │
    │             │    Return URL (redirect) │             │             │
    │             │             │           │             │             │
    │             │             │◄───────────────────────IPN│
    │             │             │ Verify checksum          │
    │             │             │ Update DB                │
    │             │             │           │             │             │
    │ Deep Link   │             │           │             │             │
    │◄────────────│             │           │             │             │
    │             │             │           │             │             │
    │ View Result │             │           │             │             │
    │────────────►│             │           │             │             │
    │             │◄──Order Detail────────────────────────│             │
    │             │             │           │             │             │
    │             │             │────────Realtime────────►│             │
    │             │             │           │             │             │
    │             │             │           │             │◄─Process───│
    │             │             │           │             │             │
    └─────────────┴─────────────┴───────────┴─────────────┴─────────────┘
```

### 4.2 Error Scenarios Sequence

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                         PAYMENT EXPIRY SEQUENCE                            │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User        Client              Supabase              VNPAY               │
│   │             │                    │                    │               │
│   │ payment_url │                    │                    │               │
│   │◄────────────│                    │                    │               │
│   │             │                    │                    │               │
│   │ User waits 15+ minutes...        │                    │               │
│   │             │                    │                    │               │
│   │ Payment page shows "Expired"     │                    │               │
│   │◄────────────│                    │                    │               │
│   │             │                    │                    │               │
│   │ User returns to app (or not)     │                    │               │
│   │────────────►│                    │                    │               │
│   │             │                    │                    │               │
│   │             │ Retry payment      │                    │               │
│   │             │───────────────────►│                    │               │
│   │             │                    │                    │               │
│   │             │  Check tx: EXPIRED │                    │               │
│   │             │  Mark tx as failed │                    │               │
│   │             │  Create new tx      │                    │               │
│   │             │                    │                    │               │
│   │             │◄─new payment_url──│                    │               │
│   │ New URL     │                    │                    │               │
│   │◄────────────│                    │                    │               │
│   │             │                    │                    │               │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│                      DUPLICATE REQUEST SEQUENCE                            │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User        Client              Supabase                                  │
│   │             │                    │                                       │
│   │ User taps "Pay" twice rapidly    │                                       │
│   │────────────►│                    │                                       │
│   │             │                    │                                       │
│   │ Request #1  │                    │                                       │
│   │────────────►│──Idempotency-Key──►│                                       │
│   │             │                    │                                       │
│   │ Request #2  │                    │                                       │
│   │────────────►│──Idempotency-Key──►│                                       │
│   │             │                    │                                       │
│   │             │  Check idempotency key                                     │
│   │             │  Found existing tx  │                                       │
│   │             │  Return same tx     │                                       │
│   │             │                    │                                       │
│   │ Same payment_url (both requests) │                                       │
│   │◄────────────│                    │                                       │
│   │◄────────────│                    │                                       │
│   │             │                    │                                       │
│   │ No duplicate transaction created! │                                       │
│   │             │                    │                                       │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Thiết kế Database

### 5.1 Schema Changes

#### Bảng `orders` - Thay đổi constraint

```sql
-- File: supabase/migrations/008_vnpay_payment_integration.sql

-- 1. Update payment_method constraint
ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS orders_payment_method_check;

ALTER TABLE public.orders
    ADD CONSTRAINT orders_payment_method_check
    CHECK (payment_method IN ('cod', 'vnpay'));
```

#### Bảng `payment_transactions` - Thêm columns

```sql
-- File: supabase/migrations/008_vnpay_payment_integration.sql

ALTER TABLE public.payment_transactions
    ADD COLUMN IF NOT EXISTS provider_order_ref TEXT,
    ADD COLUMN IF NOT EXISTS gateway_transaction_no TEXT,
    ADD COLUMN IF NOT EXISTS gateway_response_code TEXT,
    ADD COLUMN IF NOT EXISTS bank_code TEXT,
    ADD COLUMN IF NOT EXISTS pay_url TEXT,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS idempotency_key TEXT,
    ADD COLUMN IF NOT EXISTS failure_reason TEXT,
    ADD COLUMN IF NOT EXISTS return_payload JSONB,
    ADD COLUMN IF NOT EXISTS ipn_payload JSONB;
```

### 5.2 Indexes

```sql
-- File: supabase/migrations/008_vnpay_payment_integration.sql

-- Unique index cho provider_order_ref
CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_provider_order_ref
ON public.payment_transactions(provider, provider_order_ref)
WHERE provider_order_ref IS NOT NULL;

-- Unique index cho gateway transaction
CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_provider_gateway_txn
ON public.payment_transactions(provider, gateway_transaction_no)
WHERE gateway_transaction_no IS NOT NULL;

-- Index cho query theo order
CREATE INDEX IF NOT EXISTS idx_payment_tx_order_created_at
ON public.payment_transactions(order_id, created_at DESC);

-- Index cho query theo status
CREATE INDEX IF NOT EXISTS idx_payment_tx_status_created_at
ON public.payment_transactions(status, created_at DESC);
```

### 5.3 Entity Relationship

```
┌─────────────────────────────────────────────────────────────────────┐
│                      DATABASE RELATIONSHIPS                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│    ┌─────────────┐          ┌─────────────────────────────┐       │
│    │   orders    │          │  payment_transactions       │       │
│    ├─────────────┤          ├─────────────────────────────┤       │
│    │ id (PK)     │──┐  1:N  │ id (PK)                     │       │
│    │ user_id (FK)│   └──────│ order_id (FK)               │       │
│    │ total       │          │ provider                     │       │
│    │ payment_method│       │ provider_order_ref           │       │
│    │ payment_status│       │ gateway_transaction_no       │       │
│    │ status      │          │ status                      │       │
│    │ ...         │          │ amount                      │       │
│    └─────────────┘          │ pay_url                     │       │
│                             │ expires_at                  │       │
│                             │ idempotency_key            │       │
│                             │ failure_reason             │       │
│                             │ paid_at                    │       │
│                             │ return_payload (JSONB)     │       │
│                             │ ipn_payload (JSONB)        │       │
│                             │ created_at                 │       │
│                             │ updated_at                 │       │
│                             └─────────────────────────────┘       │
│                                                                      │
│    payment_transactions.status:                                      │
│    ┌────────────────────────────────────────────────────────────┐   │
│    │  pending ──────► success                                    │   │
│    │      │                                                        │   │
│    │      └──► failed                                             │   │
│    └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│    orders.payment_status:                                             │
│    ┌────────────────────────────────────────────────────────────┐   │
│    │  pending ──────► paid                                        │   │
│    │      │                                                        │   │
│    │      ├──► failed                                             │   │
│    │      │                                                        │   │
│    │      └──► refunded                                           │   │
│    └────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.4 Sample Data

```sql
-- Sample payment_transactions record
INSERT INTO payment_transactions (
    id,
    order_id,
    provider,
    provider_order_ref,
    status,
    amount,
    pay_url,
    expires_at,
    idempotency_key,
    created_at
) VALUES (
    gen_random_uuid(),
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'vnpay',
    'VNP-a1b2c3d4e5f6789012345-1744700000000',
    'pending',
    85000,
    'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...',
    NOW() + INTERVAL '15 minutes',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890-1744699900000',
    NOW()
);

-- Sample updated orders record
UPDATE orders SET
    payment_method = 'vnpay',
    payment_status = 'pending'
WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
```

---

## 6. API Contracts

### 6.1 Create Order (RPC)

```yaml
# Endpoint: POST /rest/v1/rpc/rpc_create_order
# Auth: Bearer JWT (user)
# Content-Type: application/json

Request:
{
  "p_user_id": "uuid",
  "p_restaurant_id": "uuid",
  "p_delivery_address": "string",
  "p_delivery_latitude": 21.002,
  "p_delivery_longitude": 105.843,
  "p_note": "string",
  "p_payment_method": "cod" | "vnpay",
  "p_promotion_code": "PROMO10",  # optional
  "p_items": [
    {
      "menu_item_id": "uuid",
      "quantity": 2,
      "note": "Không hành"
    }
  ]
}

Response (200):
{
  "order_id": "uuid",
  "status": "pending",
  "payment_method": "vnpay",
  "payment_status": "pending",
  "total": 85000,
  "created_at": "2026-04-15T10:00:00Z"
}

Errors:
- 400: Invalid payment_method
- 400: Invalid items
- 400: Empty cart
- 401: Unauthorized
- 404: Restaurant not found
```

### 6.2 Create VNPAY Payment

```yaml
# Endpoint: POST /functions/v1/create-vnpay-payment
# Auth: Bearer JWT (user)
# Content-Type: application/json
# Idempotency-Key: {order_id}-{timestamp}

Request:
{
  "order_id": "uuid",
  "idempotency_key": "uuid-timestamp"  # optional, server will generate if not provided
}

Response (200):
{
  "order_id": "uuid",
  "transaction_id": "uuid",
  "provider": "vnpay",
  "provider_order_ref": "VNP-xxx-timestamp",
  "payment_url": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "expires_at": "2026-04-15T10:15:00Z",
  "reused": false,
  "idempotent": false
}

Response (200 - Reused existing):
{
  "order_id": "uuid",
  "transaction_id": "uuid",
  "provider": "vnpay",
  "provider_order_ref": "VNP-xxx-timestamp",
  "payment_url": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "expires_at": "2026-04-15T10:15:00Z",
  "reused": true,
  "idempotent": true
}

Errors:
- 400: "Order payment_method must be vnpay"
- 400: "Order already paid"
- 400: "Order is cancelled"
- 400: "Invalid order amount"
- 401: "Unauthorized"
- 404: "Order not found"
- 500: Internal server error
```

### 6.3 VNPAY Return URL

```yaml
# Endpoint: GET /functions/v1/vnpay-return?vnp_*
# Auth: None (public)
# VNPAY will redirect here after user completes/cancels payment

VNPAY Query Parameters Received:
- vnp_TxnRef: "VNP-xxx-timestamp"
- vnp_ResponseCode: "00" | other codes
- vnp_TransactionNo: "123456"
- vnp_Amount: "8500000"
- vnp_SecureHash: "..."
- ... (other VNPAY params)

Action: Redirect to app deep link

Redirect URL Format:
com.utt.foodcouriers.client://payment/vnpay/callback
  ?txn_ref=VNP-xxx-timestamp
  &result=success|failed
  &response_code=00
  &transaction_no=123456
  &amount=8500000
  &checksum_valid=true|false
```

### 6.4 VNPAY IPN URL

```yaml
# Endpoint: GET /functions/v1/vnpay-ipn?vnp_*
# Auth: None (server-to-server from VNPAY)
# IMPORTANT: This is the source of truth for payment status

VNPAY Query Parameters Received:
- vnp_TxnRef: "VNP-xxx-timestamp"
- vnp_ResponseCode: "00" (success) | "01" (pending) | "24" (cancel) | etc.
- vnp_TransactionNo: "123456"
- vnp_BankCode: "NCB"
- vnp_Amount: "8500000"
- vnp_SecureHash: "..."
- ... (other VNPAY params)

Response (VNPAY expected format):
{
  "RspCode": "00",   # 00 = success, other = error codes
  "Message": "Confirm Success"
}
```

### 6.5 Get Transaction Status

```yaml
# Endpoint: GET /rest/v1/payment_transactions?order_id=eq.{id}&select=...&order=created_at.desc&limit=1
# Auth: Bearer JWT (user)

Response (200):
[
  {
    "id": "uuid",
    "status": "pending" | "success" | "failed",
    "provider_order_ref": "VNP-xxx",
    "pay_url": "https://...",
    "expires_at": "2026-04-15T10:15:00Z",
    "failure_reason": null | "...",
    "paid_at": null | "2026-04-15T10:05:00Z"
  }
]

Response (404): Transaction not found
```

---

## 7. Edge Functions Specification

### 7.1 create-vnpay-payment

```typescript
// File: supabase/functions/create-vnpay-payment/index.ts
// Runtime: Deno Deploy

Function: Create VNPAY Payment URL

Input:
- Authorization: Bearer {jwt}
- Body: { order_id: string, idempotency_key?: string }

Process:
1. Validate JWT and get user_id
2. Validate order_id exists and belongs to user
3. Check order.payment_method === 'vnpay'
4. Check order.payment_status !== 'paid'
5. Check order.status !== 'cancelled'
6. Check for existing pending transaction:
   a. If exists and not expired → return existing
   b. If exists and expired → mark failed, continue
7. Check idempotency_key if provided:
   a. If exists → return existing
8. Generate provider_order_ref: VNP-{order_id_part}-{timestamp}
9. Build VNPAY params with HMAC-SHA512 signature
10. Insert payment_transactions record
11. Return payment_url

Output:
{
  order_id: string,
  transaction_id: string,
  provider: "vnpay",
  provider_order_ref: string,
  payment_url: string,
  expires_at: ISO8601,
  reused?: boolean,
  idempotent?: boolean
}

Security:
- Uses anon key + JWT for user verification
- Uses service role key for all DB operations (RLS bypass)
- Hash secret never exposed outside function
```

### 7.2 vnpay-return

```typescript
// File: supabase/functions/vnpay-return/index.ts
// Runtime: Deno Deploy

Function: Handle VNPAY Return (User-facing redirect)

Input:
- Query params from VNPAY redirect

Process:
1. Parse all query parameters
2. Verify secure hash checksum
3. Determine result (success/failed) based on response_code
4. Update payment_transactions.return_payload with full query
5. Construct redirect URL to app deep link

Output:
- HTTP 302 Redirect to deep link

Deep Link Format:
com.utt.foodcouriers.client://payment/vnpay/callback
  ?txn_ref={vnp_TxnRef}
  &result={success|failed}
  &response_code={vnp_ResponseCode}
  &transaction_no={vnp_TransactionNo}
  &amount={vnp_Amount}
  &checksum_valid={true|false}

Note:
- This endpoint is for UX only
- Source of truth is vnpay-ipn
- Only updates return_payload, not transaction status
```

### 7.3 vnpay-ipn

```typescript
// File: supabase/functions/vnpay-ipn/index.ts
// Runtime: Deno Deploy

Function: Handle VNPAY IPN (Server-to-server)

Input:
- Query params from VNPAY server

Process:
1. Verify secure hash checksum
2. Find transaction by provider_order_ref (vnp_TxnRef)
3. If not found → return RspCode "01"
4. If already success (idempotent) → return RspCode "00"
5. Check order status:
   a. If order.cancelled → mark tx failed, return "00"
   b. Continue processing
6. Based on vnp_ResponseCode:
   a. "00" (Success):
      - Update tx: status=success, paid_at, gateway info
      - Update order: payment_status='paid'
   b. Other codes (Failure):
      - Update tx: status=failed, failure_reason
      - Update order: payment_status='failed'
7. Return VNPAY expected format

Output:
{
  "RspCode": "00" | "01" | "99",
  "Message": "Confirm Success" | "TxnRef not found" | "System error"
}

Important:
- This is the SOURCE OF TRUTH
- Must be idempotent
- VNPAY will retry if not 200 OK
```

### 7.4 shared/vnpay.ts

```typescript
// File: supabase/functions/shared/vnpay.ts
// Runtime: Deno Deploy (shared module)

Exports:
- VNPAY_CONFIG: Configuration object
- formatDateVN(date: Date): string  // yyyyMMddHHmmss in Vietnam TZ
- buildExpireDate(date: Date, minutes: number): string
- sortObject(input: Record<string, string>): Record<string, string>
- buildQueryString(params: Record<string, string>): string
- signVnpay(params: Record<string, string>): Promise<string>  // HMAC-SHA512
- verifyVnpay(query: URLSearchParams): Promise<boolean>
```

---

## 8. Client Implementation

### 8.1 Android Client Flow

```java
// File: app/src/main/java/com/utt/foodcouriers_client/viewmodel/CheckoutViewModel.java

Flow:
1. placeOrders()
   ├── validateCart()
   ├── validateSingleRestaurant() // VNPAY only
   └── placeOrderSequentially()
       ├── createOrder() for each restaurant group
       └── if VNPAY:
           └── fetchVnpayPaymentUrl()
               └── PaymentRepository.createVnpayPayment()
                   └── PaymentInitResult → openVnpayBrowser()

// File: app/src/main/java/com/utt/foodcouriers_client/ui/checkout/CheckoutActivity.java

Flow:
1. User selects VNPAY in RadioGroup
2. Button text changes: "Đặt hàng & Thanh toán VNPAY"
3. User taps button
4. Confirmation dialog
5. submitOrder() → ViewModel.placeOrders()
6. If VNPAY:
   └── openVnpayBrowser() → Intent browserIntent
7. App finishes, user in browser

// File: app/src/main/java/com/utt/foodcouriers_client/ui/payment/PaymentCallbackActivity.java

Flow (via deep link):
1. onCreate() receives intent with URI
2. PaymentDeepLinkParser.parse() extracts params
3. Check callbackResult.isSuccess()
4. Show success/failure banner
5. Navigate to OrderDetailActivity or MainActivity
```

### 8.2 Key Classes

| Class | Package | Responsibility |
|-------|---------|----------------|
| `PaymentRepository` | `data/repository/` | API calls to Edge Functions |
| `PaymentInitResult` | `data/model/` | Model for payment init response |
| `PaymentCallbackResult` | `data/model/` | Model for callback data |
| `PaymentMethodEnum` | `utils/payment/` | Enum: COD, VNPAY |
| `PaymentDeepLinkParser` | `utils/payment/` | Parse callback URI |
| `PaymentConfig` | `utils/payment/` | Constants (deep link scheme) |
| `PaymentCallbackActivity` | `ui/payment/` | Handle return from VNPAY |
| `CheckoutViewModel` | `viewmodel/` | Orchestrate checkout flow |
| `CheckoutActivity` | `ui/checkout/` | Checkout UI |

### 8.3 Deep Link Configuration

```xml
<!-- File: app/src/main/AndroidManifest.xml -->

<activity
    android:name=".ui.payment.PaymentCallbackActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="com.utt.foodcouriers.client"
            android:host="payment"
            android:pathPrefix="/vnpay/callback" />
    </intent-filter>
</activity>
```

### 8.4 Session Management

```java
// File: app/src/main/java/com/utt/foodcouriers_client/utils/SessionManager.java

// Store pending order for payment callback
public void setPendingPaymentOrderId(String orderId) {
    editor.putString(KEY_PENDING_PAYMENT_ORDER_ID, orderId);
    editor.apply();
}

public String getPendingPaymentOrderId() {
    return prefs.getString(KEY_PENDING_PAYMENT_ORDER_ID, null);
}

public void clearPendingPaymentOrderId() {
    editor.remove(KEY_PENDING_PAYMENT_ORDER_ID);
    editor.apply();
}
```

---

## 9. State Machines

### 9.1 Order Status Machine

```text
┌─────────────────────────────────────────────────────────────────────┐
│                      ORDER STATUS MACHINE                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│    ┌─────────┐                                                     │
│    │pending  │◄──────────────────┐                                 │
│    └────┬────┘                   │                                 │
│         │                        │                                 │
│         ▼                        │                                 │
│    ┌─────────┐                   │                                 │
│    │confirmed│                   │                                 │
│    └────┬────┘                   │                                 │
│         │                        │                                 │
│         ▼                        │                                 │
│    ┌──────────┐                  │                                 │
│    │preparing │                  │                                 │
│    └─────┬────┘                  │                                 │
│          │                       │                                 │
│          ▼                       │                                 │
│   ┌─────────────┐                │                                 │
│   │ready_for_pickup│             │                                 │
│   └──────┬───────┘               │                                 │
│          │                        │                                 │
│          ▼                        │                                 │
│    ┌───────────┐     ┌─────────┐  │                                 │
│    │delivering │────►│cancelled│  │                                 │
│    └─────┬─────┘     └─────────┘  │                                 │
│          │                        │                                 │
│          ▼                        │                                 │
│   ┌───────────┐                   │                                 │
│   │ delivered │                   │                                 │
│   └───────────┘                   │                                 │
│                                  │                                 │
│   Legitimate transitions:        │                                 │
│   pending → confirmed            │                                 │
│   confirmed → preparing          │                                 │
│   preparing → ready_for_pickup    │                                 │
│   ready_for_pickup → delivering  │                                 │
│   delivering → delivered          │                                 │
│   Any state (except delivered) → cancelled                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 Payment Status Machine

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    PAYMENT STATUS MACHINE                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│    ┌─────────┐     ┌─────────┐     ┌──────────┐                    │
│    │ pending │────►│   paid  │────►│ refunded │                    │
│    └────┬────┘     └─────────┘     └──────────┘                    │
│         │                                                        │
│         │                                                        │
│         ▼                                                        │
│    ┌─────────┐                                                   │
│    │  failed │                                                   │
│    └────┬────┘                                                   │
│         │                                                        │
│         │ (Retry)                                                │
│         ▼                                                        │
│    ┌─────────┐                                                   │
│    │ pending │ (new transaction created)                         │
│    └─────────┘                                                   │
│                                                                      │
│   Note: failed → pending is allowed via retry payment flow        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.3 Transaction Status Machine

```text
┌─────────────────────────────────────────────────────────────────────┐
│                   TRANSACTION STATUS MACHINE                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│    ┌─────────┐     ┌─────────┐                                     │
│    │ pending │────►│ success │                                     │
│    └────┬────┘     └─────────┘                                     │
│         │                                                        │
│         │                                                        │
│         ├──► failed (payment declined/cancelled/expired)          │
│         │                                                        │
│         │                                                        │
│         └──► failed (duplicate detected - idempotent)             │
│                                                                      │
│   Terminal states: success, failed                                  │
│   Non-terminal: pending                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.4 State Relationship

```text
┌─────────────────────────────────────────────────────────────────────┐
│              STATE RELATIONSHIP (Order vs Payment)                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Order Status           Payment Status        Can Proceed?         │
│   ─────────────────────────────────────────────────────────         │
│   pending               pending               Yes (COD)             │
│   pending               paid                 Yes (VNPAY)           │
│   pending               failed               Retry available       │
│   confirmed              pending               Yes                   │
│   confirmed              paid                 Yes                   │
│   confirmed              failed               Retry available       │
│   confirmed              refunded             Admin refund          │
│   cancelled              *                    No (order locked)    │
│   preparing              pending              No (payment required) │
│   preparing              paid                 Yes                   │
│   preparing              failed               Block!                │
│   ...                    ...                  ...                   │
│                                                                      │
│   Rule: VNPAY orders should NOT be processed until payment_status='paid' │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 10. Error Handling

### 10.1 Error Codes

| Code | Source | Description | Action |
|------|--------|-------------|--------|
| `AUTH_REQUIRED` | Client | No active session | Redirect to login |
| `ORDER_NOT_FOUND` | Backend | Order doesn't exist or wrong user | Show error |
| `WRONG_PAYMENT_METHOD` | Backend | Order not VNPAY | Show error |
| `ALREADY_PAID` | Backend | Order already paid | Navigate to order |
| `ORDER_CANCELLED` | Backend | Order was cancelled | Show error |
| `INVALID_AMOUNT` | Backend | Order total <= 0 | Show error |
| `NETWORK_ERROR` | Client | Connection failed | Retry option |
| `TIMEOUT` | Client | Request timeout | Retry option |
| `PAYMENT_EXPIRED` | Backend | 15 min window passed | Show expired message |
| `CHECKSUM_INVALID` | Backend | VNPAY signature mismatch | Log security event |

### 10.2 VNPAY Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| `00` | Transaction successful | Update to paid |
| `01` | Transaction pending | Keep pending |
| `02` | Transaction failed | Update to failed |
| `03` | Pending (card not confirmed) | Keep pending |
| `04` | Transaction reversed | Mark refunded |
| `05` | Transaction partial refund | Update amount |
| `06` | Card expired | Show expired |
| `07` | Invalid card | Show error |
| `09` | Card not registered | Show error |
| `10` | Card incorrect CVV | Show error |
| `11` | Card wrong expiry | Show error |
| `12` | Card insufficient funds | Show error |
| `13` | Transaction incorrect | Show error |
| `24` | User cancelled | Update to failed |
| `99` | System error | Log and retry |

### 10.3 Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                       ERROR HANDLING FLOW                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   User Action                                                        │
│        │                                                             │
│        ▼                                                             │
│   ┌─────────────┐                                                   │
│   │ API Call    │                                                   │
│   └──────┬──────┘                                                   │
│          │                                                          │
│          ▼                                                          │
│   ┌─────────────┐      ┌─────────────┐                              │
│   │ Success?    │──No──►│ Handle Error │                              │
│   └──────┬──────┘      └──────┬──────┘                              │
│          │Yes                  │                                     │
│          ▼                     ▼                                     │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐         │
│   │ Process     │      │ Parse Error │      │ Show User   │         │
│   │ Response    │      │ Code        │─────►│ Message     │         │
│   └─────────────┘      └──────┬──────┘      └─────────────┘         │
│                              │                                     │
│                              ▼                                     │
│                      ┌─────────────┐                               │
│                      │ Retry?      │                               │
│                      └──────┬──────┘                               │
│                             │                                       │
│              ┌──────────────┴──────────────┐                        │
│              │                             │                        │
│              ▼                             ▼                        │
│       ┌───────────┐                ┌───────────┐                   │
│       │ Yes (Safe)│                │ No (Fatal)│                   │
│       │ Retry     │                │ Navigate  │                   │
│       │ Request   │                │ Away      │                   │
│       └───────────┘                └───────────┘                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 10.4 Edge Cases

```java
// Case 1: User closes app during VNPAY payment
// → Transaction remains pending
// → When user returns, they can retry or check status

// Case 2: IPN arrives before return URL
// → PaymentCallbackActivity still works
// → Result based on actual DB state from IPN

// Case 3: IPN arrives multiple times (VNPAY retry)
// → Idempotent handling: check status before update
// → Return 00 even if already processed

// Case 4: User cancels order while payment pending
// → IPN handler checks order status first
// → If cancelled, mark tx failed but don't update payment_status

// Case 5: Payment successful but order cancelled later
// → Need manual refund process
// → Log for reconciliation

// Case 6: Clock skew - expires_at vs actual time
// → Use server time for all calculations
// → VNPAY uses Vietnam time (UTC+7)
```

---

## 11. Security Considerations

### 11.1 Secrets Management

```
┌─────────────────────────────────────────────────────────────────────┐
│                      SECRETS MANAGEMENT                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   NEVER in Client App:                                               │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ • VNPAY_TMN_CODE          ❌                                 │   │
│   │ • VNPAY_HASH_SECRET       ❌ (Core secret - server only!)    │   │
│   │ • SUPABASE_SERVICE_ROLE_KEY ❌                              │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   ONLY in Edge Functions (Supabase Secrets):                         │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ • VNPAY_TMN_CODE          ✓                                 │   │
│   │ • VNPAY_HASH_SECRET       ✓                                 │   │
│   │ • SUPABASE_SERVICE_ROLE_KEY ✓                               │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   In Client App (Safe):                                              │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ • SUPABASE_URL           ✓ (public)                        │   │
│   │ • SUPABASE_ANON_KEY      ✓ (RLS controlled)                │   │
│   │ • Deep link scheme       ✓ (public)                        │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 11.2 HMAC-SHA512 Signing

```typescript
// File: supabase/functions/shared/vnpay.ts

// VNPAY requires HMAC-SHA512 signing of parameters
async function signVnpay(params: Record<string, string>): Promise<string> {
  const sorted = sortObject(params);
  const signData = buildQueryString(sorted);
  
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(VNPAY_CONFIG.hashSecret),
    { name: "HMAC", hash: "SHA-512" },
    false,
    ["sign"]
  );
  
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(signData));
  return toHex(signature);
}
```

### 11.3 RLS Policies

```sql
-- File: supabase/migrations/010_fix_vnpay_payment_transactions_rls.sql

-- Users can only see their own payment transactions
CREATE POLICY "Users can view own payment transactions"
ON payment_transactions
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM orders
    WHERE orders.id = payment_transactions.order_id
    AND orders.user_id = auth.uid()
  )
);

-- Only service role can insert/update payment_transactions
-- (No INSERT/UPDATE policies for authenticated users)
```

### 11.4 Idempotency

```typescript
// Every payment creation includes idempotency check
async function createPayment(orderId: string, idempotencyKey?: string) {
  // 1. Check idempotency key if provided
  if (idempotencyKey) {
    const existing = await supabase
      .from('payment_transactions')
      .select('*')
      .eq('order_id', orderId)
      .eq('idempotency_key', idempotencyKey)
      .single();
    
    if (existing) return existing; // Return same response
  }
  
  // 2. Check existing pending transaction
  const pending = await supabase
    .from('payment_transactions')
    .select('*')
    .eq('order_id', orderId)
    .eq('status', 'pending')
    .single();
  
  if (pending && !isExpired(pending.expires_at)) {
    return pending; // Return existing URL
  }
  
  // 3. Create new transaction
  // ...
}
```

---

## 12. File Structure

### 12.1 Complete File Tree

```
FoodCouriers-Client/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml                           # Deep link config
│       ├── java/com/utt/foodcouriers_client/
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── PaymentInitResult.java           # Payment init model
│       │   │   │   └── PaymentCallbackResult.java       # Callback model
│       │   │   └── repository/
│       │   │       └── PaymentRepository.java            # API calls
│       │   ├── ui/
│       │   │   ├── checkout/
│       │   │   │   └── CheckoutActivity.java            # Payment selection UI
│       │   │   └── payment/
│       │   │       └── PaymentCallbackActivity.java     # Handle return
│       │   ├── utils/
│       │   │   └── payment/
│       │   │       ├── PaymentConfig.java               # Constants
│       │   │       ├── PaymentDeepLinkParser.java       # URI parser
│       │   │       └── PaymentMethodEnum.java           # Enum
│       │   └── viewmodel/
│       │       └── CheckoutViewModel.java               # Checkout logic
│       └── res/
│           └── values/
│               └── strings.xml                          # All strings
│
└── supabase/
    ├── functions/
    │   ├── create-vnpay-payment/
    │   │   └── index.ts                                 # Create payment URL
    │   ├── vnpay-return/
    │   │   └── index.ts                                 # Handle return
    │   ├── vnpay-ipn/
    │   │   └── index.ts                                 # Server callback
    │   └── shared/
    │       └── vnpay.ts                                 # Shared utilities
    │
    └── migrations/
        ├── 008_vnpay_payment_integration.sql            # Schema changes
        └── 010_fix_vnpay_payment_transactions_rls.sql   # RLS policies
```

### 12.2 File Responsibilities

#### Backend Files

| File | Responsibility |
|------|----------------|
| `create-vnpay-payment/index.ts` | Validate order, sign URL, create transaction |
| `vnpay-return/index.ts` | Verify checksum, redirect to app |
| `vnpay-ipn/index.ts` | Update DB based on payment result |
| `shared/vnpay.ts` | HMAC signing, VNPAY utilities |

#### Client Files

| File | Responsibility |
|------|----------------|
| `PaymentRepository.java` | HTTP calls to Edge Functions |
| `PaymentInitResult.java` | Model for payment response |
| `PaymentCallbackResult.java` | Model for callback data |
| `PaymentMethodEnum.java` | COD/VNPAY enum |
| `PaymentDeepLinkParser.java` | Parse callback URI |
| `PaymentCallbackActivity.java` | Handle deep link |
| `CheckoutViewModel.java` | Orchestrate checkout |
| `CheckoutActivity.java` | UI + user interaction |

### 12.3 Dependencies

```gradle
// app/build.gradle
dependencies {
    // OkHttp for HTTP calls
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // Gson for JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
}
```

---

## 13. Cấu hình môi trường

### 13.1 Supabase Edge Function Environment

```bash
# supabase/functions/.env (local development)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
SUPABASE_ANON_KEY=your-anon-key
VNPAY_TMN_CODE=K1GG6ZU3
VNPAY_HASH_SECRET=your-hash-secret
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://your-project.supabase.co/functions/v1/vnpay-return
VNPAY_IPN_URL=https://your-project.supabase.co/functions/v1/vnpay-ipn
APP_PAYMENT_DEEP_LINK_BASE=com.utt.foodcouriers.client://payment/vnpay/callback
```

### 13.2 Supabase Dashboard Secrets

```
# Go to: Supabase Dashboard → Project Settings → Edge Functions → Secrets

VNPAY_TMN_CODE=K1GG6ZU3
VNPAY_HASH_SECRET=your-production-hash-secret
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://your-project.supabase.co/functions/v1/vnpay-return
VNPAY_IPN_URL=https://your-project.supabase.co/functions/v1/vnpay-ipn
APP_PAYMENT_DEEP_LINK_BASE=com.utt.foodcouriers.client://payment/vnpay/callback
```

### 13.3 VNPAY Sandbox vs Production

```typescript
// Sandbox (Testing)
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

// Production
VNPAY_PAYMENT_URL=https://pay.vnpayment.vn/paymentv2/vpcpay.html
```

### 13.4 VNPAY Merchant Portal

Register and configure at: https://merchant.vnpayment.vn

Required configurations:
- Merchant ID (TmnCode)
- Hash Secret
- Return URL
- IPN URL
- Enable test mode for sandbox

---

## 14. Testing Checklist

### 14.1 Unit Tests

- [ ] HMAC-SHA512 signing correct
- [ ] Checksum verification correct
- [ ] Deep link parser handles all cases
- [ ] Date formatting correct (VN timezone)
- [ ] Amount conversion (VND to *100)

### 14.2 Integration Tests

```bash
# Test Create Payment
curl -X POST "https://your-project.supabase.co/functions/v1/create-vnpay-payment" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"order_id": "your-order-id"}'

# Expected: 200 with payment_url

# Test VNPAY Return (simulate)
curl "https://your-project.supabase.co/functions/v1/vnpay-return?vnp_TxnRef=test&vnp_ResponseCode=00&vnp_SecureHash=test"

# Expected: 302 redirect to deep link
```

### 14.3 E2E Tests

| Test Case | Steps | Expected Result |
|-----------|-------|----------------|
| Happy Path - Success | 1. Checkout VNPAY | Payment success |
|                  | 2. Pay on VNPAY | Redirect to app |
|                  | 3. Complete payment | Order marked paid |
| Happy Path - Cancel | 1. Checkout VNPAY | Payment prompt |
|                    | 2. Cancel on VNPAY | Redirect to app |
|                    | 3. Return to app | Order still pending |
| Retry Payment | 1. Failed payment | Show retry option |
|              | 2. Tap retry | New payment URL |
|              | 3. Complete | Success |
| Multi-restaurant | 1. Add items from 2 restaurants | VNPAY disabled |
|                  | 2. Try to pay | Error message |
| Expired Payment | 1. Start payment | Payment URL |
|                 | 2. Wait 15+ minutes | URL expired |
|                 | 3. Try to pay | New URL created |
| Duplicate Request | 1. Tap pay twice quickly | Single transaction |

### 14.4 Manual Testing Matrix

| Scenario | Tester | Date | Result |
|----------|--------|------|--------|
| COD still works | QA | - | - |
| VNPAY sandbox success | QA | - | - |
| VNPAY sandbox cancel | QA | - | - |
| Retry payment | QA | - | - |
| Payment with promo | QA | - | - |
| Multi-restaurant VNPAY blocked | QA | - | - |
| App killed during payment | QA | - | - |
| Browser back during payment | QA | - | - |
| Admin sees payment status | QA | - | - |

---

## 15. Deployment Guide

### 15.1 Pre-deployment Checklist

- [ ] Migration 008 applied to production database
- [ ] Migration 010 (RLS) applied
- [ ] Edge Functions deployed
- [ ] VNPAY secrets configured in Supabase
- [ ] Deep link scheme configured in Android manifest
- [ ] Test with sandbox first

### 15.2 Deploy Edge Functions

```bash
# Install Supabase CLI if not installed
npm install -g supabase

# Login
supabase login

# Link to project
supabase link --project-ref your-project-ref

# Deploy all functions
supabase functions deploy

# Deploy specific function
supabase functions deploy create-vnpay-payment
```

### 15.3 Apply Migrations

```bash
# Push migrations to linked project
supabase db push

# Or apply manually via SQL Editor in Supabase Dashboard
```

### 15.4 Configure VNPAY Production

1. Go to https://merchant.vnpayment.vn
2. Request production credentials
3. Update secrets:
   ```bash
   supabase secrets set VNPAY_TMN_CODE=your-production-code
   supabase secrets set VNPAY_HASH_SECRET=your-production-secret
   supabase secrets set VNPAY_PAYMENT_URL=https://pay.vnpayment.vn/paymentv2/vpcpay.html
   ```
4. Update Android deep link if scheme changes

### 15.5 Post-deployment Verification

```bash
# 1. Check functions are deployed
supabase functions list

# 2. Test with a small amount (1000 VND)
# 3. Verify IPN reaches production DB
# 4. Check payment_transactions table
# 5. Verify order.payment_status updated
```

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| IPN | Instant Payment Notification - server-to-server callback |
| TmnCode | VNPAY merchant terminal code |
| HashSecret | Secret key for HMAC signing |
| Idempotency | Property ensuring same result for same request |
| RLS | Row Level Security - Supabase access control |
| Deep Link | URL scheme to open app from browser |
| Payment Gateway | Service processing online payments (VNPAY) |
| Merchant | Business accepting payments |

## Appendix B: VNPAY Documentation Links

- Sandbox: https://sandbox.vnpayment.vn/apis
- Production: https://merchant.vnpayment.vn
- Integration Guide: Check VNPAY merchant portal

## Appendix C: Related Documents

- [Order Management Flow](./order-management.md)
- [Cart to Checkout Flow](./cart-order-checkout-flow-v1.md)
- [Order Client Admin Sync](./order-client-admin-sync.md)
- [Social Auth Google Flow](./social-auth-google-flow.md)

---

**Document Version:** 2.0
**Last Updated:** 2026-04-15
**Author:** AI Code Review
**Status:** Production Ready
