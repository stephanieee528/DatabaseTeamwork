import { createRouter, createWebHistory } from 'vue-router';
import Home from '@/pages/Home.vue';
import County from '@/pages/County.vue';
import Analysis from '@/pages/Analysis.vue';
import Alerts from '@/pages/Alerts.vue';
import Login from '@/pages/Login.vue';
import Users from '@/pages/Users.vue';
//import Dashboard from '@/pages/Dashboard.vue';
import Profile from '@/pages/Profile.vue';
import Register from '@/pages/Register.vue';
const routes = [
  { path: '/home', component: Home, meta: { requiresAuth: false } },
  { path: '/county', component: County, meta: { requiresAuth: true } },
{ path: '/analysis', component: Analysis, meta: { requiresAuth: true, roles: ['群众', '数据分析师', '管理员'] } },
{ path: '/alerts', component: Alerts, meta: { requiresAuth: true, roles: ['管理员'] } },
  { path: '/login', component: Login, meta: { requiresAuth: false } },
  { path: '/users', component: Users, meta: { requiresAuth: true, roles: ['管理员'] } },
  //{ path: '/dashboard', component: Dashboard, meta: { requiresAuth: true } },
  { path: '/profile', component: Profile, meta: { requiresAuth: true } },
  { path: '/register', component: Register, meta: { requiresAuth: false } },
  { path: '/', redirect: '/home' },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;

// 路由守卫
const getCachedRole = (): string | null => {
  const cached = localStorage.getItem('currentUser');
  if (!cached) return null;
  try {
    const parsed = JSON.parse(cached);
    return parsed.role || parsed.roleName || null;
  } catch {
    return null;
  }
};

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  const requiredRoles = (to.meta.roles as string[] | undefined) || [];
  
  // 检查是否需要认证
  if (to.meta.requiresAuth && !token) {
    next('/login');
    return;
  }

  if (requiredRoles.length > 0) {
    const role = getCachedRole();
    if (!role) {
      next('/login');
      return;
    }
    if (!requiredRoles.includes(role)) {
      next('/home');
      return;
    }
  }
  
  // 已登录用户访问登录/注册页时跳转到首页
  if ((to.path === '/login' || to.path === '/register') && token) {
    next('/home');
    return;
  }
  
  next();
});

