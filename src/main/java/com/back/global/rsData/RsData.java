package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RsData<T>(
        String resultCode,
        String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    /** resultCode "201-1" 의 앞부분(201)을 HTTP 상태 코드로 사용한다. */
    @JsonIgnore
    public int statusCode() {
        return Integer.parseInt(resultCode.split("-", 2)[0]);
    }
}
