<template>
  <div id="app">
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const token = ref(localStorage.getItem('token'));

const logout = () => {
  localStorage.removeItem('token');
  token.value = null;
  router.push('/login');
};

onMounted(() => {
  router.push('/home'); // 默认跳转到首页
});
</script>

<style scoped>
main {
  padding: 20px;
}
</style>

<style>
/* 全局样式可以从 example.html 的 <style> 中提取 */
body {
  font-family: 'Segoe UI', 'Microsoft YaHei', Roboto, Arial, sans-serif;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0fff4 100%);
  color: #2d3748;
  line-height: 1.6;
}
</style>
