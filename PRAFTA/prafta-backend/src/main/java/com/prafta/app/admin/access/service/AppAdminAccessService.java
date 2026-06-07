package com.prafta.app.admin.access.service;

import com.prafta.app.admin.access.application.param.AdminAccessParam;
import com.prafta.app.admin.access.dto.response.AdminAccessContextResponse;

/**
 * 001-P1-B1: 관리자 모드 진입판정(access-context) 서비스.
 *
 * <p>읽기 전용 조회이므로 @Transactional 은 부여하지 않는다. 식별자는 token 출처(IDOR 차단).
 */
public interface AppAdminAccessService {

    /** A1: 진입판정 컨텍스트 산출(역할축/노드축/접근사업장/모듈 활성·스코프 맵). */
    AdminAccessContextResponse selectAccessContext(AdminAccessParam param);    
}
