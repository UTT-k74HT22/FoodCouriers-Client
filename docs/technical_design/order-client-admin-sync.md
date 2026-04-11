# Order Module Sync: Client + Admin + Supabase

## 1. Source of truth

- `orders`: header don hang, trang thai hien tai, tong tien, dia chi giao.
- `order_items`: snapshot mon an tai thoi diem dat.
- `order_status_logs`: lich su chuyen trang thai.
- `notifications`: tin nhan cho customer/admin khi order thay doi.

Ca 2 app `FoodCouriers-Client` va `FoodCouriers-Admin` phai cung doc/ghi vao cung schema nay. Khong app nao la source of truth rieng.

## 2. Luong dong bo giua 2 app

### Client tao don

1. Client checkout.
2. Client goi `rpc_create_order(...)`.
3. Supabase tao `orders`, `order_items`, `order_status_logs`, `notifications` trong cung 1 transaction.
4. Client nhan `order_id`, `order_code`, `total`.
5. Client mo `OrderSuccessActivity` / `OrderTrackingActivity`.

### Admin xu ly don

1. Admin doc danh sach tu `orders`.
2. Admin doi trang thai qua `rpc_update_order_status(...)`.
3. RPC validate transition hop le:
   - `pending -> confirmed/cancelled`
   - `confirmed -> preparing/cancelled`
   - `preparing -> delivering/cancelled`
   - `delivering -> delivered/cancelled`
4. Supabase cap nhat `orders.status`, ghi them `order_status_logs`, tao `notifications`.
5. Realtime phat event tren bang `orders`.
6. Client dang subscribe theo `order_id` se cap nhat UI ngay.

## 3. WebSocket / Realtime nen dat o dau

- Client app:
  - subscribe 1 don cu the trong `OrderTrackingActivity`
  - co the subscribe danh sach don dang active trong `OrdersFragment`
- Admin app:
  - subscribe danh sach order theo `restaurant_id`
  - khi co order moi thi refresh list / show badge

Realtime chi nen dung de nhan thay doi. Logic nghiep vu van phai di qua PostgreSQL RPC hoac REST co validate + RLS.

## 4. Cach chia trach nhiem giua 2 app

- Client:
  - tao order
  - xem order cua chinh user
  - theo doi status
- Admin:
  - xem tat ca order cua restaurant/he thong
  - update status
  - assign shipper
  - cancel order voi ly do

Phan quyen nay duoc khoa bang RLS, khong dua vao viec an `anon key`.

## 5. Cho can custom trong Supabase

### Schema

- Neu can them timeline chi tiet, uu tien them cot hoac bang phu vao `order_status_logs`, khong nhoi het vao `orders`.
- Neu can tracking shipper GPS, tao bang rieng vi tri shipper thay vi ghi de len `orders`.

### RLS

- Client chi `SELECT` order cua user dang dang nhap.
- Admin/staff chi `UPDATE` order thuoc nha hang duoc phan cong.
- Neu co shipper app sau nay, tach them policy cho `shipper`.

### RPC

- `rpc_create_order`: dat hang an toan, tinh tong tien, apply promotion.
- `rpc_update_order_status`: validate state machine.
- Co the them:
  - `rpc_assign_shipper`
  - `rpc_cancel_order_by_customer`
  - `rpc_reorder`

### Realtime

- Bat Realtime cho bang `orders`.
- Neu muon timeline day du o client, bat them cho `order_status_logs`.
- Client nen fallback polling khi websocket rot.

## 6. Mapping thuc te cho code Android

- `OrderRepository`:
  - sau nay doi mock bang REST/RPC + Realtime client
  - giu API o repository on dinh de UI/ViewModel khong doi nhieu
- `OrdersViewModel`:
  - load list theo filter
  - load detail theo `orderId`
- `OrderTrackingActivity`:
  - luc nay dang dung data repository
  - buoc tiep theo la gan `subscribeOrder(orderId)`

## 7. Huong nang cap tiep theo de len Supabase that

1. Thay `OrderMockDataSource` bang remote datasource:
   - `GET /rest/v1/orders?...`
   - `GET /rest/v1/order_items?...`
   - `POST /rest/v1/rpc/rpc_create_order`
2. Bo sung `SupabaseConfig` cho:
   - `REST_URL`
   - `AUTH_URL`
   - `REALTIME_WS_URL`
3. Tao `SupabaseRealtimeClient` cho Client, dung chung message format voi Admin.
4. Luu `access_token` va gan vao header `Authorization: Bearer <token>`.
5. Test 1 luong day du:
   - Client tao order
   - Admin confirm
   - Client tracking nhan update
   - Admin giao xong
   - Client history doi status sang `Hoan thanh`
