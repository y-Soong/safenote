package com.prafta.web.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * TBM 교육자료 세부항목 조회 결과.
 *
 * <p>서명 URL 전환: 기존 record → 클래스(프로퍼티명 기반 MyBatis 매핑)로 변경하고,
 * 서비스(Tbm01ServiceImpl)에서 {@code FileUrlSigner} 로 발급한 서명 절대 URL 을 {@code fileUrl} 에 채운다.
 * 프론트는 {@code baseUrl + filePath + fileMgmtCd} 수동 조립 대신 {@code fileUrl} 을 그대로 사용한다.
 * (filePath/fileMgmtCd/fileExt 원시값은 편집 화면 호환을 위해 유지.)
 */
@Getter
@Setter
public class TbmEduItemInfoResult {

    private String chk;
    private String mtrlItemCd;
    private String mtrlCd;
    private String sortIdx;
    private String mtrlItemType;
    private String mtrlDesc;
    private String fileMgmtCd;
    private String fileNm;
    private String filePath;
    private String fileExt;
    private String url;
    private String useYn;

    private String thumbFileMgmtCd;   // prafta-033-A: 썸네일 파일코드
    private String durationSec;        // prafta-033-A: 미디어 길이(초, 동영상)

    // TBM_AI F1: 항목 AI 재열람 컬럼(analysis-status 별도병합과 별개로 조회 1차 소스)
    private String aiAnalyzeYn;   // AI_ANALYZE_YN (Y/N)
    private String aiStatus;      // AI_STATUS (SYS056 D_CD: NONE/ANALYZING/DRAFT/FAILED/CONFIRMED)
    private String aiConfirmDesc; // AI_CONFIRM_DESC (확정 서술)

    /* 데이터 초기화용 값 */
    private String oriSortIdx;
    private String oriMtrlItemType;
    private String oriMtrlDesc;
    private String oriFileMgmtCd;
    private String oriFileNm;
    private String oriFilePath;
    private String oriFileExt;
    private String oriUrl;
    private String oriUseYn;

    /** 서명 절대 URL(파일형 항목). 파일 없으면 NULL. 서비스에서 채움(MyBatis 매핑 대상 아님). */
    private String fileUrl;
}
