# Module: Quản lý đơn hàng - Client App

## 1. Mục tiêu

Module Order dành cho khách hàng (`customer`) để:
- Thực hiện đặt hàng từ giỏ hàng.
- Theo dõi trạng thái đơn hàng thời gian thực.
- Xem lịch sử đơn hàng.
- Hủy đơn hàng trong điều kiện cho phép.

## 2. Luồng nghiệp vụ chuẩn

Dựa trên quy trình mới của hệ thống, luồng đơn hàng từ phía khách hàng sẽ trải qua các giai đoạn sau:

### 2.1 Đặt hàng (Checkout)
- **Điều kiện**: Người dùng phải đăng nhập.
- **Hành động**: Khách hàng chọn các món trong giỏ hàng (có thể thuộc nhiều nhà hàng).
- **Xử lý**: 
    - Hệ thống tách các món theo từng nhà hàng (`restaurant_id`).
    - Tạo các đơn hàng riêng biệt cho mỗi nhà hàng thông qua `rpc_create_order`.
    - Trạng thái ban đầu: `orders.status = 'pending'`, `delivery_status = 'unassigned'`.

### 2.2 Theo dõi đơn hàng (Order Tracking)
Khách hàng theo dõi hai luồng trạng thái song song:

#### A. Trạng thái chuẩn bị món (`orders.status`)
1. **Chờ xác nhận (`pending`)**: Đơn mới tạo, nhà hàng chưa tiếp nhận.
2. **Đã xác nhận (`confirmed`)**: Nhà hàng đã chấp nhận đơn.
3. **Đang chuẩn bị (`preparing`)**: Nhà hàng đang nấu nướng.
4. **Sẵn sàng lấy (`ready_for_pickup`)**: Món ăn đã xong, chờ tài xế đến lấy.
5. **Đang giao (`delivering`)**: Tài xế đã lấy hàng và đang trên đường đến khách.
6. **Đã giao (`delivered`)**: Khách hàng đã nhận được hàng.
7. **Đã hủy (`cancelled`)**: Đơn bị hủy bởi khách, nhà hàng hoặc hệ thống.

#### B. Trạng thái giao hàng (`delivery_status`)
1. **Chưa gán (`unassigned`)**: Chưa bắt đầu tìm tài xế.
2. **Đang tìm tài xế (`searching`)**: Hệ thống đang tìm tài xế gần đó.
3. **Đã có tài xế (`assigned`)**: Tài xế đã nhận đơn (có thể lúc này nhà hàng vẫn đang `preparing`).
4. **Đang đến nhà hàng (`arriving_pickup`)**: Tài xế đang di chuyển tới quán.
5. **Đang chờ lấy hàng (`waiting_pickup`)**: Tài xế đã đến quán nhưng món chưa xong.
6. **Đã lấy hàng (`picked_up`)**: Tài xế đã cầm món ăn (tương ứng lúc này `orders.status` chuyển sang `delivering`).
7. **Hoàn thành (`completed`)**: Tài xế đã giao xong.

### 2.3 Hủy đơn hàng
- Khách hàng chỉ có thể hủy đơn khi trạng thái là `pending`.
- Nếu đơn đã sang `confirmed` hoặc `preparing`, việc hủy đơn cần sự can thiệp của Admin/Nhà hàng hoặc tuân theo chính sách hoàn tiền.

## 3. Các màn hình chính

### 3.1 Danh sách đơn hàng (Orders Fragment)
- Hiển thị danh sách các đơn hàng hiện tại và lịch sử.
- Thông tin chính: Mã đơn, Tên nhà hàng, Tổng tiền, Trạng thái đơn hàng hiện tại, Thời gian đặt.

### 3.2 Chi tiết & Theo dõi đơn hàng (Order Tracking Activity)
- Hiển thị Timeline chi tiết của cả 2 luồng trạng thái.
- Bản đồ vị trí nhà hàng, điểm giao và vị trí shipper (nếu có).
- Thông tin Shipper: Tên, Số điện thoại (chỉ hiển thị khi `delivery_status` từ `assigned` trở đi).

## 4. Đồng bộ dữ liệu (Realtime)
- Sử dụng Supabase Realtime để lắng nghe thay đổi trên bảng `orders` theo `order_id` cụ thể.
- Khi có thay đổi về `status` hoặc `delivery_status`, UI cần cập nhật Timeline và thông báo cho người dùng.

## 5. Các quy tắc chuyển đổi trạng thái (Phía DB/RPC xử lý)
Khách hàng chủ yếu quan sát, tuy nhiên App cần hiểu logic để hiển thị UI phù hợp:
- `delivering` chỉ bắt đầu khi tài xế đã `picked_up`.
- Tìm tài xế có thể bắt đầu ngay khi đơn là `confirmed` hoặc thậm chí `preparing`.
