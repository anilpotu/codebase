import api from './axios';

export const createProfile = (data) => api.post('/api/profiles', data);
export const getProfileByUserId = (userId) => api.get(`/api/profiles/user/${userId}`);
export const createPost = (data) => api.post('/api/posts', data);
export const getPostsByUserId = (userId) => api.get(`/api/posts/user/${userId}`);
export const sendConnectionRequest = (data) => api.post('/api/connections', data);
export const acceptConnection = (id) => api.put(`/api/connections/${id}/accept`);
export const getConnections = (userId, status) => {
  const params = status ? { status } : {};
  return api.get(`/api/connections/user/${userId}`, { params });
};
