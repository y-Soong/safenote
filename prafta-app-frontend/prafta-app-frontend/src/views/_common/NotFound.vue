<template>
  <div class="not-found">
    <h1>404</h1>
    <p>페이지를 찾을 수 없습니다.</p>
    <router-link to="/" class="home-link">메인으로 돌아가기</router-link>
  </div>
</template>

<script setup>
// 별도 로직 없음
</script>

<style scoped>
.not-found {
  text-align: center;
  padding: 100px 20px;
}
.not-found h1 {
  font-size: 6rem;
  color: #ff4d4f;
  margin: 0;
}
.not-found p {
  margin: 10px 0 20px;
  font-size: 1.2rem;
  color: #666;
}
.home-link {
  display: inline-block;
  margin-top: 10px;
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
