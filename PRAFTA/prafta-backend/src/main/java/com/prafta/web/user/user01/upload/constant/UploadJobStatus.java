package com.prafta.web.user.user01.upload.constant;

/**
 * 사용자 일괄 생성 잡 상태 상수 카탈로그 (PRAFTA-037-F6).
 *
 * <p>{@code tb_user_upload_job.STATUS} 컬럼에 저장되는 SYS061 코드값.
 *
 * <p>상태 전이:
 * <pre>
 *   PENDING (잡 생성 직후, 비동기 시작 전)
 *      ↓
 *   RUNNING (행 처리 중)
 *      ↓
 *   SUCCESS (전체 성공) | PARTIAL (일부 실패) | FAILED (치명 예외)
 * </pre>
 */
public final class UploadJobStatus {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String PARTIAL = "PARTIAL";
    public static final String FAILED  = "FAILED";

    private UploadJobStatus() {
        // 상수 카탈로그 — 인스턴스화 방지
    }

    /** 폴링 종료 판정용 — 최종 상태(SUCCESS/PARTIAL/FAILED) 여부. */
    public static boolean isTerminal(String status) {
        return SUCCESS.equals(status) || PARTIAL.equals(status) || FAILED.equals(status);
    }
}
