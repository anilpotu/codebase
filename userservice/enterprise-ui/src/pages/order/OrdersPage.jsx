import { useState } from 'react';
import { cancelOrder, createOrder, getMyOrders, getOrderById } from '../../api/orderApi';

const unwrap = (res) => res?.data?.data ?? res?.data;

export default function OrdersPage() {
  const [createForm, setCreateForm] = useState({ userId: '', productId: '', quantity: '', price: '' });
  const [createdOrder, setCreatedOrder] = useState(null);
  const [myOrders, setMyOrders] = useState(null);
  const [lookupId, setLookupId] = useState('');
  const [lookupOrder, setLookupOrder] = useState(null);
  const [cancelId, setCancelId] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const resetFeedback = () => {
    setError('');
    setMessage('');
  };

  const onCreate = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const payload = {
        userId: Number(createForm.userId),
        items: [
          {
            productId: Number(createForm.productId),
            quantity: Number(createForm.quantity),
            price: createForm.price ? Number(createForm.price) : undefined,
          },
        ],
      };
      const res = await createOrder(payload);
      setCreatedOrder(unwrap(res));
      setMessage('Order created');
    } catch (err) {
      setError(err.message);
    }
  };

  const onGetMyOrders = async () => {
    resetFeedback();
    try {
      const res = await getMyOrders();
      setMyOrders(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onGetById = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await getOrderById(Number(lookupId));
      setLookupOrder(unwrap(res));
    } catch (err) {
      setError(err.message);
    }
  };

  const onCancel = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await cancelOrder(Number(cancelId));
      setLookupOrder(unwrap(res));
      setMessage(`Order ${cancelId} cancelled`);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1>Order Service</h1>

      <div className="section">
        <h2>Create Order (single item quick form)</h2>
        <form className="form" onSubmit={onCreate}>
          <div className="form-group"><label>User ID</label><input type="number" value={createForm.userId} onChange={(e) => setCreateForm((s) => ({ ...s, userId: e.target.value }))} required /></div>
          <div className="form-group"><label>Product ID</label><input type="number" value={createForm.productId} onChange={(e) => setCreateForm((s) => ({ ...s, productId: e.target.value }))} required /></div>
          <div className="form-group"><label>Quantity</label><input type="number" value={createForm.quantity} onChange={(e) => setCreateForm((s) => ({ ...s, quantity: e.target.value }))} required /></div>
          <div className="form-group"><label>Price (optional)</label><input type="number" step="0.01" value={createForm.price} onChange={(e) => setCreateForm((s) => ({ ...s, price: e.target.value }))} /></div>
          <button className="btn" type="submit">Create Order</button>
        </form>
      </div>

      <div className="section">
        <h2>Read Orders</h2>
        <button className="btn" type="button" onClick={onGetMyOrders}>Get My Orders</button>
        <form className="form" onSubmit={onGetById} style={{ marginTop: 12 }}>
          <div className="form-group"><label>Order ID</label><input type="number" value={lookupId} onChange={(e) => setLookupId(e.target.value)} required /></div>
          <button className="btn" type="submit">Get By ID</button>
        </form>
      </div>

      <div className="section">
        <h2>Cancel Order</h2>
        <form className="form" onSubmit={onCancel}>
          <div className="form-group"><label>Order ID</label><input type="number" value={cancelId} onChange={(e) => setCancelId(e.target.value)} required /></div>
          <button className="btn btn-danger" type="submit">Cancel Order</button>
        </form>
      </div>

      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}

      {createdOrder && (
        <div className="result-card">
          <h3>Created Order</h3>
          <p><strong>ID:</strong> {createdOrder.id}</p>
          <p><strong>User ID:</strong> {createdOrder.userId}</p>
          <p><strong>Status:</strong> {createdOrder.status}</p>
          <p><strong>Total:</strong> {createdOrder.totalAmount}</p>
        </div>
      )}

      {lookupOrder && (
        <div className="result-card">
          <h3>Order Details</h3>
          <p><strong>ID:</strong> {lookupOrder.id}</p>
          <p><strong>User ID:</strong> {lookupOrder.userId}</p>
          <p><strong>Status:</strong> {lookupOrder.status}</p>
          <p><strong>Total:</strong> {lookupOrder.totalAmount}</p>
          <p><strong>Created:</strong> {lookupOrder.createdAt}</p>
        </div>
      )}

      {Array.isArray(myOrders) && (
        <div className="section">
          <h2>My Orders</h2>
          {myOrders.length === 0 ? <p>No orders found.</p> : (
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Status</th>
                  <th>Total</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {myOrders.map((o) => (
                  <tr key={o.id}>
                    <td>{o.id}</td>
                    <td>{o.status}</td>
                    <td>{o.totalAmount}</td>
                    <td>{o.createdAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
