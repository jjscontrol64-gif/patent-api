package com.jjs.patentapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatentSearchRequest {

    @NotBlank(message = "검색어를 입력하세요")
    private String query;

    private String from;
    private String to;

    @Min(value = 1, message = "page는 1 이상이어야 합니다")
    private int page = 1;

    @Min(value = 1, message = "size는 1 이상이어야 합니다")
    @Max(value = 50, message = "size는 50 이하이어야 합니다")
    private int size = 10;
}
