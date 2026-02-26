import api from './axios';

export const register = (data) => api.post('/api/auth/register', data);
export const login = (data) => api.post('/api/auth/login', data);
export const refreshToken = (refreshTokenValue) =>
  api.post('/api/auth/refresh', { refreshToken: refreshTokenValue });
export const validateToken = (token) =>
  api.get('/api/auth/validate', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
export const logout = () => api.post('/api/auth/logout');
