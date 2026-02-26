import api from './axios';

export const getProfiles = () => api.get('/api/users');
export const getProfileById = (id) => api.get(`/api/users/${id}`);
export const updateProfileById = (id, data) => api.put(`/api/users/${id}`, data);
export const deleteProfileById = (id) => api.delete(`/api/users/${id}`);
export const getMyProfile = () => api.get('/api/users/me');
export const updateMyProfile = (data) => api.put('/api/users/me', data);
