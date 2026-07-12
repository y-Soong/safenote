package com.prafta.platform.company.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.platform.common.command.CompanyInsertCommand;
import com.prafta.platform.common.command.PlatformUserInsertCommand;
import com.prafta.platform.company.application.command.SiteInsertCommand;
import com.prafta.platform.company.application.command.SiteNodeInsertCommand;
import com.prafta.platform.company.application.command.WorktypeSeedCommand;

/**
 * 플랫폼 영역 프로비저닝/부트스트랩 전용 매퍼.
 *
 * <p>모든 쓰기는 INSERT 만 수행한다(기존 '001'/타 회사 데이터 변경 금지). 템플릿 복제는
 * '001' 회사 행을 SELECT 하여 신규 CMPNY_CD 로 INSERT 한다.
 */
@Mapper
public interface CompanyProvisionMapper {

    // ===== 공용(부트스트랩 + 프로비저닝) =====

    /** 회사코드 존재 여부(충돌검사). 있으면 1 이상. */
    int selectCmpnyExists(@Param("cmpnyCd") String cmpnyCd);

    /** 회사 내 USER_ID 존재 여부(부트스트랩 멱등 판정). 있으면 1 이상. */
    int selectUserIdExists(@Param("cmpnyCd") String cmpnyCd, @Param("userId") String userId);

    /** 전사 대상 휴대폰번호 존재 여부(프로비저닝 중복검사). 있으면 1 이상. (phoneHmac = HmacSigner 로 계산한 HMAC 값) */
    int selectPhoneNumberExists(@Param("phoneHmac") String phoneHmac);

    /** TB_CMPNY 1건 INSERT. */
    int insertCmpny(CompanyInsertCommand command);

    /** USER_CD 채번 (LoginMapper.selectUserCd / User01Mapper.selectNextUserCd 와 동일 SQL). */
    String selectNextUserCd(@Param("cmpnyCd") String cmpnyCd);

    /** TB_USER 1건 INSERT(플랫폼 공용 — ACCOUNT_STATUS/SITE_CD/NODE_CD 가변). */
    int insertUser(PlatformUserInsertCommand command);

    // ===== 프로비저닝 전용 =====

    /** SITE_CD 채번 (Baim01Mapper.selectSiteCd 와 동일 SQL). */
    String selectNextSiteCd(@Param("cmpnyCd") String cmpnyCd);

    /** TB_SITE 1건 INSERT(최초 사업장). */
    int insertSite(SiteInsertCommand command);

    /** TB_SITE_NODE 1건 INSERT(최초 노드, MAIN_ADMIN_CD=신규 master). */
    int insertSiteNode(SiteNodeInsertCommand command);

    /** TB_USER_SITE_AUTH 1건 INSERT(사용자↔사업장 권한). */
    int insertUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("siteCd") String siteCd, @Param("gvUserCd") String gvUserCd);

    /** tb_syst_auth_menu: 템플릿(srcCmpnyCd) AUTH_CD ∈ {master,hr,safe,99999} 행을 신규 회사로 복제. */
    int copyAuthMenuFromTemplate(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("newCmpnyCd") String newCmpnyCd, @Param("gvUserCd") String gvUserCd);

    /** tb_baim_val_m: 템플릿 USE_YN='Y' 그룹을 신규 회사로 복제. */
    int copyBaimValMFromTemplate(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("newCmpnyCd") String newCmpnyCd, @Param("gvUserCd") String gvUserCd);

    /** tb_baim_val_d: 템플릿 USE_YN='Y' 만(COM005 제외), 그룹별 BAIM_VAL_D_CD 를 00001 부터 SORT_IDX 순 재부여하여 복제. */
    int copyBaimValDFromTemplateRenumber(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("newCmpnyCd") String newCmpnyCd, @Param("gvUserCd") String gvUserCd);

    /** tb_baim_val_d: COM005(권한) 그룹만 원본 BAIM_VAL_D_CD/VAL_D_INFO* 를 renumber 없이 verbatim 복제(표준 권한코드 보존). */
    void copyBaimValDPreserveAuth(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("newCmpnyCd") String newCmpnyCd, @Param("gvUserCd") String gvUserCd);

    /** tb_cmm_seq: 신규 회사 복제된 baim_val_d 그룹별 건수를 CURR_VAL 로 시드(SEQ_KEY=BAIM_VAL_CD). */
    int seedCmmSeqFromBaimValD(@Param("newCmpnyCd") String newCmpnyCd);

    /** TB_SCH_MGMT 기본 근무타입(ST001) 1건 INSERT. */
    int insertWorktype(WorktypeSeedCommand command);
}
