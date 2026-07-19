package com.prafta.web.baim.baim01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.baim.BaimErrorCode;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.baim.baim01.application.command.MasterSiteAuthSetCommand;
import com.prafta.web.baim.baim01.application.command.SiteAdminSiteAuthCommand;
import com.prafta.web.baim.baim01.application.command.SiteInfoCommand;
import com.prafta.web.baim.baim01.application.command.SiteNodeInfoCommand;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;
import com.prafta.web.baim.baim01.application.param.SiteInfoListParam;
import com.prafta.web.baim.baim01.application.param.SiteInfoParam;
import com.prafta.web.baim.baim01.application.query.SiteInfoListQuery;
import com.prafta.web.baim.baim01.dto.response.SiteInfoListResponse;
import com.prafta.web.baim.baim01.mapper.Baim01Mapper;
import com.prafta.web.baim.baim01.result.SiteInfoResult;
import com.prafta.web.baim.baim01.service.Baim01Service;
import com.prafta.web.subcon.subcon02.service.SiteLinkPropagationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim01ServiceImpl implements Baim01Service{
	private final Baim01Mapper baim01Mapper;

	// PRAFTA-SUBCON-T2-05: 원본 사업장 변경의 미러 재귀 전파(동기 + 동일 트랜잭션 — 실패 시 전체 롤백).
	private final SiteLinkPropagationService siteLinkPropagationService;

	// PRAFTA-COM-001-T2-2: END_DATE/STR_DATE 는 varchar(8) YYYYMMDD 문자열. 오늘과의 비교도 8자리 문자열 비교로 일관.
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

	public Baim01ServiceImpl(Baim01Mapper baim01Mapper,
			SiteLinkPropagationService siteLinkPropagationService) {
		this.baim01Mapper = baim01Mapper;
		this.siteLinkPropagationService = siteLinkPropagationService;
	}
	
	
	public SiteInfoListResponse selectSiteInfoList(SiteInfoListParam param) {
		
		SiteInfoListResponse response = null;
		
		List<SiteInfoResult> siteInfoList = baim01Mapper.selectSiteInfoList(SiteInfoListQuery.from(param));
		
		if(siteInfoList.size() > 0) {
			response = SiteInfoListResponse.builder()
					.siteInfoList(siteInfoList)
					.build();
		}
		
		return response;	
	}
	
	@Transactional
	public void saveSiteInfo(SiteInfoParam param) {

		// PRAFTA-COM-001-T2 보안 재작업: 사업장 저장 EP 역할 게이트(fail-closed).
		//   사업장 종료(USE_YN='N')는 소속 사용자 전원 로그인 차단으로 이어지는 파괴적 쓰기다.
		//   기존 JWT 유효성만으로는 일반 사용자(AUTH_CD='99999' 등)도 호출 가능했으므로,
		//   사업장 관리 권한(전사 권한 master/hr)만 통과시킨다. authCd 는 JWT 도출값만 신뢰한다.
		//   cross-tenant 는 CMPNY_CD 가 JWT 바인딩이라 별도 봉인됨(intra-tenant 권한 부재만 차단).
		assertSiteManageRole(param.gvAuthCd(), param.gvUserCd());

		for(SiteInfoModel model : param.siteInfoModelList()) {
			
			String siteCd = "";
			boolean isNewSite = (model.siteCd() == null);   // PRAFTA-042-4: 신규 사업장 생성 여부

			if(!isNewSite) {								// 기존 사업장 데이터 변경
				siteCd = model.siteCd();

				// PRAFTA-SUBCON-T2-04: 미러(LINK_SRC NOT NULL) 사업장 잠금 가드(plan D5 — 필드 diff 방식).
				//   잠금 필드(§5-5) 변경 감지 시 거부, 무변경이면 SITE_ADMIN_CD 단독 UPDATE 로 우회
				//   (mergeSiteInfo 전체 UPSERT 경유 금지 — 전파값 오염 방지). 신규 생성은 미러일 수 없다.
				String linkSrcCmpnyCd = baim01Mapper.selectSiteLinkSrcCmpny(model.gvCmpnyCd(), siteCd);
				if (linkSrcCmpnyCd != null) {
					String[] mirrorResolved = resolveEndDateBoundary(model.endDate(), model.useYn());
					SiteInfoCommand mirrorCommand = SiteInfoCommand.from(model, siteCd, mirrorResolved[0], mirrorResolved[1]);

					if (baim01Mapper.selectMirrorLockedFieldChangedCnt(mirrorCommand) > 0) {
						log.warn("미러 사업장 잠금 필드 변경 거부 - gvCmpnyCd={}, siteCd={}, linkSrc={}",
								model.gvCmpnyCd(), siteCd, linkSrcCmpnyCd);
						throw new ApiException(SubconErrorCode.SUBCON_403_002);
					}

					baim01Mapper.updateSiteAdminOnly(mirrorCommand);
					log.info("미러 사업장 SITE_ADMIN_CD 단독 저장 - gvCmpnyCd={}, siteCd={}", model.gvCmpnyCd(), siteCd);
					continue;	// 미러는 노드/전체 UPSERT/전파 전부 건너뜀(원본 소유사 전파로만 갱신).
				}
			} else {										// 신규 사업장 생성
				siteCd = baim01Mapper.selectSiteCd(model.gvCmpnyCd());
			}

			// 초기 1 depth 노드 생서
			baim01Mapper.insertSiteNodeInfo(SiteNodeInfoCommand.from(model, siteCd));

			// PRAFTA-COM-001-T2-2: A안 종료일 경계로 endDate/useYn 보정 후 저장.
			String[] resolved = resolveEndDateBoundary(model.endDate(), model.useYn());
			String resolvedEndDate = resolved[0];
			String resolvedUseYn   = resolved[1];

			baim01Mapper.mergeSiteInfo(SiteInfoCommand.from(model, siteCd, resolvedEndDate, resolvedUseYn));

			// PRAFTA-SUBCON-T2-05: 저장 후 미러 재귀 전파(활성 링크 없으면 no-op — 동일 트랜잭션).
			siteLinkPropagationService.propagateSiteInfo(model.gvCmpnyCd(), siteCd);

			// PRAFTA-042-4 (D3-①): 전사 접근 역할(master/hr/safe + system)에게 신규 사업장 권한 자동 부여.
			//   신규 생성 분기에서만 호출한다. 기존 사업장 수정 저장 시 재부여하면 D7(역할 이탈 회수)로
			//   회수된 권한이 부활하는 부작용(R-5)이 생기므로 수정 경로에서는 호출하지 않는다.
			if(isNewSite) {
				baim01Mapper.mergeMasterSiteAuthSet(MasterSiteAuthSetCommand.from(model, siteCd));

				// 신규 사업장에 지정한 관리자(SITE_ADMIN_CD)에게도 해당 사업장 권한 자동 부여.
				//   전사 역할(master/hr/safe)이 아닌 일반 관리자를 지정한 경우 위 호출로는 부여되지 않으므로
				//   별도로 부여한다. 매퍼가 동일 회사 사용자로 한정하고 ON DUPLICATE 로 멱등 처리하므로,
				//   관리자가 이미 전사 역할이라 부여된 경우에도 안전하다(중복 무해).
				if (model.siteAdminCd() != null && !model.siteAdminCd().isBlank()) {
					baim01Mapper.mergeSiteAdminSiteAuth(SiteAdminSiteAuthCommand.from(model, siteCd));
				}
			}
		}
	}

	/**
	 * PRAFTA-COM-001-T2-2 — 사업종료일 A안 경계 보정.
	 *
	 * <p>입력 endDate(YYYY-MM-DD 또는 YYYYMMDD, 빈 값 가능) 와 useYn 을 받아
	 * [보정 endDate(정규화 YYYYMMDD 또는 null), 보정 useYn] 을 반환한다.
	 *
	 * <ul>
	 *   <li>(5.2.4 재개방) useYn='Y' 인데 종료일이 과거({@code endDate < 오늘})면 → 종료일 NULL, useYn='Y' 유지.</li>
	 *   <li>(5.2.2 과거/당일) 종료일이 설정되어 있고 {@code endDate <= 오늘} 이면 → 즉시 useYn='N'.</li>
	 *   <li>(5.2.2 미래) {@code endDate > 오늘} 이면 → useYn 손대지 않음(자정 스케줄러 T2-3 이 도래 시 비활성).</li>
	 *   <li>종료일 미설정(빈 값)이면 → 종료일 null, useYn 입력값 유지.</li>
	 * </ul>
	 *
	 * @return {@code [resolvedEndDate(null 가능), resolvedUseYn]}
	 */
	private String[] resolveEndDateBoundary(String rawEndDate, String rawUseYn) {
		String todayYmd = LocalDate.now().format(YMD);

		// 종료일 정규화: '-' 제거 후 YYYYMMDD 8자리만 인정. 그 외(빈 값/형식 불일치)는 미설정으로 본다.
		String endYmd = normalizeYmd(rawEndDate);

		// 사용여부 기본값 보정(mergeSiteInfo 의 IFNULL(NEW.USE_YN,'Y') 과 정합).
		String useYn = (rawUseYn == null || rawUseYn.isBlank()) ? "Y" : rawUseYn;

		// 종료일 미설정 → 종료일 NULL, useYn 입력값 유지.
		if (endYmd == null) {
			return new String[] { null, useYn };
		}

		// (5.2.4) useYn='Y' 로 저장하는데 종료일이 과거 → 종료일 자동 NULL 재개방.
		if ("Y".equals(useYn) && endYmd.compareTo(todayYmd) < 0) {
			return new String[] { null, "Y" };
		}

		// (5.2.2 과거/당일 포함, A안) 종료일이 오늘 이하 → 즉시 비활성.
		if (endYmd.compareTo(todayYmd) <= 0) {
			return new String[] { endYmd, "N" };
		}

		// (5.2.2 미래) 종료일이 오늘 이후 → useYn 변경 없이 종료일만 저장(스케줄러 위임).
		return new String[] { endYmd, useYn };
	}

	/**
	 * 사업장 저장 EP 역할 게이트(PRAFTA-COM-001-T2 보안 재작업).
	 *
	 * <p>사업장 생성/수정/종료는 회사 단위 조직 설정에 해당하는 관리 동작이며, 특히 종료(USE_YN='N')는
	 * 소속 사용자 전원 로그인 차단(가용성 영향)을 유발한다. 따라서 사업장 관리 권한
	 * (master/hr/safe/system — system 은 운영사, master/hr/safe 는 고객사 관리자)만 허용한다.
	 * 공통 정책서 §8(권한 결정 모델)·§6(사업장 관리) 및 사업장 권한 자동부여 집합과 정합한다.
	 *
	 * <p>authCd 는 JWT 도출값만 신뢰한다(클라 바디 신뢰 금지). 미충족 시 BAIM_403_002.
	 */
	private void assertSiteManageRole(String authCd, String userCd) {
		// 사업장 관리 권한(master/hr/safe/system): 허용
		if (AuthRoleUtils.canManageSite(authCd)) {
			return;
		}
		log.warn("사업장 저장 권한 없음(역할 게이트 차단) - userCd={}, authCd={}", userCd, authCd);
		throw new ApiException(BaimErrorCode.BAIM_403_002);
	}

	/** '-' 제거 후 YYYYMMDD 8자리 숫자만 반환. 형식 불일치/빈 값은 null. */
	private String normalizeYmd(String raw) {
		if (raw == null) {
			return null;
		}
		String compact = raw.replace("-", "").trim();
		if (compact.length() != 8 || !compact.chars().allMatch(Character::isDigit)) {
			return null;
		}
		return compact;
	}

}
