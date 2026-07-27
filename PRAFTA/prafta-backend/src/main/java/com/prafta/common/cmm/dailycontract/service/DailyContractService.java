package com.prafta.common.cmm.dailycontract.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;
import com.prafta.common.cmm.dailycontract.result.ContractAmendPrecheckResult;
import com.prafta.common.cmm.dailycontract.result.ContractDocMeta;
import com.prafta.common.cmm.dailycontract.result.ContractMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.result.SignGateResult;
import com.prafta.common.cmm.file.application.model.FileBytesResult;

/**
 * 일용직 근로계약서 core 서비스 (웹 User_07/User_08 / 앱 dailycontract01 공용).
 *
 * <p>출처: 일용직 계약서+승인제 요청서 §2 D1·D2·D3·D8, §3 R1·R2·R5, §4-3, §6 / plan §T3.
 * 정책서: {@code common/11-security-privacy.md} §11.1·§11.3.
 *
 * <p>열람 인가 원칙(IDOR): 본인(세션 userCd 일치) 또는 해당 사업장 관리자(master/hr + SITE_AUTH —
 * {@code DailyEntryService.hasSiteAuthority} 재사용)만. 파일 경로는 응답에 노출하지 않고 스트림만 반환한다.
 */
public interface DailyContractService {

    /**
     * 사업장 활성 계약서 단건 조회. 없으면 null.
     *
     * <p><b>활성 기준 유지</b>(J6) — 사업장 단위 조회이므로 pin(사용자 컨텍스트 필요)을 해석할 수 없다.
     * 서명·열람 경로는 이 메서드를 쓰지 않는다.
     */
    ActiveContractResult findActiveContract(String cmpnyCd, String siteCd);

    /**
     * 계약서 서명 게이트 판정 (R2 + D8 + 승인시점 버전확정 J4).
     *
     * <p><b>판정 기준 = 승인 시점 확정(pin) 버전</b>. pin 미등록 센티넬(0) → 스킵(K4),
     * pin NULL(배포 전 레거시) → 활성 폴백(K8) → 활성도 없으면 스킵.
     * 대상 계약서가 정해진 뒤 (대상 버전 서명 없음 OR 서명의 REQ_ID 가 현재 승인 사이클 REQ_ID 와 불일치)
     * → 서명 필요('Y'). REQ_ID NULL 레거시 서명은 버전만 비교.
     * 일용직 아님(TB_DAILY_USER 미존재)도 스킵('N').
     */
    SignGateResult judgeSignGate(String cmpnyCd, String userCd);

    /**
     * 계약서 등록/교체 (웹 User_07 — D8-② 재서명 트리거).
     * 기존 활성 USE_YN='N' + CONTRACT_VER MAX+1 INSERT 를 한 트랜잭션으로 처리한다.
     * 업로드 검증: PDF 또는 이미지(png/jpg) + 10MB 이하 + 확장자-내용 정합(DAILYCONTRACT_400_004),
     * PDF 페이지 1~20(400_005), 암호 PDF 차단(400_006).
     *
     * @return 신규 계약서 버전
     */
    int registerContract(String cmpnyCd, String siteCd, String contractNm, MultipartFile file,
            String procUserCd, String procAuthCd);

    /**
     * 계약서 사용중지 (웹 User_07). 활성 계약서 없으면 DAILYCONTRACT_400_003.
     */
    void stopContract(String cmpnyCd, String siteCd, String procUserCd, String procAuthCd);

    /**
     * 미서명 계약서 <b>in-place 정정</b> (웹 User_07 — 승인시점 버전확정 T3 / K6·K7·J8~J11).
     *
     * <p>버전을 올리지 않고 파일만 제자리 교체한다 → <b>재서명 트리거가 발생하지 않는다</b>
     * ({@code CONTRACT_VER} 미증가 / {@code USE_YN} 불변 — J9). 오등록 정정이 사업장 전원 재서명을
     * 유발하지 않게 하는 것이 목적이다.
     *
     * <p>허용 조건은 <b>해당 (사업장, 버전) 서명 행 0건 단독</b>이다("당일" 조건 없음 — K7).
     * 조건 검증은 프론트 노출과 무관하게 <b>서버에서 강제</b>한다(1건 이상 → {@code DAILYCONTRACT_400_009}).
     * 계약서명({@code CONTRACT_NM})은 정정 대상이 아니다(등록 시 고정값이므로 일관성 유지).
     *
     * <p>기존 파일 행·디스크 파일은 삭제하지 않고(J8 — 감사 추적 + 삭제 실패 시 정합 붕괴 방지)
     * 새 {@code FILE_MGMT_CD} 로 저장 후 참조만 교체한다. 이전 파일코드는 로그에만 남는다.
     *
     * <p>업로드 검증은 등록({@link #registerContract})과 <b>동일 경로 재사용</b>이다(J11 —
     * PDF/이미지·10MB·20페이지·암호 PDF·페이지 렌더 픽셀 상한).
     *
     * @param contractVer 정정 대상 버전(서버가 활성 여부를 재검증 — 활성 아니면 {@code 409_002})
     * @throws com.prafta.common.exception.ApiException 400_003(해당 버전 없음) / 400_009(서명자 존재) /
     *         403_001(사업장 인가 실패) / 409_002(활성 아님·동시 정정 경합)
     */
    void amendActiveContract(String cmpnyCd, String siteCd, int contractVer, MultipartFile file,
            String procUserCd, String procAuthCd);

    /**
     * in-place 정정 사전 점검 (웹 User_07 정정 팝업 — 승인시점 버전확정 T3 / J10).
     *
     * <p>정정 가능 여부와 경고용 카운트를 반환한다. 최종 방어는 {@link #amendActiveContract} 의
     * 서버측 재검증이며, 본 EP 결과는 UI 표시 목적이다. 해당 버전이 없으면 null(호출부 400_003 — 존재 비노출).
     */
    ContractAmendPrecheckResult findAmendPrecheck(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd);

    /** 사업장 계약서 버전 이력 (웹 User_07 — 사업장 인가 가드 포함). */
    List<ContractVersionRow> findContractVersions(String cmpnyCd, String siteCd,
            String procUserCd, String procAuthCd);

    /**
     * 특정 버전 계약서 원본 로드 (웹 User_07 미리보기 — 사업장 인가 가드 포함).
     *
     * <p>PDF/이미지 원본 바이트를 그대로 반환한다(웹은 Content-Type 동적 스트림이라 변환 불필요).
     * 해당 버전 자체가 없으면 null(호출부 400_003), 행은 있는데 디스크 원본이 없으면
     * {@code DAILYCONTRACT_404_002}(미등록과 구분 — 운영 진단).
     */
    FileBytesResult loadContractImageForAdmin(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd);

    /**
     * 활성 계약서 원본 로드 (앱 일용직 서명 화면 — 본인 사업장 자동 스코프). 없으면 null.
     *
     * <p><b>구버전 앱 폴백(T5/P7)</b>: 원본이 PDF 면 전 페이지를 세로로 이어붙인 단일 PNG 로 변환해
     * 반환한다(URL·인가·응답 형태 무변경). 이미지 원본은 바이트 그대로.
     * 신규 앱은 {@link #findActiveContractMeta}/{@link #loadActiveContractPage} 를 사용한다.
     */
    FileBytesResult loadActiveContractImageForUser(String cmpnyCd, String userCd);

    /** 활성 계약서 메타 + 문서 형식/페이지 수 (앱 pager 초기화 — T4). 일용직 아님/미등록이면 null. */
    ContractMetaResult findActiveContractMeta(String cmpnyCd, String userCd);

    /**
     * 활성 계약서 <b>단일 페이지</b> 로드 (앱 pager — T4, 1-base).
     *
     * <p>PDF 는 150DPI PNG 온디맨드 렌더, 이미지 원본은 {@code page=1} 만 유효하며 원본 바이트 그대로.
     * 범위 밖 페이지는 {@code DAILYCONTRACT_400_007}. 일용직 아님/미등록이면 null.
     */
    FileBytesResult loadActiveContractPage(String cmpnyCd, String userCd, int page);

    /** 특정 버전 계약서의 형식/페이지 수 (웹 User_07 — 사업장 인가 가드 포함, T4). 해당 버전 없으면 null. */
    ContractDocMeta findContractMetaForAdmin(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd);

    /**
     * 서명 저장 (R5 — 서버 합성 + SHA-256 + append-only INSERT).
     *
     * <p>서명 PNG 서버 검증(크기/형식/디코딩) → 계약서 원본 + §4-3 자동 계약정보 블록 + 서명 합성 →
     * 합성본·원본 저장(tb_file_info) → 해시 기록 → INSERT(USER_NM_SNAPSHOT, 최근 소진 REQ_ID 연결).
     * 현재 사이클 서명 존재 시 DAILYCONTRACT_400_002(멱등 가드).
     *
     * @return 생성된 서명 메타(signId 등)
     */
    ContractSignMetaResult signContract(String cmpnyCd, String userCd, MultipartFile signFile);

    /** 본인 최신 서명 메타 조회 (앱 내 계약서 — 교부 의무 §6-1). 없으면 null. */
    ContractSignMetaResult findMySign(String cmpnyCd, String userCd);

    /**
     * 본인 최신 서명본 로드 — <b>구버전 앱 표시용</b>(본인 스코프 강제 — userCd 로만 조회). 없으면 null.
     *
     * <p>T5/P7 + plan §2 A-5: 서명본이 PDF 면 세로 병합 PNG 로 변환해 반환한다(구버전 앱의
     * {@code <img src>} 열람이 깨지지 않도록 — 근로기준법 §17② 교부 화면 연속성).
     * <b>레거시 PNG 합성본은 바이트 그대로</b>(재합성·변환·덮어쓰기 금지 — P3).
     */
    FileBytesResult loadMySignImage(String cmpnyCd, String userCd);

    /**
     * 본인 최신 서명본 <b>원본 바이트</b> 로드 (신규 앱 저장/다운로드용 — T5, 폴백 변환 없음).
     *
     * <p>본인 스코프 강제(IDOR 차단). PDF 서명본은 {@code application/pdf},
     * 레거시 합성본은 {@code image/png} 로 내려간다. 없으면 null(호출부 404_001).
     */
    FileBytesResult loadMySignFile(String cmpnyCd, String userCd);

    /** 본인 최신 서명본의 형식/페이지 수 (앱 내 계약서 pager — T4). 서명/파일 없으면 null. */
    ContractDocMeta describeMySignDoc(String cmpnyCd, String userCd);

    /**
     * 본인 최신 서명본 <b>단일 페이지</b> 로드 (앱 내 계약서 pager — T4, 1-base).
     *
     * <p>PDF 는 150DPI PNG 렌더, 레거시 PNG 합성본은 {@code page=1} 만 유효하며 원본 바이트 그대로.
     * 범위 밖 페이지는 {@code DAILYCONTRACT_400_007}. 서명/파일 없으면 null.
     */
    FileBytesResult loadMySignPage(String cmpnyCd, String userCd, int page);

    /** 서명 이력 목록 (웹 User_08 탭2 — 사업장 인가 가드 포함, 만료/탈퇴 계정 포함). */
    List<ContractSignRow> findSignListForAdmin(ContractSignListQuery query,
            String procUserCd, String procAuthCd);

    /**
     * 서명본 로드 (웹 User_08 — 관리자 열람/다운로드).
     * signId 의 SITE_CD 기준 사업장 인가 가드(IDOR). 미존재 시 DAILYCONTRACT_404_001.
     *
     * <p>PDF/레거시 PNG 모두 <b>원본 바이트를 그대로</b> 반환한다(웹은 Content-Type 동적 스트림).
     */
    FileBytesResult loadSignImageForAdmin(String cmpnyCd, String signId,
            String procUserCd, String procAuthCd);
}
