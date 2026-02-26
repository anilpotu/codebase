import { useState } from 'react';
import {
  createProduct,
  decrementStock,
  deleteProduct,
  getProductById,
  getProducts,
  getProductsByCategory,
  searchProducts,
  updateProduct,
} from '../../api/productApi';

const unwrap = (res) => res?.data?.data ?? res?.data;

export default function ProductsPage() {
  const [products, setProducts] = useState(null);
  const [productId, setProductId] = useState('');
  const [lookupResult, setLookupResult] = useState(null);
  const [searchName, setSearchName] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [category, setCategory] = useState('');
  const [categoryResults, setCategoryResults] = useState(null);
  const [createForm, setCreateForm] = useState({ name: '', description: '', price: '', stockQuantity: '', category: '' });
  const [updateForm, setUpdateForm] = useState({ id: '', name: '', description: '', price: '', stockQuantity: '', category: '', active: true });
  const [stockForm, setStockForm] = useState({ id: '', quantity: '' });
  const [deleteId, setDeleteId] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const resetFeedback = () => {
    setError('');
    setMessage('');
  };

  const onGetAll = async () => {
    resetFeedback();
    try {
      const res = await getProducts();
      setProducts(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onGetById = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await getProductById(Number(productId));
      setLookupResult(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onSearch = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await searchProducts(searchName);
      setSearchResults(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onCategory = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await getProductsByCategory(category);
      setCategoryResults(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onCreate = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const payload = {
        ...createForm,
        price: Number(createForm.price),
        stockQuantity: Number(createForm.stockQuantity),
      };
      const res = await createProduct(payload);
      setLookupResult(unwrap(res));
      setMessage('Product created');
    } catch (err) {
      setError(err.message);
    }
  };

  const onUpdate = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const payload = {
        name: updateForm.name,
        description: updateForm.description,
        price: updateForm.price ? Number(updateForm.price) : undefined,
        stockQuantity: updateForm.stockQuantity ? Number(updateForm.stockQuantity) : undefined,
        category: updateForm.category,
        active: updateForm.active,
      };
      const res = await updateProduct(Number(updateForm.id), payload);
      setLookupResult(unwrap(res));
      setMessage('Product updated');
    } catch (err) {
      setError(err.message);
    }
  };

  const onDecrementStock = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await decrementStock(Number(stockForm.id), Number(stockForm.quantity));
      setLookupResult(unwrap(res));
      setMessage('Stock updated');
    } catch (err) {
      setError(err.message);
    }
  };

  const onDelete = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      await deleteProduct(Number(deleteId));
      setMessage(`Deleted product ${deleteId}`);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1>Product Service</h1>

      <div className="section">
        <h2>Browse Products</h2>
        <button className="btn" type="button" onClick={onGetAll}>Get All</button>
        <form className="form" onSubmit={onGetById} style={{ marginTop: 12 }}>
          <div className="form-group">
            <label>Product ID</label>
            <input type="number" value={productId} onChange={(e) => setProductId(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Get By ID</button>
        </form>
      </div>

      <div className="section">
        <h2>Search</h2>
        <form className="form" onSubmit={onSearch}>
          <div className="form-group">
            <label>Name Contains</label>
            <input value={searchName} onChange={(e) => setSearchName(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Search</button>
        </form>
        <form className="form" onSubmit={onCategory} style={{ marginTop: 12 }}>
          <div className="form-group">
            <label>Category</label>
            <input value={category} onChange={(e) => setCategory(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Get Category</button>
        </form>
      </div>

      <div className="section">
        <h2>Create Product</h2>
        <form className="form" onSubmit={onCreate}>
          <div className="form-group"><label>Name</label><input value={createForm.name} onChange={(e) => setCreateForm((s) => ({ ...s, name: e.target.value }))} required /></div>
          <div className="form-group"><label>Description</label><input value={createForm.description} onChange={(e) => setCreateForm((s) => ({ ...s, description: e.target.value }))} /></div>
          <div className="form-group"><label>Price</label><input type="number" step="0.01" value={createForm.price} onChange={(e) => setCreateForm((s) => ({ ...s, price: e.target.value }))} required /></div>
          <div className="form-group"><label>Stock</label><input type="number" value={createForm.stockQuantity} onChange={(e) => setCreateForm((s) => ({ ...s, stockQuantity: e.target.value }))} required /></div>
          <div className="form-group"><label>Category</label><input value={createForm.category} onChange={(e) => setCreateForm((s) => ({ ...s, category: e.target.value }))} /></div>
          <button className="btn" type="submit">Create</button>
        </form>
      </div>

      <div className="section">
        <h2>Update / Stock / Delete</h2>
        <form className="form" onSubmit={onUpdate}>
          <div className="form-group"><label>ID</label><input type="number" value={updateForm.id} onChange={(e) => setUpdateForm((s) => ({ ...s, id: e.target.value }))} required /></div>
          <div className="form-group"><label>Name</label><input value={updateForm.name} onChange={(e) => setUpdateForm((s) => ({ ...s, name: e.target.value }))} /></div>
          <div className="form-group"><label>Description</label><input value={updateForm.description} onChange={(e) => setUpdateForm((s) => ({ ...s, description: e.target.value }))} /></div>
          <div className="form-group"><label>Price</label><input type="number" step="0.01" value={updateForm.price} onChange={(e) => setUpdateForm((s) => ({ ...s, price: e.target.value }))} /></div>
          <div className="form-group"><label>Stock</label><input type="number" value={updateForm.stockQuantity} onChange={(e) => setUpdateForm((s) => ({ ...s, stockQuantity: e.target.value }))} /></div>
          <div className="form-group"><label>Category</label><input value={updateForm.category} onChange={(e) => setUpdateForm((s) => ({ ...s, category: e.target.value }))} /></div>
          <button className="btn" type="submit">Update</button>
        </form>

        <form className="form" onSubmit={onDecrementStock} style={{ marginTop: 12 }}>
          <div className="form-group"><label>Stock ID</label><input type="number" value={stockForm.id} onChange={(e) => setStockForm((s) => ({ ...s, id: e.target.value }))} required /></div>
          <div className="form-group"><label>Quantity</label><input type="number" value={stockForm.quantity} onChange={(e) => setStockForm((s) => ({ ...s, quantity: e.target.value }))} required /></div>
          <button className="btn" type="submit">Decrement Stock</button>
        </form>

        <form className="form" onSubmit={onDelete} style={{ marginTop: 12 }}>
          <div className="form-group"><label>Delete ID</label><input type="number" value={deleteId} onChange={(e) => setDeleteId(e.target.value)} required /></div>
          <button className="btn btn-danger" type="submit">Delete</button>
        </form>
      </div>

      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}

      {lookupResult && (
        <div className="result-card">
          <h3>Product Details</h3>
          <p><strong>ID:</strong> {lookupResult.id}</p>
          <p><strong>Name:</strong> {lookupResult.name}</p>
          <p><strong>Price:</strong> {lookupResult.price}</p>
          <p><strong>Stock:</strong> {lookupResult.stockQuantity}</p>
          <p><strong>Category:</strong> {lookupResult.category}</p>
          <p><strong>Active:</strong> {String(lookupResult.active)}</p>
        </div>
      )}

      {Array.isArray(products) && (
        <div className="section">
          <h2>All Products</h2>
          {products.length === 0 ? <p>No products found.</p> : (
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Price</th>
                  <th>Stock</th>
                  <th>Category</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id}>
                    <td>{p.id}</td>
                    <td>{p.name}</td>
                    <td>{p.price}</td>
                    <td>{p.stockQuantity}</td>
                    <td>{p.category}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {Array.isArray(searchResults) && (
        <div className="section">
          <h2>Search Results</h2>
          <p>{searchResults.length} product(s)</p>
        </div>
      )}

      {Array.isArray(categoryResults) && (
        <div className="section">
          <h2>Category Results</h2>
          <p>{categoryResults.length} product(s)</p>
        </div>
      )}
    </div>
  );
}
