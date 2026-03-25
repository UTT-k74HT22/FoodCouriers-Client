# 🍔 FoodDelivery — App Khách Hàng (Client)

> Ứng dụng đặt và giao đồ ăn trực tuyến dành cho người dùng cuối.  
> Nền tảng: **Java Android · XML Layout · Firebase**

---

## 📋 Mục Lục

- [Tổng quan](#-tổng-quan)
- [Tính năng](#-tính-năng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc project](#-cấu-trúc-project)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
- [Cấu hình Firebase](#-cấu-hình-firebase)
- [Kiến trúc ứng dụng](#-kiến-trúc-ứng-dụng)
- [Màn hình chính](#-màn-hình-chính)
- [Luồng đặt hàng](#-luồng-đặt-hàng)
- [Biến môi trường](#-biến-môi-trường)

---

## 📱 Tổng Quan

**FoodDelivery Client** là ứng dụng Android dành cho khách hàng, cho phép:
- Tìm kiếm và đặt món từ các nhà hàng gần đó
- Theo dõi đơn hàng và vị trí shipper theo thời gian thực
- Thanh toán qua nhiều hình thức (COD, MoMo, ZaloPay, VNPay)
- Chat trực tiếp với shipper trong quá trình giao hàng

| Thông tin | Chi tiết |
|-----------|----------|
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 14 (API 34) |
| Ngôn ngữ | Java |
| Layout | XML |
| Architecture | MVVM + LiveData |

---

## ✨ Tính Năng

### 🔐 Xác thực
- Đăng nhập / đăng ký bằng số điện thoại (OTP Firebase)
- Đăng nhập Google (Firebase Google Sign-In)
- Onboarding 3 slide khi lần đầu mở app

### 🏠 Trang Chủ
- Banner khuyến mãi tự động cuộn (ViewPager2)
- Danh mục đồ ăn ngang (Burger, Pizza, Cơm, Bún, Trà sữa...)
- Nhà hàng gần vị trí hiện tại
- Nhà hàng đang hot / được đề xuất
- Tải thêm không giới hạn (Firestore pagination)

### 🍽️ Đặt Món
- Xem thực đơn theo danh mục với sticky header
- Chọn size / topping / tùy chọn bổ sung
- Giỏ hàng thông minh (cảnh báo khi đặt từ nhiều nhà hàng)
- Áp dụng mã giảm giá / voucher

### 🚀 Theo Dõi Đơn Hàng
- Thanh tiến trình đơn hàng (5 bước)
- Bản đồ Google Maps theo dõi vị trí shipper realtime
- Vẽ đường đi từ nhà hàng đến địa chỉ giao
- Thông tin shipper (tên, SĐT, biển số, rating)

### 💬 Tiện Ích Khác
- Chat với shipper (Firebase Realtime Database)
- Push notification (FCM) khi cập nhật đơn hàng
- Lịch sử đơn hàng + đặt lại nhanh
- Đánh giá nhà hàng & shipper sau khi nhận hàng
- Quản lý nhiều địa chỉ giao hàng

---

## 🛠️ Công Nghệ Sử Dụng

### Firebase
| Dịch vụ | Mục đích |
|---------|----------|
| Firebase Authentication | Đăng nhập OTP + Google |
| Cloud Firestore | Database chính (nhà hàng, đơn hàng, user) |
| Firebase Realtime Database | Chat realtime giữa khách & shipper |
| Firebase Storage | Lưu ảnh avatar, ảnh đánh giá |
| Firebase Cloud Messaging | Push notification |

### Third-party Libraries
| Thư viện | Version | Mục đích |
|----------|---------|----------|
| Glide | 4.16.0 | Tải và cache hình ảnh |
| Google Maps SDK | 18.2.0 | Bản đồ + tracking shipper |
| Google Places API | 3.3.0 | Tìm kiếm và tự động hoàn thiện địa chỉ |
| Material Design 3 | 1.10.0 | UI Components |
| Facebook Shimmer | 0.5.0 | Skeleton loading |
| MoMo SDK | latest | Thanh toán MoMo |
| ZaloPay SDK | latest | Thanh toán ZaloPay |

---

## 📁 Cấu Trúc Project

```
app/
├── src/main/
│   ├── java/com/fooddelivery/client/
│   │   │
│   │   ├── data/                          # Tầng dữ liệu
│   │   │   ├── model/                     # Java Model classes
│   │   │   │   ├── User.java              # Thông tin người dùng
│   │   │   │   ├── Restaurant.java        # Thông tin nhà hàng
│   │   │   │   ├── MenuItem.java          # Món ăn trong menu
│   │   │   │   ├── Order.java             # Đơn hàng
│   │   │   │   ├── CartItem.java          # Item trong giỏ hàng
│   │   │   │   ├── Address.java           # Địa chỉ giao hàng
│   │   │   │   ├── Shipper.java           # Thông tin shipper
│   │   │   │   ├── Review.java            # Đánh giá
│   │   │   │   └── Promotion.java         # Khuyến mãi / voucher
│   │   │   │
│   │   │   ├── repository/                # Xử lý logic truy vấn Firebase
│   │   │   │   ├── AuthRepository.java    # Đăng nhập / đăng ký
│   │   │   │   ├── RestaurantRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── ChatRepository.java
│   │   │   │
│   │   │   └── remote/                    # Firebase helpers
│   │   │       ├── FirestoreHelper.java   # CRUD Firestore dùng chung
│   │   │       ├── StorageHelper.java     # Upload / download ảnh
│   │   │       └── RealtimeDbHelper.java  # Firebase Realtime DB (chat)
│   │   │
│   │   ├── ui/                            # Tầng giao diện
│   │   │   ├── auth/                      # Xác thực
│   │   │   │   ├── SplashActivity.java
│   │   │   │   ├── OnboardingActivity.java
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── RegisterActivity.java
│   │   │   │   └── OtpVerifyActivity.java
│   │   │   │
│   │   │   ├── main/                      # Màn hình chính sau đăng nhập
│   │   │   │   └── MainActivity.java      # BottomNavigationView container
│   │   │   │
│   │   │   ├── home/                      # Tab Trang chủ
│   │   │   │   ├── HomeFragment.java
│   │   │   │   ├── RestaurantListFragment.java
│   │   │   │   ├── SearchActivity.java
│   │   │   │   └── adapter/
│   │   │   │       ├── BannerAdapter.java
│   │   │   │       ├── CategoryAdapter.java
│   │   │   │       └── RestaurantAdapter.java
│   │   │   │
│   │   │   ├── restaurant/                # Chi tiết nhà hàng
│   │   │   │   ├── RestaurantDetailActivity.java
│   │   │   │   ├── MenuItemDetailBottomSheet.java
│   │   │   │   └── adapter/
│   │   │   │       ├── MenuSectionAdapter.java
│   │   │   │       └── MenuItemAdapter.java
│   │   │   │
│   │   │   ├── cart/                      # Giỏ hàng
│   │   │   │   ├── CartFragment.java
│   │   │   │   ├── CartManager.java       # Singleton quản lý giỏ hàng
│   │   │   │   └── adapter/
│   │   │   │       └── CartItemAdapter.java
│   │   │   │
│   │   │   ├── order/                     # Đặt hàng & theo dõi
│   │   │   │   ├── CheckoutActivity.java
│   │   │   │   ├── OrderSuccessActivity.java
│   │   │   │   ├── OrderTrackingActivity.java
│   │   │   │   ├── OrderDetailActivity.java
│   │   │   │   └── OrderHistoryFragment.java
│   │   │   │
│   │   │   ├── chat/                      # Chat với shipper
│   │   │   │   ├── ChatActivity.java
│   │   │   │   └── adapter/
│   │   │   │       └── MessageAdapter.java
│   │   │   │
│   │   │   ├── profile/                   # Hồ sơ cá nhân
│   │   │   │   ├── ProfileFragment.java
│   │   │   │   ├── EditProfileActivity.java
│   │   │   │   ├── AddressManagerActivity.java
│   │   │   │   ├── AddEditAddressActivity.java
│   │   │   │   └── ReviewActivity.java
│   │   │   │
│   │   │   └── common/                    # Dùng chung
│   │   │       ├── BaseActivity.java      # Base với loading, permission
│   │   │       ├── BaseFragment.java
│   │   │       ├── LoadingDialog.java
│   │   │       └── EmptyStateView.java
│   │   │
│   │   ├── viewmodel/                     # MVVM ViewModels
│   │   │   ├── HomeViewModel.java
│   │   │   ├── RestaurantViewModel.java
│   │   │   ├── CartViewModel.java
│   │   │   ├── OrderViewModel.java
│   │   │   └── ProfileViewModel.java
│   │   │
│   │   ├── service/                       # Background services
│   │   │   ├── MyFirebaseMessagingService.java  # Nhận FCM notification
│   │   │   └── LocationService.java             # Cập nhật vị trí
│   │   │
│   │   └── utils/                         # Tiện ích
│   │       ├── Constants.java             # Hằng số toàn app
│   │       ├── SharedPrefManager.java     # Lưu session, cart data
│   │       ├── CurrencyUtils.java         # Format tiền VND
│   │       ├── DateTimeUtils.java         # Format ngày giờ
│   │       ├── ImageUtils.java            # Compress, resize ảnh
│   │       ├── NetworkUtils.java          # Kiểm tra kết nối mạng
│   │       ├── DistanceUtils.java         # Tính khoảng cách Haversine
│   │       └── ValidationUtils.java       # Validate form input
│   │
│   └── res/
│       ├── layout/                        # Tất cả file XML layout
│       │   ├── activity_*.xml
│       │   ├── fragment_*.xml
│       │   └── item_*.xml
│       ├── drawable/                      # Icons, shapes, backgrounds
│       ├── anim/                          # Animation XML
│       ├── values/
│       │   ├── colors.xml                 # Bảng màu (#FF6B35, #1A237E...)
│       │   ├── strings.xml                # Tất cả chuỗi văn bản
│       │   ├── styles.xml                 # Styles & themes
│       │   └── dimens.xml                 # Kích thước
│       └── font/                          # File font tùy chỉnh
│
├── google-services.json                   # ⚠️ Firebase config (không commit)
└── build.gradle                           # Dependencies
```

---

## 🚀 Hướng Dẫn Cài Đặt

### Yêu cầu
- Android Studio **Hedgehog** (2023.1.1) trở lên
- JDK 17
- Gradle 8.x
- Thiết bị / emulator Android API 24+

### Các bước

```bash
# 1. Clone repository
git clone https://github.com/your-org/food-delivery-client.git
cd food-delivery-client

# 2. Mở bằng Android Studio
# File → Open → chọn thư mục vừa clone

# 3. Thêm file cấu hình Firebase
# Tải google-services.json từ Firebase Console
# Đặt vào thư mục: app/google-services.json

# 4. Thêm API keys vào local.properties
echo "MAPS_API_KEY=your_google_maps_key" >> local.properties
echo "MOMO_APP_ID=your_momo_id" >> local.properties

# 5. Sync Gradle và chạy
# Nhấn "Sync Now" trong Android Studio
# Chọn thiết bị → Run (Shift + F10)
```

---

## 🔥 Cấu Hình Firebase

### Firestore Collections cần tạo
```
users/           → thông tin người dùng
restaurants/     → danh sách nhà hàng
  └── menuItems/ → subcollection menu của từng nhà hàng
orders/          → đơn hàng
shippers/        → thông tin shipper
reviews/         → đánh giá
promotions/      → mã giảm giá
notifications/   → thông báo
```

### Firestore Indexes cần tạo
```
orders:   userId ASC, createdAt DESC
orders:   restaurantId ASC, createdAt DESC
orders:   status ASC, createdAt DESC
restaurants: isActive ASC, rating DESC
```

### Firebase Authentication
Bật các provider sau trong Firebase Console:
- ✅ Phone Authentication
- ✅ Google Sign-In

---

## 🏗️ Kiến Trúc Ứng Dụng

```
┌─────────────────────────────────────────┐
│              UI Layer                    │
│   Activity / Fragment / Adapter          │
│   (Quan sát LiveData, gọi ViewModel)     │
└───────────────┬─────────────────────────┘
                │ observe / call
┌───────────────▼─────────────────────────┐
│           ViewModel Layer                │
│   (Xử lý UI logic, giữ trạng thái)      │
│   HomeViewModel / OrderViewModel...      │
└───────────────┬─────────────────────────┘
                │ call
┌───────────────▼─────────────────────────┐
│          Repository Layer                │
│   (Tổng hợp dữ liệu từ Firebase)        │
│   RestaurantRepo / OrderRepo...          │
└───────────────┬─────────────────────────┘
                │ query
┌───────────────▼─────────────────────────┐
│         Firebase Services               │
│   Firestore │ RealtimeDB │ Storage      │
│   Auth      │ FCM        │ Maps API     │
└─────────────────────────────────────────┘
```

**Luồng dữ liệu một chiều:**
`User Action → ViewModel → Repository → Firebase → LiveData → UI Update`

---

## 📲 Màn Hình Chính

| Màn hình | File | Mô tả |
|----------|------|-------|
| Splash | `SplashActivity` | Logo animation, kiểm tra session |
| Onboarding | `OnboardingActivity` | 3 slide giới thiệu, ViewPager2 |
| Đăng nhập | `LoginActivity` | OTP / Google Sign-in |
| OTP | `OtpVerifyActivity` | 6 ô nhập, đếm ngược 60s |
| Trang chủ | `HomeFragment` | Banner, danh mục, nhà hàng |
| Chi tiết NHÀ HÀNG | `RestaurantDetailActivity` | Menu theo danh mục, sticky header |
| Giỏ hàng | `CartFragment` | Danh sách, voucher, tóm tắt |
| Thanh toán | `CheckoutActivity` | Địa chỉ, PT thanh toán |
| Theo dõi đơn | `OrderTrackingActivity` | Maps + shipper realtime |
| Chat | `ChatActivity` | Nhắn tin với shipper |
| Hồ sơ | `ProfileFragment` | Thông tin, địa chỉ, lịch sử |

---

## 🔄 Luồng Đặt Hàng

```
Chọn nhà hàng
      ↓
Thêm món vào giỏ  →  [CartManager.addItem()]
      ↓
Xem giỏ hàng      →  Nhập voucher, ghi chú
      ↓
Checkout           →  Chọn địa chỉ + thanh toán
      ↓
Tạo Order          →  [Firestore transaction]
      ↓
Chờ xác nhận       →  [Realtime listener]
      ↓
Đang chuẩn bị      →  Notification FCM
      ↓
Shipper nhận hàng  →  Hiển thị thông tin shipper
      ↓
Đang giao          →  Tracking bản đồ realtime
      ↓
Đã giao            →  Màn hình đánh giá
```

---

## 🔑 Biến Môi Trường

Tạo / cập nhật file `local.properties` (không commit lên git):

```properties
# Google Maps
MAPS_API_KEY=AIzaSy...

# MoMo Payment
MOMO_APP_ID=your_app_id
MOMO_APP_SCHEME=your_scheme

# ZaloPay
ZALOPAY_APP_ID=your_app_id

# VNPay
VNPAY_TMN_CODE=your_code
VNPAY_HASH_SECRET=your_secret
```

---

## 🎨 Design System

| Token | Giá trị | Dùng cho |
|-------|---------|----------|
| `colorPrimary` | `#FF6B35` | Nút chính, accent, icon active |
| `colorPrimaryDark` | `#E55A25` | Trạng thái pressed |
| `colorSecondary` | `#1A237E` | Header, badge thông tin |
| `colorBackground` | `#F8F9FA` | Nền màn hình |
| `colorSurface` | `#FFFFFF` | Card, bottom sheet |
| `colorSuccess` | `#4CAF50` | Đơn thành công |
| `colorError` | `#F44336` | Lỗi, hủy đơn |

---

## 📝 Quy Tắc Code

- Comment tiếng Việt cho logic nghiệp vụ phức tạp
- Xử lý `null` trước khi sử dụng object từ Firestore
- Luôn hiển thị loading khi gọi Firebase
- Hiển thị `EmptyStateView` khi danh sách rỗng
- Dùng `Log.d(TAG, ...)` để debug, không log thông tin nhạy cảm

---

*FoodDelivery Client App · Java Android · Phiên bản 1.0.0*