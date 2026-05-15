package com.jjs.patentapi.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PatentSearchResponse {
    private String query;
    private int page;
    private int size;
    private int totalCount;
    private List<PatentItem> items;
}
