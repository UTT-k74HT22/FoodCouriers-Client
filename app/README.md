# FoodDelivery — App Khách Hàng (Client)

> Ứng dụng đặt đồ ăn trực tuyến dành cho người dùng cuối.
> Nền tảng: **Java Android · XML Layout · Supabase**

---

## Mục Lục

- [Tổng quan](#tổng-quan)
- [Tính năng](#tính-năng)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc project](#cấu-trúc-project)
- [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
- [Kiến trúc ứng dụng](#kiến-trúc-ứng-dụng)
- [Màn hình chính](#màn-hình-chính)
- [Luồng đặt hàng](#luồng-đặt-hàng)
- [Luồng theo dõi đơn hàng](#luồng-theo-dõi-đơn-hàng)
- [Cart — Local-first strategy](#cart--local-first-strategy)
- [Design System](#design-system)
- [Roadmap & Phases](#roadmap--phases)

---

## Tổng Quan

**FoodDelivery Client** là ứng dụng Android dành cho khách hàng, cho phép:

- Duyệt nhà hàng và thực đơn, tìm kiếm món ăn
- Thêm món vào giỏ hàng và đặt hàng (thanh toán COD)
- Theo dõi trạng thái đơn hàng theo thời gian thực
- Quản lý hồ sơ cá nhân và địa chỉ giao hàng
- Xem lịch sử đơn hàng

| Thông tin | Chi tiết |
|---|---|
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |
| Ngôn ngữ | Java |
| Layout | XML |
| Architecture | MVVM + Repository Pattern |
| Backend | Supabase (PostgreSQL + Auth + Storage + Realtime) |
| Thanh toán | COD (tiền mặt khi nhận hàng) |

---

## Tính Năng

### Xác thực

- Đăng ký tài khoản bằng email và mật khẩu
- Đăng nhập / đăng xuất
- Tự động đăng nhập lại khi có session hợp lệ (token còn hạn)
- Đặt lại mật khẩu qua email (Supabase Auth)

> Phase 2: Đăng nhập Google / Facebook

### Trang Chủ

- Banner khuyến mãi (ViewPager2 auto-scroll)
- Danh mục đồ ăn cuộn ngang (Cơm, Phở, Pizza, Bún, ...)
- Danh sách nhà hàng nổi bật
- Truy cập nhanh đơn hàng gần nhất

> Phase 2: Banner được quản lý từ Admin App

### Khám Phá & Tìm Kiếm

- Tìm kiếm theo tên món ăn hoặc nhà hàng (full-text search qua PostgreSQL)
- Lọc theo danh mục
- Kết quả tìm kiếm hiển thị cả nhà hàng lẫn món ăn

> Phase 2: Sort theo giá, lọc theo rating

### Chi Tiết Nhà Hàng & Thực Đơn

- Thông tin nhà hàng: tên, địa chỉ, giờ mở cửa, rating
- Thực đơn nhóm theo danh mục (sticky header)
- Chi tiết món ăn: ảnh, mô tả, giá
- Nút thêm vào giỏ hàng trực tiếp từ danh sách

### Giỏ Hàng

- Thêm / bớt số lượng / xóa món
- Hiển thị tổng tiền (subtotal + phí giao hàng)
- Cảnh báo khi thêm món từ nhà hàng khác (chỉ 1 nhà hàng/đơn)
- Nhập mã giảm giá / voucher (Phase 2)
- Giỏ hàng được lưu local — không mất khi tắt app

### Đặt Hàng

- Nhập hoặc chọn địa chỉ giao hàng
- Ghi chú cho đơn hàng
- Xem tóm tắt đơn hàng trước khi xác nhận
- Chọn phương thức thanh toán: COD (mặc định)
- Xác nhận đặt hàng → tạo đơn ngay lập tức

### Theo Dõi Đơn Hàng

- Thanh tiến trình trực quan (5 bước)
- Trạng thái cập nhật theo thời gian thực (Supabase Realtime)
- Xem chi tiết từng bước: thời gian xác nhận, thời gian bắt đầu giao

> Lưu ý: Không có bản đồ GPS tracking shipper trong phạm vi hiện tại. Trạng thái được cập nhật thủ công bởi admin/staff.

### Lịch Sử Đơn Hàng

- Danh sách đơn hàng theo thời gian
- Lọc theo trạng thái (đang xử lý / hoàn thành / đã hủy)
- Xem chi tiết từng đơn (items, giá, địa chỉ, timeline)
- Đặt lại nhanh từ đơn cũ (Phase 2)

### Hồ Sơ Cá Nhân

- Xem và chỉnh sửa thông tin: tên, số điện thoại, avatar
- Đổi mật khẩu
- Quản lý địa chỉ giao hàng (Phase 2)
- Đăng xuất

### Đánh Giá (Phase 2)

- Đánh giá sau khi nhận hàng (rating 1–5 sao + comment)
- Xem đánh giá của người dùng khác trên trang nhà hàng

---

## Công Nghệ Sử Dụng

### Supabase Platform

| Dịch vụ | Mục đích |
|---|---|
| **Supabase Auth** | Đăng ký / đăng nhập, JWT token, session management |
| **PostgreSQL** | Database chính: users, orders, restaurants, menu_items, ... |
| **Row Level Security (RLS)** | Customer chỉ đọc/ghi dữ liệu của chính mình |
| **Supabase Storage** | Ảnh nhà hàng, ảnh món ăn, avatar người dùng |
| **Supabase Realtime** | Cập nhật trạng thái đơn hàng theo thời gian thực (WebSocket) |
| **PostgREST** | REST API tự động từ schema — không cần viết server riêng |
| **PostgreSQL RPC** | Đặt hàng (atomic transaction), báo cáo |

### Android Libraries

| Thư viện | Version | Mục đích |
|---|---|---|
| Supabase Kotlin SDK | 2.x | Giao tiếp với Supabase (Auth, DB, Storage, Realtime) |
| Glide | 4.16.0 | Tải và cache hình ảnh |
| Room | 2.x | Lưu cart local (offline-first) |
| Material Design 3 | 1.10.0 | UI Components |
| Navigation Component | 2.x | Điều hướng Fragment (Bottom Navigation) |
| ViewPager2 | 1.x | Banner tự động cuộn, tab layout |
| SwipeRefreshLayout | — | Pull-to-refresh danh sách |
| CircleImageView | 3.1.0 | Ảnh avatar hình tròn |

> **Lưu ý về SDK:** Supabase SDK chính thức viết bằng Kotlin nhưng tương thích với Java project qua interop. Fallback: gọi Supabase REST API trực tiếp qua OkHttp nếu cần.

---

## Cấu Trúc Project

```
app/
└── src/main/
    └── java/com/fooddelivery/client/
        │
        ├── data/
        │   ├── model/                           # Domain models (Java POJOs)
        │   │   ├── User.java
        │   │   ├── Restaurant.java
        │   │   ├── Category.java
        │   │   ├── MenuItem.java
        │   │   ├── Cart.java
        │   │   ├── CartItem.java
        │   │   ├── Order.java
        │   │   ├── OrderItem.java
        │   │   ├── Address.java
        │   │   ├── Banner.java
        │   │   └── Review.java
        │   │
        │   ├── remote/
        │   │   ├── SupabaseClient.java           # Singleton Supabase client
        │   │   └── dto/                          # Data Transfer Objects (JSON → model)
        │   │       ├── RestaurantDto.java
        │   │       ├── MenuItemDto.java
        │   │       ├── OrderDto.java
        │   │       └── UserDto.java
        │   │
        │   ├── local/
        │   │   ├── AppDatabase.java              # Room database (cart local cache)
        │   │   ├── dao/
        │   │   │   └── CartDao.java
        │   │   └── entity/
        │   │       └── CartItemEntity.java
        │   │
        │   └── repository/
        │       ├── AuthRepository.java           # Đăng ký, đăng nhập, session
        │       ├── RestaurantRepository.java     # Danh sách, chi tiết nhà hàng
        │       ├── MenuRepository.java           # Danh sách món ăn theo nhà hàng
        │       ├── CartRepository.java           # Giỏ hàng (Room DB)
        │       ├── OrderRepository.java          # Tạo, xem, theo dõi đơn hàng
        │       ├── SearchRepository.java         # Full-text search
        │       ├── UserRepository.java           # Profile, địa chỉ
        │       └── BannerRepository.java         # Banner trang chủ
        │
        ├── domain/
        │   └── usecase/
        │       ├── PlaceOrderUseCase.java        # Tính giá, validate, tạo đơn (RPC)
        │       ├── AddToCartUseCase.java         # Validate 1 nhà hàng/giỏ
        │       └── GetMenuGroupedUseCase.java    # Nhóm món theo danh mục
        │
        ├── ui/
        │   ├── splash/
        │   │   ├── SplashActivity.java           # Kiểm tra session → redirect
        │   │   └── activity_splash.xml
        │   │
        │   ├── auth/
        │   │   ├── LoginActivity.java
        │   │   ├── RegisterActivity.java
        │   │   ├── ForgotPasswordActivity.java
        │   │   ├── activity_login.xml
        │   │   └── activity_register.xml
        │   │
        │   ├── main/
        │   │   ├── MainActivity.java             # BottomNavigationView container
        │   │   └── activity_main.xml
        │   │
        │   ├── home/
        │   │   ├── HomeFragment.java             # Banner + Category + Restaurant list
        │   │   ├── fragment_home.xml
        │   │   └── adapter/
        │   │       ├── BannerAdapter.java        # ViewPager2 banner
        │   │       ├── CategoryAdapter.java      # RecyclerView ngang
        │   │       └── RestaurantAdapter.java
        │   │
        │   ├── search/
        │   │   ├── SearchFragment.java
        │   │   ├── fragment_search.xml
        │   │   └── adapter/
        │   │       └── SearchResultAdapter.java
        │   │
        │   ├── restaurant/
        │   │   ├── RestaurantDetailActivity.java # Info + menu grouped by category
        │   │   ├── activity_restaurant_detail.xml
        │   │   └── adapter/
        │   │       ├── MenuGroupAdapter.java     # Outer adapter (category header)
        │   │       └── MenuItemAdapter.java      # Inner adapter (items)
        │   │
        │   ├── menu/
        │   │   └── MenuItemDetailBottomSheet.java  # Ảnh, mô tả, giá, nút thêm giỏ
        │   │
        │   ├── cart/
        │   │   ├── CartActivity.java
        │   │   ├── activity_cart.xml
        │   │   └── adapter/
        │   │       └── CartItemAdapter.java
        │   │
        │   ├── checkout/
        │   │   ├── CheckoutActivity.java         # Địa chỉ, ghi chú, tóm tắt, xác nhận
        │   │   └── activity_checkout.xml
        │   │
        │   ├── order/
        │   │   ├── OrdersFragment.java           # Danh sách lịch sử đơn (tabs)
        │   │   ├── OrderDetailActivity.java      # Chi tiết đơn + timeline
        │   │   ├── OrderTrackingActivity.java    # Theo dõi realtime status
        │   │   ├── fragment_orders.xml
        │   │   ├── activity_order_detail.xml
        │   │   ├── activity_order_tracking.xml
        │   │   └── adapter/
        │   │       └── OrderAdapter.java
        │   │
        │   ├── profile/
        │   │   ├── ProfileFragment.java
        │   │   ├── EditProfileActivity.java
        │   │   ├── AddressListActivity.java      # Phase 2
        │   │   ├── AddressFormActivity.java      # Phase 2
        │   │   ├── fragment_profile.xml
        │   │   └── activity_edit_profile.xml
        │   │
        │   └── common/
        │       ├── BaseActivity.java
        │       ├── BaseFragment.java
        │       ├── BaseViewModel.java
        │       ├── LoadingDialog.java
        │       └── EmptyStateView.java           # Custom view empty/error state
        │
        ├── viewmodel/
        │   ├── AuthViewModel.java
        │   ├── HomeViewModel.java
        │   ├── SearchViewModel.java
        │   ├── RestaurantViewModel.java
        │   ├── CartViewModel.java
        │   ├── CheckoutViewModel.java
        │   ├── OrderViewModel.java
        │   └── ProfileViewModel.java
        │
        └── utils/
            ├── Constants.java
            ├── SessionManager.java               # JWT token (EncryptedSharedPreferences)
            ├── CurrencyUtils.java                # Format VND
            ├── DateTimeUtils.java
            ├── ImageUtils.java                   # Compress ảnh trước upload
            └── NetworkUtils.java                 # Check connectivity
```

---

## Hướng Dẫn Cài Đặt

### Yêu cầu

- Android Studio **Hedgehog** (2023.1.1) trở lên
- JDK 17
- Gradle 8.x
- Thiết bị / emulator Android API 26+

### Các bước

```bash
# 1. Clone repository
git clone https://github.com/your-org/food-delivery-client.git
cd food-delivery-client

# 2. Mở bằng Android Studio
# File → Open → chọn thư mục vừa clone

# 3. Tạo file local.properties (không commit lên git)
echo "SUPABASE_URL=https://your-project.supabase.co" >> local.properties
echo "SUPABASE_ANON_KEY=eyJ..." >> local.properties

# 4. Sync Gradle và chạy
# Nhấn "Sync Now" → Run (Shift + F10)
```

### Tài khoản test

| Role | Email | Password |
|---|---|---|
| Customer | customer@demo.com | Demo@1234 |
| Customer 2 | customer2@demo.com | Demo@1234 |

### Biến môi trường (`local.properties`)

```properties
# Supabase project URL
SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co

# Supabase anon/public key (safe để expose trong client app)
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

> `SUPABASE_ANON_KEY` là public key, an toàn để dùng trong client app. Bảo mật dữ liệu được thực hiện qua **Supabase RLS**, không qua việc giấu key.

---

## Kiến Trúc Ứng Dụng

```
┌────────────────────────────────────────────────┐
│                   UI Layer                      │
│   Activity / Fragment / BottomSheet             │
│   XML Layout / Adapter / ViewHolder             │
│   BottomNavigationView: Home/Search/Orders/Profile│
└───────────────────┬────────────────────────────┘
                    │ observe LiveData / call
┌───────────────────▼────────────────────────────┐
│              ViewModel Layer                    │
│   Xử lý business logic, giữ trạng thái UI      │
│   Không phụ thuộc Android framework             │
│   HomeVM / CartVM / CheckoutVM / OrderVM...     │
└───────────────────┬────────────────────────────┘
                    │ call
┌───────────────────▼────────────────────────────┐
│   Domain Layer (UseCase — chỉ khi cần thiết)   │
│   PlaceOrderUseCase (validate + tạo đơn)        │
│   AddToCartUseCase (validate 1 nhà hàng/giỏ)   │
└───────────────────┬────────────────────────────┘
                    │ call
┌───────────────────▼────────────────────────────┐
│            Repository Layer                     │
│   Kết hợp dữ liệu local (Room) + remote (Supabase)│
│   CartRepo / OrderRepo / RestaurantRepo...      │
└──────────┬────────────────────┬────────────────┘
           │                    │
┌──────────▼──────┐    ┌────────▼───────────────┐
│  Room Database  │    │   Supabase Platform     │
│  (Cart local    │    │   Auth / PostgreSQL      │
│   cache)        │    │   Storage / Realtime     │
└─────────────────┘    └────────────────────────┘
```

**Điểm thiết kế quan trọng:**

- **Cart là local-first:** Lưu trong Room DB, không phụ thuộc mạng. Chỉ sync lên Supabase khi checkout.
- **PlaceOrderUseCase** gọi PostgreSQL RPC để đảm bảo atomic transaction (insert orders + order_items trong 1 lần).
- **Supabase Realtime** subscription trên `orders` table — màn hình tracking tự cập nhật khi admin đổi trạng thái.
- **AddToCartUseCase** validate conflict nhà hàng trước khi add vào Room DB.

---

## Màn Hình Chính

### Auth Flow

| Màn hình | File | Mô tả |
|---|---|---|
| Splash | `SplashActivity` | Kiểm tra session → tự động vào Home hoặc Login |
| Đăng nhập | `LoginActivity` | Email + mật khẩu |
| Đăng ký | `RegisterActivity` | Tên, email, số điện thoại, mật khẩu |
| Quên mật khẩu | `ForgotPasswordActivity` | Gửi email đặt lại mật khẩu |

### Main Flow (Bottom Navigation)

| Tab | Fragment | Mô tả |
|---|---|---|
| Home | `HomeFragment` | Banner, danh mục, nhà hàng nổi bật |
| Search | `SearchFragment` | Tìm kiếm món ăn / nhà hàng |
| Orders | `OrdersFragment` | Lịch sử và đơn đang xử lý |
| Profile | `ProfileFragment` | Hồ sơ cá nhân, cài đặt |

### Detail Screens

| Màn hình | File | Điều hướng từ |
|---|---|---|
| Chi tiết nhà hàng + menu | `RestaurantDetailActivity` | Home, Search |
| Chi tiết món ăn | `MenuItemDetailBottomSheet` | Restaurant Detail |
| Giỏ hàng | `CartActivity` | Cart icon (bất kỳ màn nào) |
| Thanh toán | `CheckoutActivity` | Cart |
| Theo dõi đơn | `OrderTrackingActivity` | Checkout (sau khi đặt) |
| Chi tiết đơn hàng | `OrderDetailActivity` | Orders list |
| Chỉnh sửa hồ sơ | `EditProfileActivity` | Profile |

---

## Luồng Đặt Hàng

```
[HomeFragment]
    │ Chọn nhà hàng
    ▼
[RestaurantDetailActivity]
    │ Xem thực đơn theo category
    │ Tap "Thêm vào giỏ"
    ▼
[AddToCartUseCase.execute(menuItem)]
    ├── Validate: cùng nhà hàng với giỏ hiện tại?
    │       └── Khác nhà hàng → Hiện ConfirmDialog "Xóa giỏ cũ?"
    ├── Upsert CartItemEntity vào Room DB
    └── Notify CartViewModel → update badge số lượng
    │
    ▼ (User tap Cart icon)
[CartActivity]
    │ Xem/sửa items, tổng tiền
    │ Tap "Đặt hàng"
    ▼
[CheckoutActivity]
    │ Nhập địa chỉ giao hàng
    │ Thêm ghi chú (tuỳ chọn)
    │ Xem lại tóm tắt đơn hàng
    │ Tap "Xác nhận đặt hàng"
    ▼
[PlaceOrderUseCase.execute()]
    ├── Tính: subtotal, delivery_fee, total
    ├── Tạo order_code (ORD-YYYYMMDD-xxxx)
    ├── Gọi Supabase RPC: place_order(...)
    │       ├── INSERT orders (status = 'pending')
    │       ├── INSERT order_items (snapshot name + price)
    │       └── INSERT notification cho admin
    ├── Xóa cart khỏi Room DB
    └── Navigate → OrderTrackingActivity
```

---

## Luồng Theo Dõi Đơn Hàng

```
[OrderTrackingActivity]
    │
    ├── Subscribe Supabase Realtime: orders WHERE id = {orderId}
    │
    │   Admin App cập nhật status:
    │   pending → confirmed → preparing → delivering → delivered
    │
    ├── Nhận Realtime event → update LiveData
    │
    └── UI tự cập nhật thanh tiến trình:

    [●]──────[●]──────[○]──────[○]──────[○]
   Đặt     Xác        Đang     Đang     Đã
   hàng    nhận      chuẩn     giao   nhận
                      bị

    Mỗi bước hiển thị: tên trạng thái + thời gian cập nhật
```

**Fallback khi Realtime không khả dụng:**

```java
// OrderTrackingActivity.java
// Nếu WebSocket fail, polling mỗi 15 giây
private void startPolling() {
    handler.postDelayed(() -> {
        orderViewModel.refreshOrder(orderId);
        startPolling();
    }, 15_000);
}
```

---

## Cart — Local-first Strategy

Giỏ hàng được thiết kế theo nguyên tắc **local-first** để hoạt động mượt mà kể cả khi mạng chậm.

```
User Action           Room DB (local)        Supabase (remote)
─────────────         ───────────────        ─────────────────
Thêm món         →    Upsert CartItem         (không gọi)
Xóa món          →    Delete CartItem         (không gọi)
Đổi số lượng     →    Update quantity         (không gọi)
Tắt app          →    Dữ liệu vẫn còn        (không gọi)
Mở lại app       →    Load từ Room            (không gọi)
Đặt hàng         →    Xóa toàn bộ cart   →   INSERT order + items
```

**Tại sao không lưu cart trên Supabase ngay?**
- Giảm số lần gọi API (mỗi thao tác giỏ hàng không cần network)
- UX mượt hơn, không bị loading spinner khi add/remove
- Đơn giản hơn cho MVP

> Phase 2: Sync cart lên Supabase để hỗ trợ multi-device (user đặt trên điện thoại khác vẫn thấy giỏ hàng).

---

## Design System

| Token | Giá trị | Dùng cho |
|---|---|---|
| `colorPrimary` | `#FF6B35` | Nút chính, Bottom Nav active, highlight |
| `colorPrimaryDark` | `#E55A2B` | Pressed state của nút chính |
| `colorSecondary` | `#1A237E` | Header, text tiêu đề quan trọng |
| `colorBackground` | `#F8F8F8` | Nền màn hình |
| `colorSurface` | `#FFFFFF` | Card, bottom sheet, dialog |
| `colorOnSurface` | `#212121` | Text chính trên card |
| `colorTextSecondary` | `#757575` | Text phụ, mô tả, placeholder |
| `colorSuccess` | `#4CAF50` | Đơn hoàn thành, trạng thái active |
| `colorWarning` | `#FFC107` | Đơn đang xử lý, cảnh báo |
| `colorError` | `#F44336` | Lỗi, đơn bị hủy |
| `colorDivider` | `#E0E0E0` | Đường kẻ ngăn cách |

### Typography

| Style | Size | Weight | Dùng cho |
|---|---|---|---|
| `titleLarge` | 22sp | Bold | Tên nhà hàng, tên màn hình |
| `titleMedium` | 16sp | SemiBold | Tên món ăn, tên section |
| `bodyLarge` | 16sp | Regular | Nội dung chính |
| `bodyMedium` | 14sp | Regular | Mô tả, địa chỉ |
| `labelSmall` | 12sp | Regular | Badge, tag, timestamp |
| `priceText` | 16sp | Bold | Giá tiền (màu `colorPrimary`) |

---

## Quy Tắc Code

- Không gọi Supabase trực tiếp từ Activity/Fragment — luôn qua ViewModel → Repository
- Cart operations chỉ dùng Room DB, không gọi network
- Mọi màn hình cần có 3 state: **loading** / **content** / **empty/error**
- Ảnh từ Supabase Storage load bằng Glide với placeholder và error drawable
- Validate input phía client trước khi gọi API (email format, mật khẩu tối thiểu 6 ký tự, ...)
- JWT token lưu trong `EncryptedSharedPreferences`, không dùng plain SharedPreferences
- Compress ảnh xuống tối đa 800KB trước khi upload avatar lên Supabase Storage
- `PlaceOrderUseCase` phải gọi qua RPC (không INSERT trực tiếp nhiều bảng từ client)

---

## Roadmap & Phases

### Phase 1 — MVP (Tuần 1–10)

- [x] Auth: đăng ký / đăng nhập / đăng xuất / auto-login
- [x] Home: banner, danh mục, danh sách nhà hàng
- [x] Browse: xem menu nhà hàng, chi tiết món ăn
- [x] Search: tìm kiếm theo tên món / nhà hàng
- [x] Cart: local-first (Room DB), validate 1 nhà hàng/giỏ
- [x] Checkout: địa chỉ text, ghi chú, COD
- [x] Order tracking: trạng thái realtime (Supabase Realtime)
- [x] Order history: danh sách + chi tiết đơn hàng
- [x] Profile: xem và chỉnh sửa thông tin cá nhân

### Phase 2 — Enhancement (Tuần 11–13)

- [ ] Địa chỉ giao hàng: lưu nhiều địa chỉ, chọn khi checkout
- [ ] Review & Rating: đánh giá sau khi nhận hàng
- [ ] Promotion code: nhập mã giảm giá tại checkout
- [ ] Notification in-app: danh sách thông báo cập nhật đơn
- [ ] Filter nâng cao: theo giá, danh mục, rating
- [ ] Đặt lại nhanh từ đơn hàng cũ

### Phase 3 — Polish (Tuần 14–16)

- [ ] Nhà hàng yêu thích (Favorites)
- [ ] Onboarding screens (lần đầu mở app)
- [ ] Animations & transitions mượt hơn
- [ ] Dark mode
- [ ] Đăng nhập Google

---

*FoodDelivery Client App · Java Android · Supabase · Version 1.0.0*
