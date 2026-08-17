package com.prafta.web.subcon.subcon02.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.mapper.Subcon02Mapper;
import com.prafta.web.subcon.subcon02.result.LinkDstRaw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 원본 사업장/근무타입 변경의 재귀 전파 서비스(PRAFTA-SUBCON-T2-05).
 *
 * <p>전파 방식 = 동기 전파 + 호출자 트랜잭션 참여(전파 실패 시 원본 저장 전체 롤백 — plan D1).
 * 전파 값은 DB 원본 행에서만 복제한다(사용자 입력 미경유 — 주입 면 차단, UPDATE_NO='SYSTEM').
 *
 * <p>재귀 단위 = tb_site_link ACTIVE 행. 중간 해지로 링크가 끊기면 그 지점에서 전파가
 * 자연 정지하고, 독립화된 수신사의 자체 수정은 자기 하위 활성 링크로만 전파된다(새 루트 — plan D8).
 *
 * <p>훅 호출부: {@code Baim01ServiceImpl.saveSiteInfo}(저장 후), {@code Attd01ServiceImpl.updateSchInfo}(저장 후),
 * {@code Attd01ServiceImpl.updateShiftSchInfo}(저장 후 — SHIFT-LINK-T3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteLinkPropagationService {

    private final Subcon02Mapper subcon02Mapper;

    /** 체인 깊이 안전핀 — 초과 시 데이터 오염(루프)으로 간주하고 전체 롤백(plan §5-3 #4 준용). */
    private static final int MAX_CHAIN_DEPTH = 20;

    /**
     * 사업장 기본정보 변경 전파(직속 미러 → 하위 미러 재귀).
     * 전파 필드 = §5-5 잠금 필드(SITE_NO·SITE_ADMIN_CD 제외). 활성 링크가 없으면 no-op.
     */
    @Transactional
    public void propagateSiteInfo(String cmpnyCd, String siteCd) {
        propagateSiteInfoInternal(cmpnyCd, siteCd, 0);
    }

    /**
     * 근무타입 변경 전파(신규 추가·사용중지·APPLY_DATE 포함 — 전 컬럼 UPSERT).
     * 미러 SCH_CD = 원본 SCH_CD(D3) 라 재귀에서도 동일 schCd 로 하강한다.
     * 전파 반영분은 미러 테넌트 TB_SCH_MGMT_HIST 에도 기록한다(D7).
     */
    @Transactional
    public void propagateSchInfo(String cmpnyCd, String siteCd, String schCd) {
        propagateSchInfoInternal(cmpnyCd, siteCd, schCd, 0);
    }

    /**
     * 교대 정의 신규 생성 전파(SHIFT-LINK-T3 — 정의 4테이블, 신규 생성 전파 한정: 지시서 §2.1-2).
     * 미러 SHIFT_CD = 원본 SHIFT_CD(D3 동형)라 재귀에서도 동일 shiftCd 로 하강한다.
     * 교대 정의는 HIST 테이블이 없어 이력 기록 없음(plan §0-2). 활성 링크가 없으면 no-op.
     */
    @Transactional
    public void propagateShiftInfo(String cmpnyCd, String siteCd, String shiftCd) {
        propagateShiftInfoInternal(cmpnyCd, siteCd, shiftCd, 0);
    }

    // =========================== private ===========================

    private void propagateSiteInfoInternal(String cmpnyCd, String siteCd, int depth) {
        assertDepth(depth, cmpnyCd, siteCd);

        List<LinkDstRaw> links = subcon02Mapper.selectActiveLinksBySrcSite(cmpnyCd, siteCd);
        for (LinkDstRaw link : links) {
            int updated = subcon02Mapper.propagateMirrorSite(cmpnyCd, siteCd, link.dstCmpnyCd(), link.dstSiteCd());
            log.info("사업장 기본정보 전파 - link={}, {}:{} -> {}:{}, 영향행={}",
                    link.linkId(), cmpnyCd, siteCd, link.dstCmpnyCd(), link.dstSiteCd(), updated);

            // 하위 미러로 재귀(수신 미러가 다시 SRC 인 활성 링크 — n차 체인).
            propagateSiteInfoInternal(link.dstCmpnyCd(), link.dstSiteCd(), depth + 1);
        }
    }

    private void propagateSchInfoInternal(String cmpnyCd, String siteCd, String schCd, int depth) {
        assertDepth(depth, cmpnyCd, siteCd);

        List<LinkDstRaw> links = subcon02Mapper.selectActiveLinksBySrcSite(cmpnyCd, siteCd);
        for (LinkDstRaw link : links) {
            int affected = subcon02Mapper.propagateMirrorSch(
                    cmpnyCd, siteCd, schCd, link.dstCmpnyCd(), link.dstSiteCd());

            // 원본 행이 없으면(affected=0) 미러에 반영할 것이 없다 — HIST/재귀 생략.
            if (affected > 0) {
                // 미러 테넌트 이력 기록(수신사 Attd_01 이력 화면 정합 — INSERT_NO='SYSTEM').
                int histIdx = subcon02Mapper.selectMirrorSchHistIdx(link.dstCmpnyCd(), link.dstSiteCd(), schCd);
                subcon02Mapper.insertMirrorSchHist(link.dstCmpnyCd(), link.dstSiteCd(), schCd, histIdx);
            }

            log.info("근무타입 전파 - link={}, {}:{}:{} -> {}:{}, 영향행={}",
                    link.linkId(), cmpnyCd, siteCd, schCd, link.dstCmpnyCd(), link.dstSiteCd(), affected);

            propagateSchInfoInternal(link.dstCmpnyCd(), link.dstSiteCd(), schCd, depth + 1);
        }
    }

    private void propagateShiftInfoInternal(String cmpnyCd, String siteCd, String shiftCd, int depth) {
        assertDepth(depth, cmpnyCd, siteCd);

        List<LinkDstRaw> links = subcon02Mapper.selectActiveLinksBySrcSite(cmpnyCd, siteCd);
        for (LinkDstRaw link : links) {
            // 부모 정의 전파(순수 INSERT — 신규 채번 SHIFT_CD 라 미러에 없음. PK 충돌 = 데이터 오염 →
            // 예외 전파 = 원본 저장 롤백이 올바른 동작이므로 UPSERT 로 덮지 않는다).
            int affected = subcon02Mapper.propagateMirrorShift(
                    cmpnyCd, siteCd, shiftCd, link.dstCmpnyCd(), link.dstSiteCd());

            // 원본 행이 없으면(affected=0) 하위 전파할 것도 없다 — 하위 3문 생략.
            if (affected > 0) {
                subcon02Mapper.propagateMirrorShiftPtrn(
                        cmpnyCd, siteCd, shiftCd, link.dstCmpnyCd(), link.dstSiteCd());
                subcon02Mapper.propagateMirrorShiftTeamMeta(
                        cmpnyCd, siteCd, shiftCd, link.dstCmpnyCd(), link.dstSiteCd());
                subcon02Mapper.propagateMirrorShiftAssign(
                        cmpnyCd, siteCd, shiftCd, link.dstCmpnyCd(), link.dstSiteCd());
            }

            log.info("교대 정의 전파 - link={}, {}:{}:{} -> {}:{}, 영향행={}",
                    link.linkId(), cmpnyCd, siteCd, shiftCd, link.dstCmpnyCd(), link.dstSiteCd(), affected);

            // 하위 미러로 재귀(수신 미러가 다시 SRC 인 활성 링크 — n차 체인, 미러 SHIFT_CD=원본 코드).
            propagateShiftInfoInternal(link.dstCmpnyCd(), link.dstSiteCd(), shiftCd, depth + 1);
        }
    }

    /** 깊이 안전핀 — 초과 시 링크 데이터 오염(루프) 감지로 간주(로그 필수 + 전체 롤백). */
    private void assertDepth(int depth, String cmpnyCd, String siteCd) {
        if (depth > MAX_CHAIN_DEPTH) {
            log.error("연동 전파 깊이 초과(데이터 오염 의심) - cmpnyCd={}, siteCd={}, depth={}", cmpnyCd, siteCd, depth);
            throw new ApiException(SubconErrorCode.SUBCON_500_001);
        }
    }
}
