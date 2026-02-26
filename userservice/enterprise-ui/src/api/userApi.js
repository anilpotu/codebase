import api from './axios';

const basePath = '/api/grpc-users';

export const createUser = (data) => api.post(basePath, data);
export const getUser = (id) => api.get(`${basePath}/${id}`);
export const deleteUser = (id) => api.delete(`${basePath}/${id}`);
