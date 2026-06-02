<template>
  <div class="coming-soon">
    <h1>준비 중입니다</h1>
    <p>이 페이지는 곧 업데이트됩니다.</p>
    <router-link to="/main" class="home-link">메인으로 돌아가기</router-link>
  </div>
</template>

<script setup>
// 별도 로직 없음
</script>

<style scoped>
.coming-soon {
  text-align: center;
  padding: 100px 20px;
}
.coming-soon h1 {
  font-size: 2.5rem;
  color: #409eff;
  margin-bottom: 10px;
}
.coming-soon p {
  font-size: 1.2rem;
  color: #666;
}
.home-link {
  display: inline-block;
  margin-top: 20px;
  padding: 10px 16px;
  background-color: #409eff;
  color: white;
  border-radius: 6px;
  text-decoration: none;
}
.home-link:hover {
  background-color: #66b1ff;
}
</style>
