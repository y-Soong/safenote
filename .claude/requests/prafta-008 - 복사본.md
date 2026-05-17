\## 백엔드 ApiException 관련 수정사항



1. 아래와 같은 형태의 Exception 처리가 의도한대로 동작하지 않고있음

\-throw new ApiException(AttdErrorCode.ATTD\_404\_010);



기대한 동작 방식은 AttdErrorCode.Attd\_404\_010에 매칭시켜놓은 오류 메시지가 프론트앤드로 넘어가서 사용자에게 보여지길 원하지만 프론트에서 아래와같이 Exception 메시지를 처리하고 있어서 
백엔드에서 던진 메시지가 후순위로 잡혀 의도한대로 동작하지 않고있음



&#x20;   const msg =

&#x20;     err?.response?.data?.message ||

&#x20;     err?.message ||

&#x20;     getMessage(MSG.SAVE\_ERROR);



모든 화면을 체크하여 상기와 같은 방식으로 예외처리를 하고있는 부분을 수정해줘



백앤드에서 던지는 메시지가 가장 우선해서 사용자에게 보여질 값이야



\## 추가 설명이 필요하거나 모호한 부분이 있을 경우는 채팅으로 질문해줘

