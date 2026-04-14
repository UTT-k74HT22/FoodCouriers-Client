# VNPAY Payment Integration - Client + Admin + Supabase

## 1. Mục tiêu tài liệu

Tài liệu này mô tả đầy đủ thiết kế nghiệp vụ và kỹ thuật để tích hợp thêm thanh toán `VNPAY` vào hệ thống hiện có, trong khi vẫn giữ flow `COD` đang chạy ổn định.

Mục tiêu:

- Giữ `orders` là source of truth duy nhất cho cả client app và admin app.
- Không phá vỡ flow checkout hiện tại đang dùng `Supabase REST + RPC`.
- Không để secret VNPAY nằm trong Android app.
- Đồng bộ trạng thái thanh toán giữa:
  - `FoodCouriers-Client`
  - `FoodCouriers-Admin`
  - `Supabase database`
  - `VNPAY`

Tài liệu này được viết như một blueprint implement. Team có thể dùng trực tiếp để chia task backend, app client, app admin, migration, test và rollout.

## 1.1 Trạng thái repo tại thời điểm rà soát

Đã có setup cơ bản trong repo nhưng chưa đủ để gọi là logic hoàn chỉnh:

- Đã có thư mục deploy thật `supabase/functions/*` cho:
  - `create-vnpay-payment`
  - `vnpay-return`
  - `vnpay-ipn`
- Client app đã thêm `PaymentCallbackActivity` và một số class payment mới
- Tuy nhiên tại thời điểm rà soát:
  - `PaymentRepository` và `PaymentCallbackActivity` vẫn gần như placeholder
  - schema VNPAY mới chưa được apply lên Supabase project
  - migration `008_vnpay_payment_integration.sql` trước đó chỉ nằm ở `docs/`, chưa nằm trong `supabase/migrations/`
  - function `create-vnpay-payment` cần dùng đúng cột `orders.total` và insert đủ `payment_transactions.amount`

Vì vậy tài liệu này nên được hiểu là:

- phase setup nền tảng: gần xong sau khi apply schema + deploy function
- phase business logic/UI flow: vẫn cần triển khai tiếp

---

## 2. Phạm vi

### 2.1 In scope

- Tích hợp thêm phương thức thanh toán `VNPAY`
- Thiết kế end-to-end flow từ checkout đến callback/IPN
- Thiết kế schema/migration liên quan payment
- Thiết kế file structure cũ/mới tham gia
- Thiết kế config/env cho app và server
- Thiết kế contract giữa app và Supabase Edge Functions
- Thiết kế rule đồng bộ với admin app
- Thiết kế test, logging, idempotency, rollback plan

### 2.2 Out of scope

- Refund VNPAY hoàn chỉnh
- Đối soát kế toán cuối ngày
- Hỗ trợ nhiều cổng online payment cùng lúc
- Gom nhiều order vào một payment session ở phase đầu

---

## 3. Hiện trạng hệ thống

## 3.1 Luồng checkout hiện tại

Hệ thống hiện có:

- Client checkout từ cart
- Client gọi `rpc_create_order`
- Mỗi restaurant tạo ra một `order` riêng
- Payment method hiện đang hardcode là `cod`
- Admin app đọc `orders` và `payment_status/payment_method`

Luồng hiện tại trong client:

1. User chọn item trong cart
2. `CheckoutActivity` gọi `CheckoutViewModel`
3. `CheckoutViewModel` group item theo restaurant
4. `OrderRepository.createOrder(...)` gọi `POST /rpc/rpc_create_order`
5. Supabase insert:
   - `orders`
   - `order_items`
   - `order_status_logs`
6. Client chuyển sang `OrderSuccessActivity`

## 3.2 Điểm mạnh hiện tại

- Đã có `RPC` làm write-path chính cho order
- Đã có `payment_method` và `payment_status` trong bảng `orders`
- Đã có bảng `payment_transactions`
- Admin app đã đọc được dữ liệu payment cơ bản

## 3.3 Điểm còn thiếu

- Chưa có lớp backend ký request VNPAY
- Chưa có callback flow cho payment gateway
- Chưa có transaction lifecycle đầy đủ cho online payment
- Chưa có deep link callback riêng cho payment
- Chưa có idempotent update cho IPN
- Checkout hiện đang hardcode `"cod"`

---

## 4. Kiến trúc mục tiêu

## 4.1 Nguyên tắc thiết kế

- `Android app` không bao giờ giữ `vnp_HashSecret`
- Tất cả thao tác tạo URL thanh toán và verify callback phải nằm ở server-side
- `orders.status` và `orders.payment_status` là 2 state machine khác nhau
- `payment_transactions` là audit trail cho payment gateway
- `IPN` là nguồn sự thật cuối cùng cho thanh toán
- `return URL` chủ yếu phục vụ UX, không phải source of truth

## 4.2 Thành phần tham gia

- `FoodCouriers-Client`
  - checkout UI
  - payment selection
  - mở VNPAY URL
  - nhận deep link callback
  - refresh order/payment state
- `Supabase`
  - PostgreSQL
  - RPC cho order/payment update
  - Edge Functions cho VNPAY integration
- `VNPAY`
  - hosted payment page
  - redirect về return URL
  - server-to-server callback qua IPN
- `FoodCouriers-Admin`
  - đọc order/payment status
  - lọc và hiển thị trạng thái thanh toán

---

## 5. Quyết định nghiệp vụ

## 5.1 Payment methods chuẩn hóa

Thay vì để `orders.payment_method` là `('cod', 'online')`, nên chuẩn hóa rõ provider:

- `cod`
- `vnpay`

Không dùng giá trị generic `online` ở phase VNPAY vì:

- khó filter/report
- khó mở rộng sang `momo`, `zalopay`
- admin UI không biết đang dùng cổng nào

## 5.2 Tách bạch order status và payment status

`orders.status` phản ánh vận hành đơn:

- `pending`
- `confirmed`
- `preparing`
- `ready_for_pickup`
- `delivering`
- `delivered`
- `cancelled`

`orders.payment_status` phản ánh thanh toán:

- `pending`
- `paid`
- `failed`
- `refunded`

Không dùng `payment_status` để điều khiển trực tiếp `orders.status`.

## 5.3 Rule phase đầu

Phase 1 chỉ hỗ trợ:

- `COD`: giữ nguyên như hiện tại
- `VNPAY`: chỉ cho checkout chứa item của `1 restaurant`

Lý do:

- Flow hiện tại tạo nhiều `orders` nếu cart chứa nhiều restaurant
- VNPAY phù hợp với một payment request rõ ràng cho một amount duy nhất
- Nếu muốn hỗ trợ multi-restaurant online payment đúng chuẩn, cần thêm `payment_session` gom nhiều order, không nên làm vội ở phase đầu

## 5.4 Rule vận hành đề xuất

- COD:
  - order được tạo
  - `payment_status = 'pending'`
  - thanh toán khi nhận hàng
- VNPAY:
  - order được tạo trước
  - transaction payment được tạo ở trạng thái `pending`
  - khi VNPAY báo thành công:
    - `payment_status = 'paid'`
  - khi user hủy hoặc gateway trả fail:
    - `payment_status = 'failed'`

Admin/staff không nên chuẩn bị món cho order VNPAY chưa `paid` nếu nghiệp vụ muốn tránh thất thoát.

---

## 6. End-to-end flow

## 6.1 COD flow

```text
Client Checkout
  -> rpc_create_order(payment_method='cod')
  -> orders.payment_status = 'pending'
  -> OrderSuccessActivity
  -> Admin xử lý order như flow hiện tại
  -> Thu tiền khi giao hàng
```

## 6.2 VNPAY flow thành công

```text
Client Checkout
  -> create order with payment_method='vnpay'
  -> call Edge Function create-vnpay-payment(order_id)
  -> server creates payment_transactions(status='pending')
  -> server signs VNPAY URL
  -> app opens browser/custom tab
  -> user pays on VNPAY
  -> VNPAY redirect to server return endpoint
  -> server verifies checksum
  -> server redirects to app deep link
  -> VNPAY also sends IPN to server
  -> server verifies IPN
  -> server updates payment_transactions='success'
  -> server updates orders.payment_status='paid'
  -> app refreshes order detail
  -> admin app sees paid status
```

## 6.3 VNPAY flow thất bại hoặc hủy

```text
Client Checkout
  -> create order with payment_method='vnpay'
  -> create payment transaction pending
  -> user cancels / timeout / gateway fail
  -> return URL brings user back to app
  -> IPN or return verification marks payment failed
  -> orders.payment_status='failed'
  -> app shows retry payment option
```

## 6.4 Sequence chi tiết

```text
User
  -> CheckoutActivity
CheckoutActivity
  -> CheckoutViewModel.placeVnpayOrder()
CheckoutViewModel
  -> OrderRepository.createOrder(..., paymentMethod='vnpay')
OrderRepository
  -> Supabase RPC rpc_create_order
Supabase DB
  -> insert orders/order_items/order_status_logs
Supabase RPC
  -> returns order_id
CheckoutViewModel
  -> PaymentRepository.createVnpayPayment(order_id)
PaymentRepository
  -> Edge Function create-vnpay-payment
Edge Function
  -> validates order owner + status
  -> inserts payment_transactions pending
  -> signs VNPAY params
  -> returns payment_url
PaymentRepository
  -> CheckoutActivity
CheckoutActivity
  -> open browser/custom tab
VNPAY
  -> user completes payment
VNPAY
  -> return endpoint on server
  -> IPN endpoint on server
Edge Function
  -> verify checksum
  -> update payment_transactions
  -> update orders.payment_status
Edge Function
  -> redirect deep link app
PaymentCallbackActivity
  -> refresh order
Orders/OrderDetail screens
  -> display paid/failed state
Admin app
  -> reads updated payment fields from orders
```

---

## 7. Cấu trúc thư mục và file tham gia

## 7.1 File hiện có trong Client app

Các file đang tham gia trực tiếp hoặc gián tiếp vào checkout/order/payment:

- [AndroidManifest.xml](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/AndroidManifest.xml)
- [CheckoutActivity.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/checkout/CheckoutActivity.java)
- [CheckoutViewModel.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/viewmodel/CheckoutViewModel.java)
- [OrderRepository.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/repository/OrderRepository.java)
- [OrderSummary.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/model/OrderSummary.java)
- [OrderSuccessActivity.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/order/OrderSuccessActivity.java)
- [OrderDetailActivity.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/order/OrderDetailActivity.java)
- [OrdersFragment.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/order/OrdersFragment.java)
- [activity_checkout.xml](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/res-layouts/order/layout/activity_checkout.xml)
- [strings.xml](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/res/values/strings.xml)
- [SocialAuthCallbackActivity.java](C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/auth/SocialAuthCallbackActivity.java)

## 7.2 File hiện có trong Admin app

- [AndroidManifest.xml](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/AndroidManifest.xml)
- [Order.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/data/model/Order.java)
- [OrderClient.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/data/remote/OrderClient.java)
- [DeliveryRepository.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/data/repository/DeliveryRepository.java)
- [OrderAdapter.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/order/adapter/OrderAdapter.java)
- [OrderDetailActivity.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/order/OrderDetailActivity.java)
- [strings.xml](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/res/values/strings.xml)

## 7.3 File database hiện có

- [001_initial_schema.sql](C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/001_initial_schema.sql)
- [002_rpc_functions.sql](C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/002_rpc_functions.sql)
- [003_rls_policies.sql](C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/003_rls_policies.sql)
- [003_rls_policies_final.sql](C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/003_rls_policies_final.sql)
- [006_fix_rpc_create_order.sql](C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/006_fix_rpc_create_order.sql)

## 7.4 Cấu trúc file mới đề xuất

### Client app

```text
FoodCouriers-Client/
└── app/
    └── src/main/
        ├── AndroidManifest.xml                       (sửa)
        ├── java/com/utt/foodcouriers_client/
        │   ├── data/model/
        │   │   ├── PaymentInitResult.java           (mới)
        │   │   └── PaymentCallbackResult.java       (mới)
        │   ├── data/repository/
        │   │   ├── OrderRepository.java             (sửa)
        │   │   └── PaymentRepository.java           (mới)
        │   ├── ui/checkout/
        │   │   └── CheckoutActivity.java            (sửa)
        │   ├── ui/payment/
        │   │   └── PaymentCallbackActivity.java     (mới)
        │   ├── util/payment/
        │   │   ├── PaymentMethod.java               (mới)
        │   │   └── PaymentDeepLinkParser.java       (mới)
        │   └── viewmodel/
        │       └── CheckoutViewModel.java           (sửa)
        └── res/
            ├── layouts/order/layout/
            │   └── activity_checkout.xml            (sửa)
            └── values/
                └── strings.xml                      (sửa)
```

### Supabase backend

```text
FoodCouriers-Client/
└── supabase/
    ├── functions/
    │   ├── create-vnpay-payment/
    │   │   └── index.ts                            (mới)
    │   ├── vnpay-return/
    │   │   └── index.ts                            (mới)
    │   ├── vnpay-ipn/
    │   │   └── index.ts                            (mới)
    │   └── shared/
    │       ├── vnpay.ts                            (mới)
    │       ├── payment-status.ts                   (mới)
    │       └── http.ts                             (mới)
    └── migrations/
        ├── 008_vnpay_payment_integration.sql       (mới)
        └── 009_vnpay_rls_and_rpc.sql               (mới)
```

### Admin app

```text
FoodCouriers-Admin/
└── app/
    └── src/main/
        ├── java/com/utt/foodcouriers_admin/
        │   ├── data/model/Order.java               (sửa nhẹ)
        │   ├── data/remote/OrderClient.java        (sửa nhẹ)
        │   ├── ui/order/adapter/OrderAdapter.java  (sửa nhẹ)
        │   └── ui/order/OrderDetailActivity.java   (sửa nhẹ)
        └── res/values/strings.xml                  (sửa nhẹ)
```

## 7.5 Trách nhiệm từng file mới

- `PaymentRepository.java`
  - gọi Edge Function tạo VNPAY payment
  - parse response
  - hỗ trợ retry/query nếu cần
- `PaymentCallbackActivity.java`
  - nhận deep link từ payment return
  - parse status/order_id/txn_ref
  - trigger reload order detail
- `PaymentMethod.java`
  - định nghĩa constant `COD`, `VNPAY`
- `PaymentDeepLinkParser.java`
  - parse URI callback ổn định, tránh rải logic vào activity
- `create-vnpay-payment/index.ts`
  - validate order
  - tạo transaction pending
  - sign URL
- `vnpay-return/index.ts`
  - verify checksum từ query params
  - redirect về deep link app
- `vnpay-ipn/index.ts`
  - verify callback server-side
  - idempotent update DB

---

## 8. Thiết kế database

## 8.1 Schema hiện tại

Hiện bảng `orders` và `payment_transactions` đã tồn tại:

- `orders.payment_method`
- `orders.payment_status`
- `payment_transactions.provider`
- `payment_transactions.external_txn_id`
- `payment_transactions.status`
- `payment_transactions.raw_response`

Nhưng schema hiện tại chưa đủ để vận hành VNPAY production-grade.

## 8.2 Thay đổi schema đề xuất

### Bảng `orders`

Đề xuất:

- đổi `payment_method` check constraint từ:
  - `('cod', 'online')`
- thành:
  - `('cod', 'vnpay')`

Không cần tạo thêm cột mới trong `orders` ở phase đầu nếu chưa cần report sâu hơn.

### Bảng `payment_transactions`

Đề xuất thêm các cột:

- `provider_order_ref TEXT`
- `gateway_transaction_no TEXT`
- `gateway_response_code TEXT`
- `bank_code TEXT`
- `pay_url TEXT`
- `expires_at TIMESTAMPTZ`
- `paid_at TIMESTAMPTZ`
- `idempotency_key TEXT`
- `failure_reason TEXT`
- `return_payload JSONB`
- `ipn_payload JSONB`

### Index và uniqueness

Đề xuất:

- unique `(provider, provider_order_ref)`
- unique `(provider, gateway_transaction_no)` với điều kiện not null
- index `(order_id, created_at desc)`
- index `(status, created_at desc)`

## 8.3 Migration mẫu

```sql
ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS orders_payment_method_check;

ALTER TABLE public.orders
    ADD CONSTRAINT orders_payment_method_check
    CHECK (payment_method IN ('cod', 'vnpay'));

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

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_provider_order_ref
ON public.payment_transactions(provider, provider_order_ref)
WHERE provider_order_ref IS NOT NULL;
```

## 8.4 RPC/DB write-path đề xuất

Đề xuất không cho app update trực tiếp bảng `payment_transactions` hay `orders.payment_status`.

Write-path chuẩn:

- app gọi Edge Function
- Edge Function dùng service role hoặc logic server-side để update
- nếu cần dùng DB function, tạo thêm:
  - `rpc_mark_order_payment_success`
  - `rpc_mark_order_payment_failed`

## 8.5 RPC đề xuất

### `rpc_mark_order_payment_success`

Nhiệm vụ:

- verify order đang `payment_status='pending'`
- update `orders.payment_status='paid'`
- update transaction current thành `success`
- có thể ghi notification cho user

### `rpc_mark_order_payment_failed`

Nhiệm vụ:

- verify transaction pending/current
- update `orders.payment_status='failed'`
- update transaction thành `failed`

Nếu muốn gọn hơn, có thể gói trong Edge Function và không public RPC này cho client.

---

## 9. RLS và security

## 9.1 Nguyên tắc

- Client chỉ được đọc transaction của order thuộc chính mình
- Client không được tự tạo transaction bằng REST insert
- Client không được update `orders.payment_status`
- Chỉ server-side payment function mới được ghi các trường payment

## 9.2 Chính sách đề xuất

Cho `payment_transactions`:

- `SELECT`: owner của order hoặc admin
- `INSERT/UPDATE`: chỉ service role hoặc function nội bộ

Cho `orders`:

- customer `SELECT` own order
- customer không tự `PATCH payment_status`
- admin/staff đọc để vận hành

## 9.3 Secret management

Các biến sau chỉ được đặt ở server-side:

- `VNPAY_TMN_CODE`
- `VNPAY_HASH_SECRET`
- `VNPAY_RETURN_BASE_URL`
- `VNPAY_IPN_ALLOWED_IPS` nếu cần

Không commit các giá trị thật vào repo.

---

## 10. Thiết kế contract giữa app và backend

## 10.1 Tạo order

Client vẫn dùng luồng cũ:

`POST /rest/v1/rpc/rpc_create_order`

Payload:

```json
{
  "p_user_id": "uuid",
  "p_restaurant_id": "uuid",
  "p_delivery_address": "string",
  "p_delivery_latitude": 21.002,
  "p_delivery_longitude": 105.843,
  "p_note": "string",
  "p_payment_method": "cod | vnpay",
  "p_promotion_code": "PROMO10",
  "p_items": []
}
```

## 10.2 Create VNPAY payment

Endpoint đề xuất:

`POST /functions/v1/create-vnpay-payment`

Request:

```json
{
  "order_id": "uuid"
}
```

Response:

```json
{
  "order_id": "uuid",
  "transaction_id": "uuid",
  "provider": "vnpay",
  "provider_order_ref": "VNP-ORD20260414-00001",
  "payment_url": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "expires_at": "2026-04-14T15:20:00Z"
}
```

## 10.3 VNPAY return endpoint

Endpoint đề xuất:

`GET /functions/v1/vnpay-return`

Nhiệm vụ:

- nhận query params từ VNPAY
- verify checksum
- chuẩn hóa payload
- redirect sang app deep link

Deep link ví dụ:

```text
com.utt.foodcouriers.client://payment/vnpay/callback
    ?order_id=...
    &transaction_id=...
    &result=success
    &response_code=00
```

## 10.4 VNPAY IPN endpoint

Endpoint đề xuất:

`GET /functions/v1/vnpay-ipn`

Nhiệm vụ:

- verify checksum
- update DB idempotent
- trả HTTP body đúng format VNPAY mong đợi

## 10.5 Query order sau callback

Client không nên tin tuyệt đối vào callback result.

Sau khi nhận deep link:

1. parse payload
2. mở `OrderDetailActivity` hoặc màn hình kết quả
3. gọi lại `getOrderById(orderId)`
4. render theo `orders.payment_status` thực tế từ DB

---

## 11. Config và environment

## 11.1 Android app config

Client app chỉ cần biết:

- base URL Supabase/Edge Function
- deep link scheme/host/path
- có thể cần cờ feature toggle bật/tắt VNPAY

Đề xuất:

```text
APP_DEEP_LINK_SCHEME=com.utt.foodcouriers.client
APP_PAYMENT_CALLBACK_HOST=payment
APP_PAYMENT_CALLBACK_PATH=/vnpay/callback
FEATURE_VNPAY_ENABLED=true
```

## 11.2 Supabase Edge Function env

```text
SUPABASE_URL=...
SUPABASE_SERVICE_ROLE_KEY=...
VNPAY_TMN_CODE=...
VNPAY_HASH_SECRET=...
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://<project>.supabase.co/functions/v1/vnpay-return
VNPAY_IPN_URL=https://<project>.supabase.co/functions/v1/vnpay-ipn
APP_PAYMENT_DEEP_LINK_BASE=com.utt.foodcouriers.client://payment/vnpay/callback
TZ=Asia/Ho_Chi_Minh
```

## 11.3 AndroidManifest deep link

Đề xuất thêm activity riêng thay vì reuse social auth callback:

```xml
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

## 11.4 VNPAY environment

Nên tách rõ:

- `sandbox`
- `production`

Tuyệt đối không hardcode production URL/secret vào app.

---

## 12. Thiết kế UI/UX

## 12.1 Checkout screen

Thêm section chọn phương thức thanh toán:

- `COD`
- `VNPAY`

Rule:

- nếu cart có nhiều restaurant:
  - disable `VNPAY`
  - hiển thị note: `VNPAY hiện chỉ hỗ trợ đơn từ một nhà hàng`

## 12.2 Sau khi tạo order VNPAY

Không nhảy thẳng vào `OrderSuccessActivity`.

Flow đề xuất:

1. create order
2. create payment URL
3. mở browser/custom tab
4. callback về app
5. app xác nhận trạng thái thanh toán
6. chuyển tới:
   - `OrderSuccessActivity` nếu paid
   - màn hình `payment failed / retry` nếu failed

## 12.3 Order detail / order list

Hiển thị rõ:

- payment method
- payment status

Text gợi ý:

- `COD - Thanh toán khi nhận hàng`
- `VNPAY - Đã thanh toán`
- `VNPAY - Thanh toán thất bại`
- `VNPAY - Chờ thanh toán`

## 12.4 Retry payment

Đề xuất cho phase sau hoặc phase đầu nếu kịp:

- nếu `payment_status='failed'` và `status='pending'`
- cho user bấm `Thanh toán lại`
- gọi lại `create-vnpay-payment`

---

## 13. Đồng bộ với Admin app

## 13.1 Mục tiêu

Admin app không phải nơi xử lý payment gateway, nhưng phải nhìn thấy trạng thái đúng để vận hành order đúng.

## 13.2 Yêu cầu hiển thị

Trong order list và order detail của admin:

- hiển thị `payment_method`
- hiển thị `payment_status`
- có badge dễ nhìn:
  - `COD / Chờ thu`
  - `VNPAY / Đã thanh toán`
  - `VNPAY / Thất bại`

## 13.3 Rule vận hành

Đề xuất:

- order `VNPAY` chưa `paid`:
  - admin/staff không xác nhận chế biến nếu nghiệp vụ muốn chặn
- order `COD`:
  - flow cũ giữ nguyên

Nếu muốn enforce chặt, logic này nên nằm ở RPC update status chứ không chỉ ở UI admin.

## 13.4 File admin cần sửa

- `Order.java`
  - đảm bảo parse đủ field payment
- `OrderAdapter.java`
  - hiển thị badge thanh toán
- `OrderDetailActivity.java`
  - hiển thị thông tin transaction cơ bản nếu cần
- `strings.xml`
  - thêm text trạng thái

---

## 14. Thiết kế Edge Function

## 14.1 `create-vnpay-payment`

### Input

- authenticated user token
- `order_id`

### Validate

- order tồn tại
- order thuộc user hiện tại
- `payment_method = 'vnpay'`
- `payment_status != 'paid'`
- order chưa bị `cancelled`
- chưa có transaction success

### Logic

1. load order
2. generate `provider_order_ref`
3. insert `payment_transactions(status='pending')`
4. build VNPAY params
5. sort params + sign checksum
6. trả `payment_url`

## 14.2 `vnpay-return`

### Input

- query params từ VNPAY

### Logic

1. parse query
2. verify checksum
3. map outcome:
   - success
   - failed
   - invalid
4. redirect về deep link app

Không nên coi return endpoint là nơi quyết định cuối cùng trạng thái DB nếu IPN chưa xác nhận. Có thể update nhẹ transaction return payload, nhưng source of truth cuối vẫn là IPN.

## 14.3 `vnpay-ipn`

### Input

- query params từ VNPAY server

### Logic

1. verify checksum
2. tìm transaction theo `provider_order_ref`
3. nếu đã success trước đó:
   - return idempotent success response
4. nếu `response_code = 00`:
   - update transaction `success`
   - update `orders.payment_status='paid'`
5. nếu fail:
   - update transaction `failed`
   - update `orders.payment_status='failed'`
6. trả response code đúng format gateway cần

## 14.4 Shared utility

Nên gom logic chung ở `supabase/functions/shared/vnpay.ts`:

- build canonical sign string
- HMAC SHA512
- parse response codes
- map sandbox/production config

---

## 15. State machine đề xuất

## 15.1 Order state machine

```text
pending -> confirmed -> preparing -> ready_for_pickup -> delivering -> delivered
pending -> cancelled
confirmed -> cancelled
preparing -> cancelled
delivering -> cancelled
```

## 15.2 Payment state machine

```text
pending -> paid
pending -> failed
paid -> refunded
failed -> pending   (chỉ khi business hỗ trợ retry bằng transaction mới; order vẫn giữ open)
```

## 15.3 Rule giữa 2 state machine

- `orders.status='cancelled'` không tự động nghĩa là `payment_status='refunded'`
- `payment_status='paid'` không tự động nghĩa là `status='confirmed'`
- nếu muốn auto-confirm sau khi thanh toán online thành công, phải được business approve rõ ràng

Khuyến nghị phase đầu:

- chỉ cập nhật `payment_status`
- vận hành order vẫn theo admin/staff xác nhận

---

## 16. Logging, audit, monitoring

## 16.1 Cần log gì

- create order with payment method
- create VNPAY transaction
- return callback received
- IPN received
- payment success/failure update
- duplicate IPN ignored
- redirect deep link result

## 16.2 Lưu DB ở đâu

- `payment_transactions.raw_response`
- `payment_transactions.return_payload`
- `payment_transactions.ipn_payload`

## 16.3 Monitoring cần có

- số payment pending quá lâu
- số payment failed
- số callback checksum invalid
- số IPN duplicate

---

## 17. Failure scenarios

## 17.1 User đóng app sau khi mở VNPAY

Xử lý:

- transaction vẫn pending
- khi user mở lại order detail, app load từ DB
- nếu IPN đã tới thì thấy `paid/failed`
- nếu chưa có kết quả, vẫn hiện `pending`

## 17.2 Return callback thành công nhưng IPN chưa tới

Xử lý:

- app hiển thị `Đang xác nhận thanh toán`
- poll/reload order detail ngắn hạn
- kết quả cuối cùng dựa trên DB/IPN

## 17.3 IPN gửi lặp nhiều lần

Xử lý:

- update idempotent
- không tạo duplicate transaction success
- không double notify

## 17.4 User bấm thanh toán lại nhiều lần

Xử lý:

- chỉ cho phép 1 transaction active/pending gần nhất
- hoặc tạo transaction mới nhưng khóa bằng `idempotency_key`

## 17.5 Order bị admin hủy khi payment còn pending

Xử lý đề xuất:

- `create-vnpay-payment` không cho tạo payment nếu order đã `cancelled`
- IPN tới muộn cần có rule business:
  - nếu order đã cancelled mà payment success đến sau, chuyển sang quy trình hoàn tiền/manual review

---

## 18. Test plan

## 18.1 Unit test

- build canonical sign string đúng
- checksum verify đúng
- parser deep link đúng
- map response code đúng

## 18.2 Integration test

- create order COD không regression
- create order VNPAY + payment URL thành công
- return success
- return fail
- IPN success
- IPN fail
- IPN duplicate
- retry payment

## 18.3 Manual UAT

- single restaurant checkout bằng VNPAY
- multi restaurant checkout: VNPAY bị disable
- user cancel trên trang VNPAY
- app bị kill trước callback
- admin thấy payment badge đúng
- order detail hiển thị đúng payment status

## 18.4 Regression checklist

- COD vẫn đặt đơn bình thường
- promotion vẫn áp dụng đúng
- order history không mất dữ liệu
- realtime order status vẫn hoạt động

---

## 19. Kế hoạch implement theo phase

## 19.1 Phase 1

- migration DB cho `vnpay`
- Edge Functions cơ bản
- checkout chọn payment method
- callback deep link
- order detail hiển thị payment status
- admin hiển thị payment status
- chỉ hỗ trợ single restaurant

## 19.2 Phase 2

- retry payment
- analytics/report payment
- audit log tốt hơn
- enforce vận hành admin theo payment status

## 19.3 Phase 3

- hỗ trợ multi-order payment session
- refund flow
- nhiều cổng online payment

---

## 20. Checklist file change

## 20.1 Client app

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/.../ui/checkout/CheckoutActivity.java`
- `app/src/main/java/.../viewmodel/CheckoutViewModel.java`
- `app/src/main/java/.../data/repository/OrderRepository.java`
- `app/src/main/java/.../data/repository/PaymentRepository.java`
- `app/src/main/java/.../ui/payment/PaymentCallbackActivity.java`
- `app/src/main/java/.../util/payment/PaymentMethod.java`
- `app/src/main/java/.../util/payment/PaymentDeepLinkParser.java`
- `app/src/main/res-layouts/order/layout/activity_checkout.xml`
- `app/src/main/res/values/strings.xml`

## 20.2 Supabase

- `supabase/migrations/008_vnpay_payment_integration.sql`
- `supabase/migrations/009_vnpay_rls_and_rpc.sql`
- `supabase/functions/create-vnpay-payment/index.ts`
- `supabase/functions/vnpay-return/index.ts`
- `supabase/functions/vnpay-ipn/index.ts`
- `supabase/functions/shared/vnpay.ts`

## 20.3 Admin app

- `app/src/main/java/.../data/model/Order.java`
- `app/src/main/java/.../ui/order/adapter/OrderAdapter.java`
- `app/src/main/java/.../ui/order/OrderDetailActivity.java`
- `app/src/main/res/values/strings.xml`

---

## 21. Kết luận

Hướng tích hợp phù hợp nhất với hệ thống hiện tại là:

- giữ flow tạo order hiện tại bằng `rpc_create_order`
- thêm `payment orchestration layer` bằng `Supabase Edge Functions`
- dùng `payment_transactions` làm audit log của gateway
- dùng `orders.payment_status` làm trạng thái thanh toán business-level cho cả client và admin
- dùng deep link callback riêng cho payment
- phase đầu chỉ hỗ trợ `VNPAY` cho checkout một nhà hàng

Đây là phương án ít phá kiến trúc hiện có nhất, an toàn hơn về bảo mật, và đồng bộ tốt với cách app client/admin đang đọc dữ liệu từ Supabase.
