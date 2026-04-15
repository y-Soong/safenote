package com.prafta.web.tbm.tbm01.dto.request;

import java.util.List;

import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TbmEduInfoRequest {

	private String mtrlCd;
	private String title;
	private String contents;
	private String mtrlType;
	private String useYn;
	private List<TbmEduItemInfoModel> tbmEduItemInfoModelList;
}
