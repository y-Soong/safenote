package com.prafta.web.notice.notice01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 생성/수정 요청.
 * 생성 시 noticeId 는 무시(서버 채번), 수정 시 noticeId 필수.
 * editPwd 는 생성 시 필수(BCrypt 해시 저장), 수정 시 검증용(master 면제).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeSaveRequest {
    private String noticeId;          // 수정 시만
    private String title;
    private String content;
    private String editPwd;           // 생성 시 신규 비번 / 수정 시 검증 비번
    private String targetScope;       // ALL / SITE / NODE
    private String includeDailyYn;    // Y/N
    private String popupYn;           // Y/N
    private String popupFromYmd;      // YYYYMMDD (popupYn=Y 시 필수)
    private String popupToYmd;        // YYYYMMDD (popupYn=Y 시 필수)
    private String pinYn;             // Y/N
    private Integer pinOrder;         // pinYn=Y 시 (서버 정규화)

    private List<NoticeTargetRequest> targetList; // SITE/NODE 시 대상 다건
    private List<NoticeFileRequest> fileList;     // 첨부 다건
}
