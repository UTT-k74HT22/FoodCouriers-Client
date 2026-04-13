package com.utt.foodcouriers_client.utils.websocket;

import com.google.gson.JsonObject;

/**
 * Interface định nghĩa các callback để lắng nghe sự kiện realtime từ Supabase.
 * 
 * Sử dụng khi subscribe vào một database table để nhận thông báo khi có
 * INSERT, UPDATE, hoặc DELETE xảy ra.
 * 
 * Ví dụ sử dụng:
 * <pre>
 * RealtimeListener listener = new RealtimeListener() {
 *     {@literal @}Override
 *     public void onInsert(JsonObject record) {
 *         String id = record.get("id").getAsString();
 *         Log.d("New record:", id);
 *     }
 *     
 *     {@literal @}Override
 *     public void onUpdate(JsonObject record, JsonObject oldRecord) {
 *         Log.d("Updated:", record.toString());
 *     }
 *     
 *     {@literal @}Override
 *     public void onDelete(JsonObject oldRecord) {
 *         Log.d("Deleted:", oldRecord.toString());
 *     }
 * };
 * </pre>
 */
public interface RealtimeListener {
    /**
     * Hàm được gọi khi có sự kiện mới.
     * @param record Dữ liệu của sự kiện.
     */
    void onInsert(JsonObject record);

    /**
     * Hàm được gọi khi có sự kiện cập nhật.
     * @param record Dữ liệu mới.
     * @param oldRecord Dữ liệu cũ.
     */
    void onUpdate(JsonObject record, JsonObject oldRecord);

    /**
     * Hàm được gọi khi có sự kiện xóa.
     * @param oldRecord Dữ liệu cũ.
     */
    void onDelete(JsonObject oldRecord);

    /**
     * Hàm được gọi khi kết nối thành công.
     */
    default void onConnected() {}

    /**
     * Hàm được gọi khi kết nối bị mất.
     */
    default void onDisconnected() {}

    /**
     * Hàm được gọi khi có lỗi.
     * @param error Thông báo lỗi.
     */
    default void onError(String error) {}
}