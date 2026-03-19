package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementVO {
    private Integer id;
    private String title;
    private String content;
    private String summary;
    private String type;
    private String level;
    private Integer targetRole;
    private String targetText;
    private Integer isTop;
    private Integer isPublished;
    private String roomHint;
    private Integer createdBy;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
