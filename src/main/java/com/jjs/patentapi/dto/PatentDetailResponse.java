package com.jjs.patentapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatentDetailResponse {
    private String id;
    private String title;
    private String abstractText;
    private String applicant;
    private String filingDate;
    private String publicationNumber;
    private String originalUrl;
}
