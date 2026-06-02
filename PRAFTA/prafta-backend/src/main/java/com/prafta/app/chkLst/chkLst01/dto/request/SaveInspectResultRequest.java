package com.prafta.app.chkLst.chkLst01.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

/**
 * prafta-036-B1: 점검결과 저장(multipart/form-data) 요청.
 * <p>multipart 바인딩 특성상 MultipartFile 필드 포함, Lombok @Data 사용.
 * <p>prafta-app-011: userCd 필드 제거 -- DB 기록은 tokenInfo.gv_userCd() 사용.
 *   클라이언트가 userCd 를 전송해도 바인딩 대상 필드가 없으므로 무시된다.
 */
@Data
public class SaveInspectResultRequest {
    private String cmpnyCd;
    private String siteCd;
    private String chkptCd;
    private String workDate;
    private MultipartFile items;
}
