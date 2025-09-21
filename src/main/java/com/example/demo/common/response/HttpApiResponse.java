package com.example.demo.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@AllArgsConstructor
@Getter
public class HttpApiResponse<T> {
    private final CodeEnum code;
    private final String message;
    private final T data;

    public static <T> HttpApiResponse of(T data){
        return HttpApiResponse.builder()
                .code(CodeEnum.RS_001)
                .data(data)
                .message("")
                .build();
    }
    // 기본
    public static HttpApiResponse fromExceptionMessage(String message){
        return HttpApiResponse.builder()
                .code(CodeEnum.FRS_001)
                .data(null)
                .message(message)
                .build();
    }
    // 특정 코드 지정
    public static HttpApiResponse fromExceptionMessage(CodeEnum code, String message){
        return HttpApiResponse.builder()
                .code(code)
                .data(null)
                .message(message)
                .build();
    }
    //데이터 포함
    public static HttpApiResponse fromExceptionMessage(String message, CodeEnum code, Map<String, Object> data){
        return HttpApiResponse.builder()
                .code(code)
                .data(data)
                .message(message)
                .build();
    }


}




