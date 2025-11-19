<template>
  <div>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const token = ref(localStorage.getItem('token'));

// 监听token变化，更新全局请求头
watchEffect(() => {
  if (token.value) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token.value}`;
  } else {
    delete axios.defaults.headers.common['Authorization'];
  }
});

const logout = () => {
  localStorage.removeItem('token');
  token.value = null;
  router.push('/login');
};
</script>