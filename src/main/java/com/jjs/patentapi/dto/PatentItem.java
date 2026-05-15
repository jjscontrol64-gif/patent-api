package com.jjs.patentapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatentItem {
    private String id;
    private String title;
    private String applicant;
    private String filingDate;
    private String publicationNumber;
}
