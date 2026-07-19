package com.prafta.web.subcon.subcon02.service;

import org.springframework.stereotype.Service;

import com.prafta.web.subcon.subcon02.mapper.ChkptResultHistMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 순회점검 결과 덮어쓰기 감사 이력(append-only) 공용 캡처 서비스(PRAFTA-SUBCON-T6-AUDIT-02, plan D3).
 *
 * <p>단일 chokepoint 가 없으므로(자체저장/전파가 서로 다른 매퍼) write 경로 4곳(W1 앱 자체저장,
 * W2 응답 전파, W3 웹 불량 자체저장, W4 불량 전파)이 각자 캡처를 호출하되, 중복 구현/누락을 막기 위해
 * 본 서비스로 위임한다.
 *
 * <p><b>트랜잭션</b>: 별도 {@code @Transactional} 을 두지 않는다 — 호출 지점(W1/W3 의 {@code @Transactional},
 * W2/W4 의 전파 서비스 트랜잭션)에 그대로 참여한다. 원본 저장이 롤백되면 HIST INSERT 도 함께 롤백되어
 * 유령 이력이 남지 않는다(plan D5, 메인 세션 Q1 확정 — 감사 무결성 우선, best-effort 아님).
 *
 * <p><b>캡처 규약</b>(§1-1): 호출자가 write <b>직전</b>에 존재여부({@link #existsAnswer}/{@link #existsDefectAction})를
 * 조회해 CHG_TYPE 을 결정하고, write <b>직후</b> {@link #captureAnswer}/{@link #captureDefect} 로 스냅샷을 append 한다.
 * 캡처는 순수 append 이며 존재여부 판정은 호출자가 넘긴 chgType 을 그대로 신뢰한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChkptResultHistRecorder {

    /** 변경유형 — 신규(최초 write, INSERT). */
    public static final String CHG_TYPE_NEW = "01";

    /** 변경유형 — 덮어쓰기(기존 좌표 재저장, UPDATE). */
    public static final String CHG_TYPE_OVERWRITE = "02";

    /** 전파(체인 write)로 기록되는 HIST 의 트리거 주체 표식. */
    public static final String INSERT_NO_SYSTEM = "SYSTEM";

    private final ChkptResultHistMapper chkptResultHistMapper;

    /** 응답 좌표 존재여부 조회(write 직전) — 반환 &gt; 0 이면 {@link #CHG_TYPE_OVERWRITE}, 아니면 {@link #CHG_TYPE_NEW}. */
    public boolean existsAnswer(String cmpnyCd, String siteCd, String chkptCd, String inspectItemCd, String workDate) {
        return chkptResultHistMapper.selectExistsAnswer(cmpnyCd, siteCd, chkptCd, inspectItemCd, workDate) > 0;
    }

    /** 응답 스냅샷 append(write 직후) — 방금 쓴 행을 HIST 로 복제한다. */
    public void captureAnswer(String cmpnyCd, String siteCd, String chkptCd, String inspectItemCd, String workDate,
            String chgType, String insertNo) {
        chkptResultHistMapper.insertAnswerHist(cmpnyCd, siteCd, chkptCd, inspectItemCd, workDate, chgType, insertNo);
        log.info("점검 응답 이력 적재 - {}:{}:{}:{}:{}, 변경유형={}, 트리거={}",
                cmpnyCd, siteCd, chkptCd, inspectItemCd, workDate, chgType, insertNo);
    }

    /** 불량조치 좌표 존재여부 조회(write 직전). */
    public boolean existsDefectAction(String cmpnyCd, String siteCd, String chkptCd, String inspectItemCd,
            String workDate) {
        return chkptResultHistMapper.selectExistsDefectAction(cmpnyCd, siteCd, chkptCd, inspectItemCd, workDate) > 0;
    }

    /** 불량조치 스냅샷 append(write 직후). */
    public void captureDefect(String cmpnyCd, String siteCd, String chkptCd, String inspectItemCd, String workDate,
            String chgType, String insertNo) {
        chkptResultHistMapper.insertDefectActionHist(cmpnyCd, siteCd, chkptCd, inspectItemCd, workDate, chgType, insertNo);
        log.info("불량조치 이력 적재 - {}:{}:{}:{}:{}, 변경유형={}, 트리거={}",
                cmpnyCd, siteCd, chkptCd, inspectItemCd, workDate, chgType, insertNo);
    }

    /** 존재여부 boolean 을 CHG_TYPE 코드로 변환하는 헬퍼(호출자 편의). */
    public static String chgType(boolean existed) {
        return existed ? CHG_TYPE_OVERWRITE : CHG_TYPE_NEW;
    }
}
