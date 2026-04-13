# Module: Quản lý đơn hàng - App Client

## 1. Mục tiêu

Module Order Management dùng cho Client để:

- Tạo đơn hàng từ giỏ hàng.
- Theo dõi trạng thái đơn hàng theo thời gian thực.
- Xem lịch sử đơn hàng.
- Hủy đơn (khi chưa được xác nhận).
- Đánh giá sau khi nhận hàng.

Tài liệu này chuẩn hóa nghiệp vụ order bên client, đồng bộ với Admin theo tài liệu `docs/ba/admin/03-order-management.md`.

## 2. Hai nhóm trạng thái

### 2.1 Order status

`orders.status` phản ánh vòng đời nghiệp vụ của đơn:

| Trạng thái | Tiếng Việt | Mô tả |
|---|---|---|
| `pending` | Chờ xác nhận | Đơn mới tạo, chờ nhà hàng xác nhận |
| `confirmed` | Đã xác nhận | Nhà hàng đã chấp nhận đơn |
| `preparing` | Đang chuẩn bị | Nhà hàng đang nấu món |
| `ready_for_pickup` | Sẵn sàng lấy | Món đã xong, chờ shipper đến lấy |
| `delivering` | Đang giao | Shipper đã lấy hàng và đang giao |
| `delivered` | Đã giao | Giao hàng thành công |
| `cancelled` | Đã hủy | Đơn bị hủy |

### 2.2 Delivery status

`orders.delivery_status` phản ánh quá trình điều phối tài xế:

| Delivery status | Tiếng Việt | Mô tả |
|---|---|---|
| `unassigned` | Chưa phân công | Chưa bắt đầu tìm tài xế |
| `searching` | Đang tìm tài xế | Hệ thống đang tìm tài xế |
| `assigned` | Đã gán tài xế | Đã có tài xế nhận đơn |
| `arriving_pickup` | Đang đến | Tài xế đang đi tới nhà hàng |
| `waiting_pickup` | Đang chờ | Tài xế đã đến, đang chờ lấy hàng |
| `picked_up` | Đã lấy hàng | Tài xế đã lấy hàng |
| `completed` | Hoàn tất | Giao hàng hoàn tất |
| `failed` | Thất bại | Điều phối thất bại |

## 3. User Flow

### 3.1 Tạo đơn hàng (Checkout)

```
┌─────────────────────────────────────────────────────────────┐
│                    CHECKOUT FLOW                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Cart ──▶ Review Order ──▶ Select Address ──▶ Payment ──▶  │
│                                                   │         │
│                                                   ▼         │
│                                            Create Order     │
│                                                   │         │
│                                                   ▼         │
│                                          order.status =     │
│                                          'pending'          │
│                                          delivery_status =  │
│                                          'unassigned'        │
│                                                   │         │
│                                                   ▼         │
│                                          Order Created      │
│                                          ────────────────    │
│                                          [Theo dõi đơn]     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Theo dõi đơn hàng

```
┌─────────────────────────────────────────────────────────────┐
│               ORDER TRACKING FLOW                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Order Created ──▶ Restaurant Confirms ──▶ Preparing ──▶   │
│       │                                                      │
│       │              (delivery_status = searching)          │
│       │                       │                              │
│       │                       ▼                              │
│       │              Shipper Assigned ──▶ Arriving ──▶     │
│       │              (delivery_status = assigned)   (arriving_pickup)
│       │                                            │         │
│       │                                            ▼         │
│       │                               Waiting Pickup ──▶    │
│       │                               (waiting_pickup)      │
│       │                                      │               │
│       │                                      ▼               │
│       │                            Picked Up ──▶ Delivering│
│       │                            (picked_up)   (delivering)│
│       │                                      │               │
│       │                                      ▼               │
│       │                            Delivered ──▶ Completed│
│       │                            (delivered)  (completed)│
│       │                                                      │
│       └──────────────────▶ Cancelled                         │
│                          (if restaurant rejects)             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 4. Features

| # | Feature | Mô tả |
|---|---------|-------|
| 1 | Checkout Flow | Tạo đơn từ giỏ hàng |
| 2 | Order List | Danh sách đơn hàng theo tab |
| 3 | Order Detail | Xem chi tiết đơn |
| 4 | Order Tracking | Theo dõi trạng thái realtime |
| 5 | Cancel Order | Hủy đơn (nếu chưa xác nhận) |
| 6 | Reorder | Đặt lại đơn cũ |
| 7 | Reviews | Đánh giá sau khi nhận hàng |
| 8 | Realtime Updates | Cập nhật trạng thái tự động |

## 5. API Endpoints

### 5.1 Tạo đơn hàng

```
POST /rest/v1/orders
{
    "user_id": "uuid",
    "restaurant_id": "uuid",
    "delivery_address_id": "uuid",
    "items": [
        {"menu_item_id": "uuid", "quantity": 2, "note": "ít đường"}
    ],
    "payment_method": "cod" | "zalopay" | "vnpay",
    "note": "Gọi khi đến"
}
```

### 5.2 Lấy danh sách đơn của user

```
GET /rest/v1/orders?user_id=eq.{userId}&order=created_at.desc
```

Response:
```json
[
    {
        "id": "uuid",
        "code": "ORD-001",
        "status": "preparing",
        "delivery_status": "assigned",
        "restaurant_id": "uuid",
        "restaurant_name": "Phở Hùng",
        "total": 150000,
        "created_at": "2024-01-15T10:30:00Z"
    }
]
```

### 5.3 Lấy chi tiết đơn

```
GET /rest/v1/orders?id=eq.{orderId}&select=*,restaurant:restaurants(name,avatar_url),items:order_items(*),delivery_address:delivery_addresses(*),shipper:shippers(user_id,users(full_name,phone))`
```

### 5.4 Cập nhật trạng thái đơn (Hủy)

```
POST /rest/v1/rpc/update_order_status
{
    "p_order_id": "uuid",
    "p_new_status": "cancelled",
    "p_changed_by": "uuid",
    "p_note": "Khách hàng hủy"
}
```

### 5.5 Đánh giá đơn

```
POST /rest/v1/reviews
{
    "order_id": "uuid",
    "rating": 5,
    "comment": "Ngon lắm!"
}
```

## 6. UI Components

### 6.1 Order List (Fragment)

```xml
<LinearLayout android:orientation="vertical">
    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout">
        <Tab android:text="Tất cả"/>
        <Tab android:text="Đang xử lý"/>
        <Tab android:text="Hoàn thành"/>
        <Tab android:text="Đã hủy"/>
    </com.google.android.material.tabs.TabLayout>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh">
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvOrders"/>
    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <include layout="@layout/layout_empty_orders"/>
</LinearLayout>
```

### 6.2 Order Item (RecyclerView Item)

```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout android:orientation="vertical" android:padding="12dp">
        <LinearLayout android:orientation="horizontal">
            <TextView android:id="@+id/tvOrderCode" android:textStyle="bold"/>
            <TextView android:id="@+id/tvStatus" android:layout_gravity="end"/>
        </LinearLayout>

        <TextView android:id="@+id/tvRestaurantName"/>
        <TextView android:id="@+id/tvOrderDate"/>

        <LinearLayout android:orientation="horizontal">
            <TextView android:id="@+id/tvTotal"/>
            <TextView android:id="@+id/tvDeliveryStatus"/>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 6.3 Order Detail (Activity)

```xml
<ScrollView>
    <LinearLayout android:orientation="vertical" android:padding="16dp">
        <!-- Status Timeline -->
        <include android:id="@+id/statusTimeline" layout="@layout/layout_order_timeline"/>

        <!-- Order Info -->
        <TextView android:id="@+id/tvOrderCode"/>
        <TextView android:id="@+id/tvOrderDate"/>

        <!-- Restaurant -->
        <include android:id="@+id/restaurantInfo" layout="@layout/layout_restaurant_info"/>

        <!-- Items -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvItems"/>

        <!-- Summary -->
        <LinearLayout android:orientation="vertical">
            <TextView android:id="@+id/tvSubtotal"/>
            <TextView android:id="@+id/tvDeliveryFee"/>
            <TextView android:id="@+id/tvDiscount"/>
            <TextView android:id="@+id/tvTotal"/>
        </LinearLayout>

        <!-- Delivery Address -->
        <include android:id="@+id/deliveryAddress" layout="@layout/layout_address"/>

        <!-- Shipper Info (nếu đã gán) -->
        <include android:id="@+id/shipperInfo" layout="@layout/layout_shipper_info"/>

        <!-- Actions -->
        <LinearLayout android:orientation="horizontal">
            <Button android:id="@+id/btnCancel"/>
            <Button android:id="@+id/btnReview"/>
            <Button android:id="@+id/btnReorder"/>
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

### 6.4 Status Timeline Component

```xml
<LinearLayout android:orientation="vertical">
    <!-- Order Status -->
    <TextView android:text="Trạng thái đơn hàng"/>
    <LinearLayout android:id="@+id/llOrderStatus" android:orientation="vertical"/>

    <!-- Delivery Status -->
    <TextView android:text="Trạng thái giao hàng"/>
    <LinearLayout android:id="@+id/llDeliveryStatus" android:orientation="vertical"/>
</LinearLayout>
```

Mỗi trạng thái hiển thị:
- Icon (checked/pending/current)
- Tên trạng thái
- Thời gian (nếu có)

## 7. ViewModel

### 7.1 OrderListViewModel

```java
public class OrderListViewModel extends ViewModel {
    private MutableLiveData<List<Order>> allOrders = new MutableLiveData<>();
    private MutableLiveData<String> currentTab = new MutableLiveData<>("all");
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> error = new MutableLiveData<>();

    public void loadOrders(String tab) { }
    public void refreshOrders() { }
    public LiveData<List<Order>> getFilteredOrders() { }
}
```

### 7.2 OrderDetailViewModel

```java
public class OrderDetailViewModel extends ViewModel {
    private MutableLiveData<Order> order = new MutableLiveData<>();
    private MutableLiveData<List<OrderStatusLog>> statusLogs = new MutableLiveData<>();
    private MutableLiveData<Boolean> canCancel = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> canReview = new MutableLiveData<>(false);
    private MutableLiveData<String> error = new MutableLiveData<>();

    public void loadOrderDetail(String orderId) { }
    public void cancelOrder(String reason) { }
    public void submitReview(int rating, String comment) { }
    public void reorder() { }
}
```

### 7.3 CheckoutViewModel

```java
public class CheckoutViewModel extends ViewModel {
    private MutableLiveData<Cart> cart = new MutableLiveData<>();
    private MutableLiveData<DeliveryAddress> selectedAddress = new MutableLiveData<>();
    private MutableLiveData<String> paymentMethod = new MutableLiveData<>("cod");
    private MutableLiveData<Order> createdOrder = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> error = new MutableLiveData<>();

    public void createOrder(String note) { }
}
```

## 8. Realtime Subscription

### 8.1 Subscribe to Order Updates

```java
supabase.channel("orders")
    .on("postgres_changes",
        filter: "table=orders",
        filter: "user_id=eq." + currentUserId
    )
    .subscribe((event) {
        if (event.eventType == "UPDATE") {
            // Update local order
            // Show notification
            // Update UI if on order detail screen
        }
    });
```

### 8.2 Subscribe to Delivery Status

```java
supabase.channel("delivery:" + orderId)
    .on("postgres_changes",
        filter: "table=orders",
        filter: "id=eq." + orderId
    )
    .subscribe((event) {
        // Update delivery_status in UI
        // Update timeline
    });
```

## 9. Quy tắc hiển thị

### 9.1 Tab lọc đơn hàng

| Tab | Điều kiện |
|---|---|
| Tất cả | `status != null` |
| Đang xử lý | `status IN ('pending', 'confirmed', 'preparing', 'ready_for_pickup', 'delivering')` |
| Hoàn thành | `status = 'delivered'` |
| Đã hủy | `status = 'cancelled'` |

### 9.2 Hiển thị trạng thái

- **Order status**: Hiển thị màu sắc theo trạng thái
  - `pending`: Màu cam
  - `confirmed`, `preparing`: Màu xanh dương
  - `ready_for_pickup`: Màu xanh lá
  - `delivering`: Màu tím
  - `delivered`: Màu xanh lá đậm
  - `cancelled`: Màu đỏ

- **Delivery status**: Chỉ hiển thị khi đã có shipper hoặc đang tìm shipper

### 9.3 Các nút hành động

| Nút | Điều kiện hiển thị | Điều kiện enable |
|---|---|---|
| Hủy đơn | `status IN ('pending', 'confirmed')` | Chưa bị hủy |
| Đánh giá | `status = 'delivered'` && chưa đánh giá | Đã nhận hàng |
| Đặt lại | `status = 'delivered'` | Luôn enable |

## 10. Edge Cases

### 10.1 Không có đơn hàng

- Hiển thị hình ảnh và text "Bạn chưa có đơn hàng nào"
- Nút "Khám phá món ngay" dẫn về Home

### 10.2 Đơn bị hủy bởi nhà hàng

- Hiển thị lý do hủy (nếu có)
- Hiển thị thông báo "Đơn hàng đã bị hủy"

### 10.3 Đơn đã đánh giá

- Ẩn nút "Đánh giá"
- Hiển thị đánh giá đã gửi

### 10.4 Không thể hủy đơn

- Khi `status` đã qua `confirmed`
- Hiển thị toast "Không thể hủy đơn này"

### 10.5 Đã có shipper nhưng nhà hàng làm chậm

- Hiển thị `delivery_status = 'waiting_pickup'`
- Text: "Shipper đang chờ lấy hàng"
- Đây là trạng thái bình thường, không phải lỗi

### 10.6 Món xong nhưng chưa có shipper

- `status = 'ready_for_pickup'`
- `delivery_status = 'searching'` hoặc `'failed'`
- Hiển thị cảnh báo "Đang tìm tài xế..."

## 11. Mapping giữa Admin và Client

### 11.1 Order Status

| Admin | Client | Ý nghĩa |
|---|---|---|
| `pending` | `pending` | Chờ xác nhận |
| `confirmed` | `confirmed` | Đã xác nhận |
| `preparing` | `preparing` | Đang chuẩn bị |
| `ready_for_pickup` | `ready_for_pickup` | Sẵn sàng lấy |
| `delivering` | `delivering` | Đang giao |
| `delivered` | `delivered` | Đã giao |
| `cancelled` | `cancelled` | Đã hủy |

### 11.2 Delivery Status

| Admin | Client | Ý nghĩa |
|---|---|---|
| `unassigned` | `unassigned` | Chưa phân công |
| `searching` | `searching` | Đang tìm tài xế |
| `assigned` | `assigned` | Đã gán tài xế |
| `arriving_pickup` | `arriving_pickup` | Đang đến |
| `waiting_pickup` | `waiting_pickup` | Đang chờ |
| `picked_up` | `picked_up` | Đã lấy hàng |
| `completed` | `completed` | Hoàn tất |
| `failed` | `failed` | Thất bại |

## 12. Kết luận

Tài liệu này chuẩn hóa nghiệp vụ Order bên Client, đồng bộ với:

- Tài liệu Admin: `docs/ba/admin/03-order-management.md`
- Database: `docs/supabase/migrations/012_order_delivery_refactor.sql`

Client cần theo dõi cả `status` và `delivery_status` để hiển thị chính xác trạng thái đơn hàng cho người dùng.

---

## 13. Thanh toán (Payment)

### 13.1 Phương thức thanh toán

| Phương thức | Mã | Mô tả |
|---|---|---|
| Tiền mặt | `cod` | Thanh toán khi nhận hàng |
| Online | `online` | Thanh toán qua ZaloPay/VNPay |

### 13.2 Payment Status

| Status | Tiếng Việt | Mô tả |
|---|---|---|
| `pending` | Chờ thanh toán | COD chờ giao hàng, Online chờ thanh toán |
| `paid` | Đã thanh toán | Đã nhận tiền |
| `failed` | Thất bại | Thanh toán online thất bại |
| `refunded` | Đã hoàn tiền | Hoàn tiền cho khách |

### 13.3 Flow thanh toán

```
┌─────────────────────────────────────────────────────────────┐
│                   PAYMENT FLOW                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Checkout ──▶ Select Payment Method                        │
│                    │                                        │
│                    ├─▶ COD ──▶ orders.payment_status =     │
│                    │               'pending'                 │
│                    │                │                        │
│                    │                ▼                        │
│                    │        Order Created                   │
│                    │                │                        │
│                    │                ▼                        │
│                    │        Delivery & Collect Money        │
│                    │                │                        │
│                    │                ▼                        │
│                    │        success: payment_status =       │
│                    │               'paid'                    │
│                    │                                        │
│                    └─▶ Online ──▶ Create Payment Link        │
│                                     │                        │
│                                     ▼                        │
│                             Redirect to Payment App          │
│                                     │                        │
│                                     ▼                        │
│                        Payment Result Callback              │
│                                     │                        │
│                    ┌────────────────┼────────────────┐     │
│                    │                │                │     │
│                    ▼                ▼                ▼     │
│                 Success         Failed           Pending  │
│                    │                │                │     │
│                    ▼                ▼                ▼     │
│            payment_status =   payment_status =  payment_    │
│            'paid'              'failed'         status =    │
│                                               'pending'     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 13.4 API Payment

#### Tạo thanh toán online (Phase 2)
```
POST /rest/v1/rpc/create_payment
{
    "p_order_id": "uuid",
    "p_amount": 150000,
    "p_provider": "zalopay" | "vnpay"
}
```

#### Update payment status
```
POST /rest/v1/rpc/update_payment_status
{
    "p_order_id": "uuid",
    "p_status": "paid" | "failed"
}
```

#### Ghi transaction log
```
POST /rest/v1/payment_transactions
{
    "order_id": "uuid",
    "provider": "zalopay",
    "external_txn_id": "zalopay_txn_id",
    "amount": 150000,
    "status": "success",
    "raw_response": "..."
}
```

### 13.5 UI Payment

```xml
<!-- Payment Method Selection -->
<RadioGroup android:id="@+id/rgPaymentMethod">
    <RadioButton
        android:id="@+id/rbCod"
        android:text="Tiền mặt (COD)"
        android:checked="true"/>
    <RadioButton
        android:id="@+id/rbOnline"
        android:text="Thanh toán online"/>
</RadioGroup>

<!-- Order Summary -->
<LinearLayout>
    <TextView android:id="@+id/tvSubtotal"/>    <!-- Tổng tiền món -->
    <TextView android:id="@+id/tvDeliveryFee"/> <!-- Phí vận chuyển -->
    <TextView android:id="@+id/tvDiscount"/>    <!-- Giảm giá -->
    <TextView android:id="@+id/tvTotal"/>       <!-- Tổng cộng -->
</LinearLayout>
```

## 14. Voucher / Khuyến mãi (Promotions)

### 14.1 Promotions Table

| Column | Type | Mô tả |
|---|---|---|
| `id` | UUID | PK |
| `code` | TEXT | Mã voucher |
| `name` | TEXT | Tên chương trình |
| `description` | TEXT | Mô tả |
| `discount_type` | ENUM | `percent` hoặc `fixed` |
| `discount_value` | INTEGER | Giá trị giảm |
| `min_order` | INTEGER | Đơn tối thiểu |
| `max_discount` | INTEGER | Giảm tối đa |
| `start_date` | TIMESTAMP | Bắt đầu |
| `end_date` | TIMESTAMP | Kết thúc |
| `usage_limit` | INTEGER | Số lần sử dụng tối đa |
| `usage_count` | INTEGER | Đã sử dụng |
| `is_active` | BOOLEAN | Kích hoạt |

### 14.2 Logic tính giảm giá

```
IF discount_type = 'percent' THEN
    discount = subtotal * discount_value / 100
    IF max_discount IS NOT NULL AND discount > max_discount THEN
        discount = max_discount
    END IF
ELSE
    discount = discount_value
END IF
```

### 14.3 Validation Rules

| Rule | Điều kiện |
|---|---|
| Mã hợp lệ | `is_active = true` && `NOW() BETWEEN start_date AND end_date` |
| Giới hạn sử dụng | `usage_count < usage_limit` |
| Đơn tối thiểu | `subtotal >= min_order` |
| Mỗi user 1 lần | Kiểm tra `promotion_usages.user_id` |

### 14.4 API Promotions

#### Lấy danh sách voucher khả dụng
```
GET /rest/v1/promotions?is_active=true&start_date=lte.now()&end_date=gte.now()
```

#### Validate voucher
```
POST /rest/v1/rpc/validate_promotion
{
    "p_promotion_code": "FLASHSALE20",
    "p_user_id": "uuid",
    "p_subtotal": 150000
}
```

Response:
```json
{
    "valid": true,
    "discount": 30000,
    "message": "Áp dụng thành công"
}
```

#### Áp dụng voucher khi checkout
```
POST /rest/v1/rpc/create_order
{
    "p_user_id": "uuid",
    "p_restaurant_id": "uuid",
    "p_delivery_address": "123 ABC",
    "p_items": [...],
    "p_promotion_code": "FLASHSALE20",  // Mã voucher
    "p_payment_method": "cod"
}
```

### 14.5 UI Voucher

```xml
<!-- Voucher Selection -->
<LinearLayout android:orientation="vertical">
    <TextView android:text="Mã giảm giá"/>

    <LinearLayout android:orientation="horizontal">
        <EditText
            android:id="@+id/etVoucherCode"
            android:hint="Nhập mã voucher"/>
        <Button android:id="@+id/btnApplyVoucher"
            android:text="Áp dụng"/>
    </LinearLayout>

    <!-- Available Vouchers -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvVouchers"/>

    <!-- Applied Voucher Info -->
    <LinearLayout
        android:id="@+id/llAppliedVoucher"
        android:visibility="gone">
        <TextView android:id="@+id/tvAppliedVoucherCode"/>
        <TextView android:id="@+id/tvAppliedDiscount"/>
        <Button android:id="@+id/btnRemoveVoucher"
            android:text="Xóa"/>
    </LinearLayout>
</LinearLayout>
```

### 14.6 ViewModel

```java
public class CheckoutViewModel extends ViewModel {
    // ... existing fields ...

    // Voucher
    private MutableLiveData<Promotion> appliedPromotion = new MutableLiveData<>();
    private MutableLiveData<List<Promotion>> availablePromotions = new MutableLiveData<>();
    private MutableLiveData<Integer> discountAmount = new MutableLiveData<>(0);

    public void loadAvailablePromotions() { }
    public void applyVoucher(String code) { }
    public void removeVoucher() { }

    // Calculate total
    public int calculateTotal(int subtotal, int deliveryFee) {
        int discount = discountAmount.getValue() != null ? discountAmount.getValue() : 0;
        return subtotal + deliveryFee - discount;
    }
}
```

## 15. Tổng kết thanh toán

### 15.1 Công thức tính tiền

```
subtotal = SUM(menu_item_price * quantity)
delivery_fee = restaurant.delivery_fee (hoặc tính theo khoảng cách)
discount = promotion.discount_value (nếu áp dụng)
total = subtotal + delivery_fee - discount
```

### 15.2 Order Summary UI

```xml
<LinearLayout android:orientation="vertical" android:padding="16dp">
    <!-- Items -->
    <TextView android:text="Tổng tiền món" android:id="@+id/tvSubtotalLabel"/>
    <TextView android:text="150.000đ" android:id="@+id/tvSubtotal"/>

    <!-- Delivery -->
    <TextView android:text="Phí vận chuyển" android:id="@+id/tvDeliveryFeeLabel"/>
    <TextView android:text="15.000đ" android:id="@+id/tvDeliveryFee"/>

    <!-- Discount (nếu có) -->
    <LinearLayout android:id="@+id/llDiscount" android:visibility="gone">
        <TextView android:text="Giảm giá" android:id="@+id/tvDiscountLabel"/>
        <TextView android:text="-20.000đ" android:id="@+id/tvDiscount"/>
    </LinearLayout>

    <View android:background="@android:color/darker_gray"/>

    <!-- Total -->
    <LinearLayout android:orientation="horizontal">
        <TextView android:text="Tổng cộng" android:textStyle="bold"/>
        <TextView android:id="@+id/tvTotal" android:textStyle="bold"/>
    </LinearLayout>
</LinearLayout>
```

### 15.3 Edge Cases

| Case | Handling |
|---|---|
| Voucher hết hạn | Show error "Mã voucher đã hết hạn" |
| Voucher đã dùng | Show error "Mã voucher đã được sử dụng" |
| Đơn không đủ điều kiện | Show error "Đơn hàng chưa đạt tối thiểu Xđ" |
| Thanh toán thất bại | Show retry dialog, keep order with payment_status='failed' |
| Thanh toán timeout | Show "Vui lòng thử lại", allow retry |
| COD không thanh toán | Staff update payment_status='paid' when collecting |