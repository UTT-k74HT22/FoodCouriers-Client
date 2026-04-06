package com.utt.foodcouriers_client.data.remote;

public abstract class BaseResponse<T> {
    private BaseResponse() {}

    public static final class Success<T> extends BaseResponse<T> {
        private final T data;
        public Success(T data) { this.data = data; }
        public T getData() { return data; }
    }

    public static final class Error<T> extends BaseResponse<T> {
        private final String message;
        private final int code;
        public Error(String message, int code) {
            this.message = message;
            this.code = code;
        }
        public String getMessage() { return message; }
        public int getCode() { return code; }
    }
}