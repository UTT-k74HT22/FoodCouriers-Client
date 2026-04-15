# BA Analysis: Realtime & Notification Module - Phase Status Report
# Date: 2026-04-15
# Status: P0 Completed | P1-P2 Pending

---

## Executive Summary

| Item | Status | Notes |
|------|--------|-------|
| Database Realtime (orders) | ✅ **Đã enable** | Publication `supabase_realtime` có orders |
| Database Realtime (notifications) | ✅ **Đã enable** | Publication `supabase_realtime` có notifications |
| RLS policies | ✅ **Đã config** | Orders & Notifications có RLS đúng |
| P0: Client App - Order Realtime | ✅ **Completed** | OrderTrackingActivity + OrderDetailActivity |
| P1: Notification Module | ❌ **Pending** | Chỉ có UI skeleton |
| P2: Admin App Realtime | ❌ **Pending** | Chưa implement |

---

## Phase P0: Client App - Order Realtime ✅ COMPLETED

### Implementation Summary

| Task | File | Description |
|------|------|-------------|
| P0.1 | `FoodCouriersClientApp.java` | Initialize SupabaseRealtimeClient on app start |
| P0.1 | `ProfileFragment.java` | Disconnect realtime on logout |
| P0.2 | `OrderTrackingActivity.java` | Subscribe orders table, auto-refresh |
| P0.3 | `OrderDetailActivity.java` | Subscribe orders table, auto-refresh |
| P0.4 | Both Activities | Cleanup - unsubscribe on destroy |

### Code Changes Detail

#### P0.1: FoodCouriersClientApp.java

```java
public static void initializeRealtime(String accessToken) {
    if (accessToken == null || accessToken.isEmpty()) return;
    SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();
    client.initialize(accessToken);
    client.connect();
}

public static void disconnectRealtime() {
    SupabaseRealtimeClient.getInstance().disconnect();
}
```

**Called from:** `onCreate()` when `sessionManager.isLoggedIn() == true`

#### P0.2 & P0.3: OrderTrackingActivity.java & OrderDetailActivity.java

```java
// Fields:
public static final String TAG_REAL_TIME = "RealTimeWebSocket";
private RealtimeChannel orderChannel;
private String currentOrderId;

// subscribeToOrderUpdates():
orderChannel = SupabaseRealtimeClient.getInstance()
    .subscribe("public:orders", "id=eq." + orderId, listener);

// onDestroy():
if (orderChannel != null) {
    SupabaseRealtimeClient.getInstance().unsubscribe(orderChannel);
    orderChannel = null;
}
```

### Data Flow After P0

```
Admin updates order status
         │
         ▼
Supabase DB (orders table updated)
         │
         ▼
Supabase Realtime ─WebSocket──► Client App
         │
         ▼
OrderTrackingActivity.onUpdate()
         │
         ▼
viewModel.loadOrderDetail() 
         │
         ▼
UI auto-refresh (không cần user pull-to-refresh)
```

### Files Modified - P0 Complete

| File | Lines Changed | Description |
|------|---------------|-------------|
| `FoodCouriersClientApp.java` | +15 | Add initializeRealtime(), disconnectRealtime() |
| `ProfileFragment.java` | +4 | Call disconnectRealtime() on logout |
| `OrderTrackingActivity.java` | +70 | Add realtime subscription with logs |
| `OrderDetailActivity.java` | +80 | Add realtime subscription with logs |

---

## Phase P1: Notification Module - ❌ PENDING

### Current Status

| Component | Status | Notes |
|-----------|--------|-------|
| `NotificationItem.java` | ✅ Model có sẵn | Đã define đầy đủ fields |
| `NotificationRepository.java` | ❌ Empty class | Chỉ có singleton pattern |
| `NotificationViewModel.java` | ❌ Empty class | Chỉ extends BaseViewModel |
| `NotificationsFragment.java` | ⚠️ UI skeleton | Chỉ có layout, chưa load data |

### Required Implementation

| Task | Action | Description |
|------|--------|-------------|
| P1.1 | 🆕 Create | `NotificationClient.java` - REST API client |
| P1.2 | ⚠️ Modify | `NotificationRepository.java` - Add methods |
| P1.3 | ⚠️ Modify | `NotificationViewModel.java` - Add LiveData + logic |
| P1.4 | ⚠️ Modify | `NotificationsFragment.java` - Connect ViewModel |
| P1.5 | 🆕 Create | `NotificationAdapter.java` - RecyclerView adapter |
| P1.6 | ⚠️ Modify | Add realtime subscription |

---

## Phase P2: Admin App Realtime - ❌ PENDING

### Potential Implementation

| Task | Description |
|------|-------------|
| P2.1 | Subscribe to orders table for real-time new order notifications |
| P2.2 | Sound/notification when new order arrives |
| P2.3 | Auto-refresh order list |

---

## Implementation Priority Summary

| Priority | Phase | Task | Status | Effort |
|----------|-------|------|--------|--------|
| P0 | Order Realtime | Initialize Client + Subscribe Orders | ✅ Completed | Medium |
| P1 | Notification | Repository + ViewModel + UI | ❌ Pending | Medium |
| P1 | Notification | Realtime Subscribe | ❌ Pending | Low |
| P2 | Admin App | Realtime for Order List | ❌ Pending | Medium |

---

## Pros & Cons Analysis

### Using Supabase Realtime

| Pros | Cons |
|------|------|
| ✅ Real-time push, không cần polling | ⚠️ WebSocket consume battery (thấp hơn polling) |
| ✅ Tăng UX đáng kể cho order tracking | ⚠️ Cần handle disconnect/reconnect |
| ✅ Server-side đã config sẵn, không tốn thêm chi phí | ⚠️ Supabase realtime có limit connections |
| ✅ Hỗ trợ filter theo user_id, order_id | ⚠️ Không hoạt động khi app background |

---

## User Experience Improvement

| Scenario | Before P0 | After P0 |
|----------|-----------|----------|
| Admin xác nhận đơn | User không biết cho đến khi refresh | User thấy ngay "Đã nhận" step |
| Shipper nhận đơn | User refresh mới thấy | User thấy "Đang giao" |
| Đơn hoàn thành | User phải kiểm tra thủ công | User thấy "Đã giao" ngay |

---

## Next Steps

1. **P1**: Hoàn thiện Notification Module (Repository → ViewModel → UI)
2. **P1**: Thêm realtime subscription cho notifications list
3. **P2**: Admin App realtime (optional)

---

*Generated: 2026-04-15*
*Last Updated: 2026-04-15*
