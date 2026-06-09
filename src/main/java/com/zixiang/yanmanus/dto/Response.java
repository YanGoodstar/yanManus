package com.zixiang.yanmanus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Response<T> ok(T data) {
        return new Response<>(200, "success", data);
    }

    public static <T> Response<T> ok(String message, T data) {
        return new Response<>(200, message, data);
    }

    public static <T> Response<T> ok() {
        return new Response<>(200, "success", null);
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(500, message, null);
    }
}
