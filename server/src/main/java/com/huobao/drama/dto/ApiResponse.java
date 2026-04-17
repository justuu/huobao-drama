package com.huobao.drama.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private T data;
    private String message;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, data, "success");
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, null, "success");
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, data, "success");
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, null, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, null, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, null, message);
    }
}
