# Auth, Notification va Realtime Flow

Tai lieu nay tom tat cac luong chay chinh trong client Android: dang nhap Google qua Supabase OAuth, notification in-app, va order realtime. Muc tieu la doc nhanh de nam duoc man hinh nao goi lop nao, du lieu di dau, va vi sao realtime event van reload lai bang REST.

## 1. Google Login Qua Supabase OAuth

```mermaid
sequenceDiagram
    actor User
    participant Login as Login/Register Activity
    participant Repo as SocialAuthRepository
    participant Manager as SocialAuthManager
    participant Browser as Browser
    participant Supabase as Supabase Auth
    participant Callback as SocialAuthCallbackActivity
    participant Auth as AuthClient
    participant Session as SessionManager
    participant App as FoodCouriersClientApp

    User->>Login: Bam Continue with Google
    Login->>Repo: startAuth(context, GOOGLE)
    Repo->>Manager: launchProvider(...)
    Manager->>Manager: build redirect URI va save pending provider
    Manager->>Browser: ACTION_VIEW /auth/v1/authorize
    Browser->>Supabase: provider=google, redirect_to=app deeplink
    Supabase-->>Browser: OAuth thanh cong
    Browser-->>Callback: com.utt.foodcouriers.client://auth/callback#access_token=...
    Callback->>Repo: handleCallback(context, uri)
    Repo->>Manager: handleCallback(...)
    Manager->>Manager: parse query + fragment tokens
    Manager->>Auth: bootstrapSocialSession(access, refresh)
    Auth-->>Manager: UserProfile tu public.users
    Manager->>Session: saveSession(access, refresh, profile)
    Manager->>App: initializeRealtime(access)
    Callback-->>User: Mo MainActivity
```

Ghi nho:

- App khong dung Google Sign-In SDK truc tiep. Supabase Auth va browser xu ly OAuth.
- Callback co the tra token trong fragment sau dau `#`, nen parser phai doc ca query va fragment.
- Token OAuth moi la Supabase Auth session. App van phai bootstrap profile nghiep vu trong `public.users`.
- Realtime chi duoc khoi tao sau khi session chinh duoc luu thanh cong.

## 2. Notification In-App Va Realtime

```mermaid
sequenceDiagram
    participant App as FoodCouriersClientApp
    participant RT as SupabaseRealtimeClient
    participant Main as MainActivity
    participant Fragment as NotificationsFragment
    participant VM as NotificationViewModel
    participant Repo as NotificationRepository
    participant Client as NotificationClient
    participant DB as Supabase notifications

    App->>RT: initialize(accessToken) + connect()
    App->>RT: subscribe public:notifications user_id=eq.currentUser
    Main->>App: addNotificationRefreshListener(...)
    Main->>Repo: getNotifications(userId)
    Repo->>Client: getNotifications(userId)
    Client->>DB: REST GET notifications
    DB-->>Client: rows
    Client-->>Main: unread count source data

    Fragment->>VM: loadNotifications(context)
    VM->>Repo: getNotifications(userId)
    Repo->>Client: getNotifications(userId)
    Client->>DB: REST GET notifications
    DB-->>Fragment: list via LiveData

    DB-->>RT: INSERT/UPDATE/DELETE realtime event
    RT-->>App: listener event
    App->>Main: notifyNotificationChanged()
    Main->>Repo: reload badge
    RT-->>Fragment: listener event khi man notification dang mo
    Fragment->>VM: refreshNotifications()
```

Ghi nho:

- Co hai subscription notification:
  - `FoodCouriersClientApp`: subscription toan app de hien toast va refresh badge.
  - `NotificationsFragment`: subscription cuc bo khi dang mo danh sach, dung de reload list.
- Khi user mark read/delete, ViewModel goi REST qua repository/client, sau do reload state.
- Realtime event khong render truc tiep item moi; no chi kich hoat reload REST de UI dong bo voi backend/RLS.

## 3. Order Realtime Va Tracking

```mermaid
sequenceDiagram
    participant Orders as OrdersFragment
    participant Detail as OrderDetailActivity
    participant Tracking as OrderTrackingActivity
    participant VM as OrdersViewModel
    participant Repo as OrderRepository
    participant RT as SupabaseRealtimeClient
    participant DB as Supabase orders

    Orders->>VM: loadOrders(context, filter)
    VM->>Repo: getOrders(filter)
    Repo->>DB: REST GET orders?user_id=eq.currentUser
    DB-->>Orders: list via LiveData
    Orders->>RT: subscribe public:orders user_id=eq.currentUser

    Orders->>Detail: open with orderId
    Detail->>VM: loadOrderDetail(orderId)
    VM->>Repo: getOrderById(orderId)
    Repo->>DB: REST GET orders?id=eq.orderId select restaurants, order_items
    DB-->>Detail: detail via LiveData
    Detail->>RT: subscribe public:orders id=eq.orderId

    Detail->>Tracking: open tracking with orderId
    Tracking->>VM: loadOrderDetail(orderId)
    Tracking->>RT: subscribe public:orders id=eq.orderId

    DB-->>RT: UPDATE status/delivery_status
    RT-->>Orders: refresh list
    RT-->>Detail: refresh order detail
    RT-->>Tracking: refresh order detail va update timeline
```

Ghi nho:

- `OrdersFragment` subscribe theo `user_id` vi no quan tam moi don cua user trong danh sach.
- `OrderDetailActivity` va `OrderTrackingActivity` subscribe theo `id` vi chi quan tam mot don dang mo.
- Payload realtime tu Supabase co the khong du `restaurants` va `order_items`, nen UI luon goi lai `OrderRepository#getOrderById`.
- Timeline dung thu tu `OrderStatus` de xac dinh buoc nao da dat.

## 4. Vi Tri Cac Lop Chinh

| Luong | Lop chinh | Vai tro |
| --- | --- | --- |
| Google OAuth | `LoginActivity`, `RegisterActivity` | Diem user bam provider |
| Google OAuth | `SocialAuthRepository` | Lop trung gian cho UI |
| Google OAuth | `SocialAuthManager` | Build authorize URL, parse callback, bootstrap session |
| Google OAuth | `SocialAuthCallbackActivity` | Nhan deep link va dieu huong ket qua |
| Notification | `FoodCouriersClientApp` | Subscribe notification toan app, refresh badge/toast |
| Notification | `NotificationsFragment` | Danh sach notification va subscription cuc bo |
| Notification | `NotificationViewModel` | State list, unread count, success action |
| Notification | `NotificationRepository`, `NotificationClient` | REST read/update/delete notifications |
| Order | `OrdersFragment` | Danh sach orders va realtime theo user |
| Order | `OrderDetailActivity` | Chi tiet order va realtime theo order id |
| Order | `OrderTrackingActivity` | Timeline tracking va realtime theo order id |
| Order | `OrderRepository` | REST/RPC tao, doc, validate promotion, parse order |
| Realtime | `SupabaseRealtimeClient` | WebSocket, heartbeat, reconnect, subscribe/unsubscribe |
| Realtime | `RealtimeChannel`, `RealtimeListener` | Quan ly listener va callback INSERT/UPDATE/DELETE |
