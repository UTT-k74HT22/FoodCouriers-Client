# Tài liệu Chức năng Lấy Vị trí Điểm Giao Hàng - Client

## 1. Tổng quan

### Mục đích
Cho phép người dùng nhập/chọn địa chỉ giao hàng với tọa độ chính xác, phục vụ tính khoảng cách và phí vận chuyển.

### Kiến trúc High-Level

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT APP                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │ AddressForm  │───▶│ LocationHelper│───▶│  OSMDroid    │     │
│  │   Activity   │    │               │    │   MapView    │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
│         │                   │                                    │
│         │                   ▼                                    │
│         │         ┌──────────────┐    ┌──────────────┐        │
│         │         │Android       │    │DistanceUtils │        │
│         │         │Geocoder     │    │(Haversine)   │        │
│         │         └──────────────┘    └──────────────┘        │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────┐                                               │
│  │AddressClient │───────▶ Supabase REST API                    │
│  └──────────────┘         (user_addresses table)               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Thành phần chính (Code Breakdown)

### 2.1 UI Layer

| File | Mô tả |
|------|-------|
| `AddressFormActivity.java` | Activity chính, xử lý UI, events |
| `activity_address_form.xml` | Layout XML với AutoCompleteTextView + MapView |
| `AddressSuggestionAdapter.java` | Adapter cho dropdown gợi ý địa chỉ |

### 2.2 Utility Layer

| File | Mục đích |
|------|----------|
| `LocationHelper.java` | Lấy vị trí GPS hiện tại |
| `DistanceUtils.java` | Tính khoảng cách Haversine |

### 2.3 Data Layer

| File | Mục đích |
|------|----------|
| `AddressClient.java` | Gọi Supabase API |
| `Address.java` | Model data |
| `CartRepository.java` | Lấy restaurant có lat/long |

---

## 3. Models & Callbacks

### 3.1 Address Model

```java
public class Address {
    private String id;
    private String userId;
    private String label;          // "Nhà", "Công ty", "Khác"
    private String fullAddress;     // Địa chỉ đầy đủ
    private String district;       // Quận/Huyện
    private String city;           // Thành phố
    private Boolean isDefault;     // Địa chỉ mặc định
    private Double latitude;       // Vĩ độ
    private Double longitude;      // Kinh độ
}
```

### 3.2 LocationHelper Callbacks

```java
public interface LocationListener {
    void onLocationReceived(LocationData locationData); // Thành công
    void onLocationError(String error);                 // Lỗi
}

public static class LocationData {
    public final double latitude;
    public final double longitude;
    public final float accuracy;
    public final String address;
}
```

### 3.3 AddressClient Callback

```java
public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String error);
}
```

---

## 4. Flow xử lý

### 4.1 Flow 1: Tạo địa chỉ mới

```
┌────────────────┐
│ User mở        │
│ AddressForm   │
└───────┬────────┘
        │
        ▼
┌────────────────┐     ┌─────────────────┐
│ Nhập địa chỉ   │────▶│ Geocoder tìm   │
│ vào text field │     │ kiếm (≥3 ký tự) │
└───────┬────────┘     └────────┬────────┘
        │                       │
        ▼                       ▼
┌────────────────┐     ┌─────────────────┐
│ Click icon     │     │ Hiển thị gợi ý  │
│ kính lúp       │     │ dropdown        │
└───────┬────────┘     └────────┬────────┘
        │                       │
        ▼                       ▼
┌────────────────┐     ┌─────────────────┐
│ Gọi API tìm    │     │ User chọn 1    │
│ kiếm           │     │ gợi ý          │
└───────┬────────┘     └───────┬────────┘
        │                     │
        ▼                     ▼
┌────────────────┐     ┌─────────────────┐
│ Hiển thị map   │◀────│ Update map     │
│ với marker     │     │ + tọa độ        │
└────────────────┘     └─────────────────┘
        │
        ▼
┌────────────────┐
│ Click Save     │
└───────┬────────┘
        │
        ▼
┌────────────────┐     ┌─────────────────┐
│ Gọi API create │────▶│ Lưu vào        │
│ với lat/lon    │     │ Supabase        │
└───────┬────────┘     └─────────────────┘
```

### 4.2 Flow 2: Permissions

```
┌────────────────┐
│ App yêu cầu   │
│ ACCESS_FINE_   │
│ LOCATION       │
└───────┬────────┘
        │
   ┌────┴────┐
   │ User    │
   │ Allow?  │
   └────┬────┘
     Yes │ No
     ┌───┘    └──▶ Hiển thị lỗi, dùng tay
     ▼
┌──────────────┐
│ Lấy GPS     │
│ location    │
└──────┬───────┘
       │
   ┌───┴───┐
   │ Success│ Error
   └───┬───┘  └──▶ Thông báo lỗi
       ▼
┌──────────────┐
│ Hiển thị    │
│ địa chỉ +   │
│ map         │
└──────────────┘
```

### 4.3 Flow 3: Tính khoảng cách (Checkout)

```
┌────────────────┐
│ Checkout       │
│ Activity       │
└───────┬────────┘
        │
        ▼
┌────────────────┐
│ Lấy delivery   │
│ address từ DB  │
└───────┬────────┘
        │
        ▼
┌────────────────┐
│ Lấy restaurant │
│ lat/lon từ    │
│ CartRepository │
└───────┬────────┘
        │
        ▼
┌────────────────┐     ┌─────────────────┐
│ DistanceUtils  │────▶│ Tính Haversine  │
│ calculate      │     │ (đường thẳng)   │
│ DistanceKm     │     └─────────────────┘
└───────┬────────┘
        │
        ▼
┌────────────────┐
│ Hiển thị      │
│ khoảng cách  │
│ + tính phí    │
└────────────────┘
```

---

## 5. Dependencies

### 5.1 Gradle Dependencies

```kotlin
// OSMDroid - Map
implementation("org.osmdroid:osmdroid-android:6.1.18")

// Google Play Services Location
implementation(libs.playServicesLocation)

// Supabase (đã có)
implementation(libs.supabase)
```

### 5.2 Android Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 5.3 Supabase Database Schema

```sql
-- Bảng user_addresses (đã có)
CREATE TABLE user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id),
    label TEXT,
    full_address TEXT,
    district TEXT,
    city TEXT,
    is_default BOOLEAN DEFAULT false,
    latitude DOUBLE PRECISION,  -- Thêm mới
    longitude DOUBLE PRECISION, -- Thêm mới
    created_at TIMESTAMP DEFAULT NOW()
);

-- Bảng restaurants (đã có)
CREATE TABLE restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT,
    address TEXT,
    latitude DOUBLE PRECISION,  -- Thêm mới
    longitude DOUBLE PRECISION, -- Thêm mới
    ...
);
```

---

## 6. Ví dụ sử dụng Code

### 6.1 Tạo địa chỉ mới

```java
// Trong AddressFormActivity.java

// 1. Khởi tạo
sessionManager = SessionManager.getInstance(this);
suggestionAdapter = new AddressSuggestionAdapter(this, new ArrayList<>());
etAddress.setAdapter(suggestionAdapter);
etAddress.setThreshold(1);

// 2. Listen text change -> gợi ý
etAddress.addTextChangedListener(new TextWatcher() {
    @Override
    public void afterTextChanged(Editable s) {
        if (s.toString().trim().length() >= 3) {
            searchSuggestions(s.toString().trim()); // Gọi Geocoder
        }
    }
});

// 3. Search button
binding.btnSearchAddress.setOnClickListener(v -> {
    String query = etAddress.getText().toString();
    searchAddress(query); // Geocoder + update map
});

// 4. Save
private void saveAddress() {
    AddressClient.getInstance().createAddressWithCoordinates(
        userId, label, fullAddress, district, city, isDefault,
        currentLatitude, currentLongitude, // Lưu tọa độ
        new ApiCallback<Address>() {
            @Override
            public void onSuccess(Address result) {
                // Success
            }
        }
    );
}
```

### 6.2 Tính khoảng cách

```java
// Trong CheckoutViewModel.java

// Lấy từ CartRepository (đã có restaurant lat/lon)
for (CartRestaurantGroup group : state.getRestaurantGroups()) {
    Double restLat = group.getRestaurantLatitude();
    Double restLon = group.getRestaurantLongitude();
    
    if (restLat != null && restLon != null) {
        double distance = DistanceUtils.calculateDistanceKm(
            restLat, restLon, 
            deliveryLat, deliveryLon
        );
        totalDistance += distance;
    }
}

// Format hiển thị
String distanceText = DistanceUtils.formatDistance(totalDistance);
// Output: "2.5km" hoặc "500m"
```

### 6.3 DistanceUtils

```java
// DistanceUtils.java
public class DistanceUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    public static double calculateDistanceKm(
        double lat1, double lon1, 
        double lat2, double lon2
    ) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) 
                   * Math.cos(Math.toRadians(lat2))
                   * Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS_KM * c;
    }
    
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0fm", distanceKm * 1000);
        }
        return String.format("%.1fkm", distanceKm);
    }
}
```

---

## 7. Best Practices, Limitations & Edge Cases

| Danh mục | Nội dung |
|----------|----------|
| **Best Practices** | |
| ✓ Debounce | Đợi 500ms sau khi gõ xong mới gọi API tránh spam |
| ✓ Threshold | Tối thiểu 3 ký tự mới gọi Geocoder |
| ✓ Fallback | Nếu không lấy được GPS dùng default lat/lon (HN: 21.0285, 105.8542) |
| ✓ UX | Hiển thị progress khi đang tìm kiếm |
| **Limitations** | |
| ✗ Geocoder | Android Geocoder phụ thuộc thiết bị, không phải lúc nào cũng có kết quả |
| ✗ Distance | Chỉ tính đường thẳng (Haversine), không phải đường thực tế |
| ✗ API Limit | Nominatim (OSM) giới hạn 1 request/giây |
| **Edge Cases** | |
| ⚠️ Không GPS | User từ chối cấp quyền → dùng nhập tay |
| ⚠️ Không có kết quả geocoder | Hiển thị lỗi, vẫn cho lưu địa chỉ text |
| ⚠️ Địa chỉ không tồn tại | Cho phép lưu nhưng lat/lon = null |
| ⚠️ Map không load | OSMDroid cần internet, có fallback offline |

---

## 8. Glossary (Thuật ngữ)

| Thuật ngữ | Giải thích |
|-----------|------------|
| **Geocoding** | Chuyển địa chỉ text → tọa độ lat/lon |
| **Reverse Geocoding** | Chuyển tọa độ lat/lon → địa chỉ text |
| **Haversine** | Công thức tính khoảng cách đường thẳng giữa 2 điểm |
| **Debounce** | Kỹ thuật đợi sau khi user ngừng gõ mới xử lý |
| **Marker** | Điểm đánh dấu trên bản đồ |
| **Draggable** | Có thể kéo thả |
| **OSMDroid** | Thư viện map Android dùng OpenStreetMap |
| **Supabase** | Backend-as-a-Service, lưu trữ database |

---

## 9. Cải tiến đề xuất

### 9.1 Tính khoảng cách thực tế (Routing)

| Giải pháp | Mô tả | Phí |
|-----------|-------|-----|
| **OSRM** | Gọi API OSRM lấy đường đi thực tế | Miễn phí, giới hạn |
| **Google Directions** | Dùng Google Routing API | $5-10/1000 requests |
| **Mapbox** | Mapbox Directions API | Miễn phí 100k/tháng |

**Code example:**
```java
// Gọi OSRM API
String url = "http://router.project-osrm.org/route/v1/driving/" 
    + lon1 + "," + lat1 + ";" + lon2 + "," + lat2 
    + "?overview=false";
// Response: {"routes": [{"distance": 3500, "duration": 600}]}
```

### 9.2 Tự động phát hiện vị trí hiện tại

Thêm button "Dùng vị trí hiện tại" → gọi LocationHelper → tự điền địa chỉ + map.

### 9.3 Validate địa chỉ

- Kiểm tra lat/lon hợp lệ (trong phạm vi VN)
- Thông báo nếu địa chỉ ngoài vùng phục vụ

### 9.4 Cache gợi ý

Lưu cache kết quả geocoder để không phải gọi lại cho cùng địa chỉ.

---

## 10. Tóm tắt

| Thành phần | Vai trò |
|------------|---------|
| `AddressFormActivity` | UI, xử lý events |
| `AddressSuggestionAdapter` | Dropdown gợi ý địa chỉ |
| `LocationHelper` | Lấy GPS |
| `DistanceUtils` | Tính khoảng cách |
| `AddressClient` | Gọi Supabase API |

**Luồng chính:**
1. User nhập địa chỉ → Geocoder → Hiển thị gợi ý
2. Chọn gợi ý → Update map + marker
3. Drag marker → Chỉnh vị trí
4. Save → Lưu vào Supabase với lat/lon
5. Checkout → Tính khoảng cách → Tính phí ship