# Social Auth Google Flow

## Mục tiêu

Tài liệu này mô tả trạng thái hiện tại của luồng đăng nhập Google trên app client Android, cách dữ liệu đi qua từng lớp, phần cấu hình Supabase bắt buộc, cơ chế debug chuẩn, và các điểm tái sử dụng khi mở rộng sang Facebook.

Phạm vi hiện tại:

- Google OAuth: đã hoạt động end-to-end
- Facebook OAuth: mới ở mức khung `provider`, chưa tích hợp provider thật
- App stack: Java Android + XML + Supabase Auth + PostgREST

---

## Trạng thái hiện tại

Đã setup xong các phần sau:

- Mở Google OAuth từ app qua Supabase `/auth/v1/authorize`
- Deep link callback về app qua `com.utt.foodcouriers.client://auth/callback`
- Parse `access_token`, `refresh_token`, `expires_in` từ callback fragment
- Bootstrap session với Supabase `/auth/v1/user`
- Tự tạo `public.users` profile nếu user đăng nhập social lần đầu
- Lưu `avatar_url` từ Google metadata vào `public.users`
- Backfill avatar nếu user social đã có profile cũ nhưng `avatar_url` đang trống
- Logging chuẩn theo format `Step X: mô tả | chi tiết`

---

## Cấu trúc thư mục

Các file chính liên quan đến flow:

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/utt/foodcouriers_client/
│   ├── FoodCouriersClientApp.java
│   ├── data/
│   │   ├── auth/
│   │   │   ├── SocialAuthManager.java
│   │   │   ├── SocialAuthProvider.java
│   │   │   └── SocialAuthResult.java
│   │   ├── remote/
│   │   │   ├── AuthClient.java
│   │   │   ├── BaseSupabaseClient.java
│   │   │   └── SupabaseConfig.java
│   │   ├── repository/
│   │   │   ├── AuthRepository.java
│   │   │   └── SocialAuthRepository.java
│   │   └── model/
│   │       └── UserProfile.java
│   ├── ui/
│   │   └── auth/
│   │       ├── LoginActivity.java
│   │       ├── RegisterActivity.java
│   │       ├── ResetPasswordActivity.java
│   │       └── SocialAuthCallbackActivity.java
│   └── utils/
│       ├── SessionManager.java
│       ├── SessionStore.java
│       └── ToastBanner.java
└── res-layouts/auth/layout/
    ├── activity_login.xml
    ├── activity_register.xml
    └── activity_social_auth_callback.xml
```

Vai trò từng nhóm:

- `data/auth`: điều phối social auth flow, parse callback, giữ context provider
- `data/remote/AuthClient`: gọi Supabase Auth REST + tạo/fetch profile `public.users`
- `data/repository/SocialAuthRepository`: cầu nối UI -> manager
- `ui/auth/*`: màn hình login/register/callback
- `utils/SessionManager`, `SessionStore`: lưu session dùng cho app và social bootstrap

---

## Cấu hình Supabase bắt buộc

### 1. Auth Provider

Trong Supabase Dashboard:

- `Authentication -> Providers -> Google`: bật provider
- Callback URL của Google provider phải trỏ về:

```text
https://<project-ref>.supabase.co/auth/v1/callback
```

### 2. Redirect URL của app

Trong `Authentication -> URL Configuration` phải có:

```text
com.utt.foodcouriers.client://auth/callback
```

### 3. RLS cho bảng `public.users`

Vì app tự tạo profile `public.users` sau khi Google login thành công, bảng `users` phải có policy insert cho chính user đó.

Policy bắt buộc:

```sql
create policy "users_self_insert"
on public.users
for insert
to authenticated
with check (auth_id = auth.uid());
```

Nếu thiếu policy này, login Google vẫn thành công nhưng bootstrap profile sẽ fail với lỗi:

```text
new row violates row-level security policy for table "users"
```

---

## Android Deep Link

Deep link callback được khai báo trong [AndroidManifest.xml](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/AndroidManifest.xml:24):

```xml
<activity
    android:name=".ui.auth.SocialAuthCallbackActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:host="auth"
            android:pathPrefix="/callback"
            android:scheme="com.utt.foodcouriers.client" />
    </intent-filter>
</activity>
```

URL callback đầy đủ:

```text
com.utt.foodcouriers.client://auth/callback
```

---

## Kiến trúc luồng

### Lớp UI

- [LoginActivity.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/auth/LoginActivity.java:184)
- [RegisterActivity.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/ui/auth/RegisterActivity.java:183)

Nhiệm vụ:

- nhận click từ người dùng
- chọn `SocialAuthProvider.GOOGLE`
- gọi `SocialAuthRepository.startAuth(...)`

### Lớp orchestration

- [SocialAuthRepository.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/repository/SocialAuthRepository.java:14)
- [SocialAuthManager.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/auth/SocialAuthManager.java:24)

Nhiệm vụ:

- build authorize URL của Supabase
- mở browser
- nhận deep link callback
- parse token từ URL fragment
- lưu social session tạm
- bootstrap profile app-level

### Lớp remote / bootstrap

- [AuthClient.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/remote/AuthClient.java:22)

Nhiệm vụ:

- set access token / refresh token
- gọi `/auth/v1/user`
- đọc `user_metadata`
- tìm profile trong `public.users`
- nếu chưa có thì insert profile mới
- nếu có nhưng thiếu `avatar_url` thì backfill avatar

---

## End-to-end flow

### A. User đã từng tồn tại trong `public.users`

1. User nhấn `Continue with Google`
2. App mở:

```text
GET /auth/v1/authorize?provider=google&redirect_to=com.utt.foodcouriers.client://auth/callback
```

3. Supabase chuyển sang Google consent screen
4. Google trả về Supabase callback
5. Supabase redirect lại app bằng deep link kèm fragment:

```text
#access_token=...
#refresh_token=...
#expires_in=...
```

6. `SocialAuthManager` parse callback
7. `AuthClient` gọi:

```text
GET /auth/v1/user
```

8. App đọc `auth.users` info và tìm row trong `public.users`
9. Nếu profile đã tồn tại:
   app dùng profile đó để tạo session app
10. App vào `MainActivity`

### B. User Google đăng nhập lần đầu

1. Các bước 1 -> 7 giống flow trên
2. `GET /auth/v1/user` trả metadata từ Google:

```json
{
  "email": "...",
  "full_name": "...",
  "avatar_url": "https://..."
}
```

3. App không tìm thấy row trong `public.users`
4. `AuthClient.createUserProfile(...)` insert row mới vào `public.users`
5. Dữ liệu được lưu:

```text
auth_id
full_name
email
phone
avatar_url
role=customer
is_active=true
```

6. Session app được lưu trong `SessionManager`
7. App vào `MainActivity`

### C. User social cũ nhưng profile thiếu avatar

1. OAuth thành công
2. App load `auth.users`
3. App load `public.users`
4. Nếu `public.users.avatar_url` trống nhưng `auth.user_metadata.avatar_url` có giá trị:
   app gọi `updateProfile(...)`
5. Avatar được backfill vào DB

---

## Cơ chế avatar Google

Nguồn avatar hiện tại:

- Ưu tiên `user_metadata.avatar_url`
- Không dùng `picture` trực tiếp trong code hiện tại

Các điểm xử lý:

- Parse metadata: [AuthClient.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/remote/AuthClient.java:641)
- Resolve avatar: [AuthClient.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/remote/AuthClient.java:586)
- Insert avatar khi tạo profile: [AuthClient.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/remote/AuthClient.java:360)
- Backfill avatar nếu profile cũ thiếu ảnh: [AuthClient.java](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/app/src/main/java/com/utt/foodcouriers_client/data/remote/AuthClient.java:416)

Lưu ý:

- Avatar Google là URL external, không upload vào Supabase Storage
- Nếu sau này cần đồng bộ ảnh bền vững hơn, có thể tải ảnh về và upload sang bucket `avatars`

---

## Logging chuẩn

### Login/Register trigger

- `Step 1: User requested social auth | provider=google`
- `Step 2: Social auth launch delegated to manager | provider=google`

### OAuth manager

- `Step 1: Preparing Supabase OAuth launch | provider=google`
- `Step 2: Opening browser for Supabase OAuth | redirectTo=..., authorizeUrl=...`
- `Step 3: Received OAuth callback | uri=...`
- `Step 4: Supabase callback contained session payload | provider=google, accessToken=present, refreshToken=present`
- `Step 4: Supabase callback did not complete login | ...`
- `Step 6: Bootstrap completed and session saved | provider=google, userId=..., email=...`

### Auth bootstrap / profile creation

- `Step 5: Loaded auth user from Supabase | authId=..., email=..., fullName=..., avatarUrl=...`
- `Step 6: Creating missing public.users profile | authId=..., email=..., fullName=..., phone=..., avatarUrl=...`
- `Step 6: Created public.users profile successfully | userId=..., avatarUrl=...`
- `Step 6: Backfilling avatar from auth metadata | userId=..., avatarUrl=...`
- `Step 6: Avatar backfill completed | userId=..., avatarUrl=...`

Quy ước debug:

- `Step 1-4`: OAuth transport
- `Step 5`: đọc auth user từ Supabase Auth
- `Step 6`: app-level bootstrap vào `public.users` và session app

---

## Debug checklist chuẩn

### Trường hợp 1: bấm Google không mở được browser

Kiểm tra:

- log `Step 1` và `Step 2`
- `SupabaseConfig.SUPABASE_URL`
- app có internet permission

### Trường hợp 2: consent xong nhưng không quay về app

Kiểm tra:

- `AndroidManifest` có `intent-filter` đúng scheme/host/path
- Supabase Redirect URL có `com.utt.foodcouriers.client://auth/callback`
- browser thực sự redirect đúng deep link

### Trường hợp 3: callback về app nhưng không có token

Kiểm tra:

- log `Step 3`
- trong URI fragment có `access_token` và `refresh_token` hay không
- Supabase Auth logs ở `/auth/v1/callback`

### Trường hợp 4: login thành công nhưng bootstrap fail

Kiểm tra:

- log `Step 5` và `Step 6`
- RLS policy bảng `public.users`
- đặc biệt policy:

```sql
with check (auth_id = auth.uid())
```

### Trường hợp 5: login được nhưng avatar không có

Kiểm tra:

- `Step 5` có `avatarUrl=...` hay không
- `Step 6` insert/backfill có chạy không
- `public.users.avatar_url` trong DB có giá trị chưa

Nếu `Step 5` đã có `avatarUrl` mà DB vẫn trống:

- kiểm tra payload insert/update
- kiểm tra policy `UPDATE` của `public.users`

---

## Khác biệt giữa Google OAuth hiện tại và Facebook sau này

Phần có thể tái sử dụng gần như nguyên vẹn:

- `SocialAuthManager`
- `SocialAuthRepository`
- `SocialAuthCallbackActivity`
- `AuthClient.bootstrapSocialSession(...)`
- `createUserProfile(...)`
- `maybeBackfillSocialAvatar(...)`
- `SessionStore` và `SessionManager`

Phần cần thay đổi cho Facebook:

1. Bật provider Facebook trong Supabase
2. Cấu hình Facebook App callback URL
3. Cho phép `provider != GOOGLE` trong `SocialAuthManager.launchProvider(...)`
4. Kiểm tra Facebook metadata map có dùng `avatar_url` hay tên field khác
5. Viết test log tương tự Google

Khuyến nghị:

- giữ chung một callback activity
- giữ chung một cơ chế bootstrap profile
- chỉ tách logic mapping metadata nếu provider-specific field khác nhau

---

## Checklist mở rộng sang Facebook

1. Thêm cấu hình provider Facebook trên Supabase
2. Thêm App ID / App Secret trong dashboard
3. Kiểm tra Redirect URL vẫn dùng deep link hiện tại
4. Bật nhánh `FACEBOOK` trong `SocialAuthManager`
5. Kiểm tra metadata thực tế từ `/auth/v1/user`
6. Nếu avatar field không phải `avatar_url`, bổ sung normalize logic trong `AuthClient.AuthUser`
7. Kiểm tra `public.users` insert/update policy vẫn đủ
8. Chuẩn hóa log `Step 1 -> Step 6` tương tự Google

---

## File liên quan trong repo Supabase

Schema và migration hiện có:

- [001_initial_schema.sql](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/001_initial_schema.sql:1)
- [003_rls_policies.sql](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/003_rls_policies.sql:1)
- [007_add_users_self_insert_policy.sql](/abs/path/C:/UTT/AppFood/FoodCouriers-Client/docs/supabase/migrations/007_add_users_self_insert_policy.sql:1)

---

## Ghi chú triển khai hiện tại

- Flow hiện tại dùng Supabase OAuth callback qua browser + deep link
- App không còn tự tạo `state` riêng
- Xác thực OAuth `state` do Supabase Auth phía server xử lý
- `public.users` là hồ sơ nghiệp vụ của app, tách biệt với `auth.users`
- Google avatar đang lưu dưới dạng URL ngoài

---

## Tóm tắt ngắn

Luồng Google login hiện tại gồm 3 lớp:

1. UI mở Supabase OAuth
2. Callback deep link trả token về app
3. Bootstrap profile app-level vào `public.users`

Để flow này chạy ổn định, 3 điều kiện quan trọng nhất là:

1. Redirect URL deep link phải đúng
2. Bảng `public.users` phải có `INSERT` policy cho chính user
3. Metadata social như `avatar_url` phải được map và lưu xuống profile app-level
