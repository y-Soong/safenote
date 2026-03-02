export function fnSearchAddress(zipRef, addr1Ref, addr2Ref) {
  new window.daum.Postcode({
    oncomplete: function (data) {
      console.log(data);
      zipRef.value = data.zonecode;
      addr1Ref.value = data.address;
      addr2Ref.value = "";
    },
  }).open();
}
