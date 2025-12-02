<template>
  <header class="header">
    <div class="nav-container">
      <div class="logo">832工程贫困县可视化分析系统</div>
      <nav>
        <ul class="nav-menu">
          <li>
            <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
              首页
            </router-link>
          </li>
          <li>
            <router-link to="/county" class="nav-link" :class="{ active: $route.path === '/county' }">
              县详情
            </router-link>
          </li>
          <li v-if="canViewAnalysis">
            <router-link to="/analysis" class="nav-link" :class="{ active: $route.path === '/analysis' }">
              数据分析
            </router-link>
          </li>
          <li v-if="canManageAlerts">
            <router-link to="/alerts" class="nav-link" :class="{ active: $route.path === '/alerts' }">
              警告管理
            </router-link>
          </li>
          
          <!-- 登录后显示个人中心和登出 -->
          <li v-if="isLoggedIn">
            <router-link to="/profile" class="nav-link" :class="{ active: $route.path === '/profile' }">
              个人中心
            </router-link>
          </li>
          <li v-if="isLoggedIn">
            <a @click="handleLogout" class="nav-link logout-btn">登出</a>
          </li>
          
          <!-- 未登录时显示登录 -->
          <li v-else>
            <router-link to="/login" class="nav-link" :class="{ active: $route.path === '/login' }">
              登录
            </router-link>
          </li>
          
          <!-- 管理员可见的用户管理 -->
          <li v-if="canManageUsers">
            <router-link to="/users" class="nav-link" :class="{ active: $route.path === '/users' }">
              用户管理
            </router-link>
          </li>
        </ul>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getCurrentUser } from '@/api';
import { ElMessage, ElMessageBox } from 'element-plus';
import axios from 'axios';



const router = useRouter();
const route = useRoute();

const isLoggedIn = ref(!!localStorage.getItem('token'));
const userRole = ref('');
const currentUser = ref('');
const canViewAnalysis = computed(() => isLoggedIn.value && ['群众', '数据分析师', '管理员'].includes(userRole.value));
const canManageUsers = computed(() => isLoggedIn.value && userRole.value === '管理员');
const canManageAlerts = computed(() => isLoggedIn.value && userRole.value === '管理员');

// 检查登录状态
const applyUserProfile = (payload) => {
  if (!payload) return;
  currentUser.value = payload.fullname || payload.username || '';
  userRole.value = payload.roleName || payload.role || '';
};

const restoreUserFromCache = () => {
  const cached = localStorage.getItem('currentUser');
  if (!cached) return false;
  try {
    const parsed = JSON.parse(cached);
    applyUserProfile(parsed);
    return true;
  } catch {
    return false;
  }
};

const checkLoginStatus = () => {
  isLoggedIn.value = !!localStorage.getItem('token');
  if (isLoggedIn.value) {
    loadUserInfo();
  } else {
    currentUser.value = '';
    userRole.value = '';
    localStorage.removeItem('currentUser');
  }
};

// 加载用户信息
const loadUserInfo = async () => {
  const hadCache = restoreUserFromCache();
  try {
    const response = await getCurrentUser();
    const userData = response.data;
    const payload = {
      username: userData.username,
      fullname: userData.fullname || userData.username,
      role: userData.roleName || userData.role || '',
      roleName: userData.roleName || userData.role || ''
    };
    localStorage.setItem('currentUser', JSON.stringify(payload));
    applyUserProfile(payload);
  } catch (error) {
    if (!hadCache) {
      currentUser.value = '';
      userRole.value = '';
    }
    console.error('获取用户信息失败:', error);
  }
};

// 登出功能
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    localStorage.removeItem('token');
    isLoggedIn.value = false;
    currentUser.value = '';
    userRole.value = '';
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    // 清除全局请求头中的token
    delete axios.defaults.headers.common['Authorization'];
    
    ElMessage.success('已成功登出');
    router.push('/login');
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('登出失败:', error);
    }
  }
};

// 监听存储变化（用于多个标签页同步登录状态）
const handleStorageChange = (event) => {
  if (event.key === 'token') {
    checkLoginStatus();
  }
  if (event.key === 'currentUser' && event.newValue) {
    restoreUserFromCache();
  }
};

onMounted(() => {
  checkLoginStatus();
  window.addEventListener('storage', handleStorageChange);
});

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange);
});
</script>

<style scoped>
.header {
  background: linear-gradient(135deg, #2c5282 0%, #3182ce 100%);
  color: white;
  padding: 20px 0;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  position: relative;
  overflow: hidden;
}

.header::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at top right, rgba(255,255,255,0.2) 0%, transparent 30%);
  pointer-events: none;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 12px;
  letter-spacing: 0.5px;
}

.logo::before {
  content: "📊";
  font-size: 32px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

.nav-menu {
  display: flex;
  gap: 30px;
  list-style: none;
}

.nav-menu a {
  color: rgba(255,255,255,0.9);
  text-decoration: none;
  transition: all 0.3s ease;
  padding: 8px 12px;
  border-radius: 20px;
  font-weight: 500;
}

.nav-menu a:hover {
  background: rgba(255,255,255,0.15);
  transform: translateY(-2px);
}

.nav-menu a.active {
  background: rgba(255,255,255,0.25);
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.logout-btn {
  cursor: pointer;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .nav-menu {
    flex-direction: column;
    gap: 10px;
  }
  
  .header {
    padding: 15px 0;
  }
}
</style>