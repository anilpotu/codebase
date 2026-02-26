import api from './axios';

export const getProducts = () => api.get('/api/products');
export const getProductById = (id) => api.get(`/api/products/${id}`);
export const searchProducts = (name) => api.get('/api/products/search', { params: { name } });
export const getProductsByCategory = (category) => api.get(`/api/products/category/${category}`);
export const createProduct = (data) => api.post('/api/products', data);
export const updateProduct = (id, data) => api.put(`/api/products/${id}`, data);
export const deleteProduct = (id) => api.delete(`/api/products/${id}`);
export const decrementStock = (id, quantity) => api.patch(`/api/products/${id}/stock`, null, { params: { quantity } });
