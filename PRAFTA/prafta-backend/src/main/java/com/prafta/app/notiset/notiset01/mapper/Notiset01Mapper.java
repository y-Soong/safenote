package com.prafta.app.notiset.notiset01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.notiset.notiset01.application.command.PushSettingUpsertCommand;
import com.prafta.app.notiset.notiset01.mapper.result.PushSettingRowResult;

/**
 * 푸시 알림 수신 설정 Mapper (PRAFTA-APP-021-1, 앱).
 *
 * <p>식별자(cmpnyCd/userCd)는 JWT 클레임 출처(IDOR 차단). leading 콤마, #{} 바인딩, SELECT * 금지.
 * 본인 행만 조회/저장한다(WHERE CMPNY_CD + USER_CD 강제).
 */
@Mapper
public interface Notiset01Mapper {

    /**
     * 본인 푸시 설정 전 행 조회(마스터 '__MASTER__' 행 포함).
     * 행이 없는 NOTI_TYPE 은 응답 단계에서 ON(opt-out) 으로 보정한다.
     */
    List<PushSettingRowResult> selectMySettings(@Param("cmpnyCd") String cmpnyCd,
                                                @Param("userCd") String userCd);

    /**
     * 설정 1행 upsert(PK 충돌 시 USE_YN/UPDATE 갱신).
     * {@code INSERT ... ON DUPLICATE KEY UPDATE}.
     */
    int upsertSetting(PushSettingUpsertCommand command);

    /**
     * isAdmin 판정용 — 전사(사업장 무관) 노드 정/부 관리자 존재 여부.
     * tb_site_node 의 MAIN_ADMIN_CD/SUB_ADMIN_CD 에 본인 userCd 가 있으면 1.
     * (AppAdminAccessMapper.existsNodeAdminAnySite 와 동일 판정을 본 도메인에 자체 보유 —
     *  app/web 분리·도메인 독립 위해 cross-module 호출 대신 복제.)
     *
     * @return 0/1
     */
    int existsNodeAdminAnySite(@Param("cmpnyCd") String cmpnyCd,
                               @Param("userCd") String userCd);
}
