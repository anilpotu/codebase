import React, { useState } from 'react';
import { sendConnectionRequest, acceptConnection, getConnections } from '../../api/socialApi';

const ConnectionsPage = () => {
  // Send Connection Request state
  const [sendForm, setSendForm] = useState({ userId: '', connectedUserId: '' });
  const [sendLoading, setSendLoading] = useState(false);
  const [sendError, setSendError] = useState('');
  const [sendResult, setSendResult] = useState(null);

  // Accept Connection state
  const [acceptId, setAcceptId] = useState('');
  const [acceptLoading, setAcceptLoading] = useState(false);
  const [acceptError, setAcceptError] = useState('');
  const [acceptResult, setAcceptResult] = useState(null);

  // List Connections state
  const [listUserId, setListUserId] = useState('');
  const [listStatus, setListStatus] = useState('ALL');
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [listResult, setListResult] = useState(null);

  const handleSendFormChange = (e) => {
    const { name, value } = e.target;
    setSendForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSendRequest = async (e) => {
    e.preventDefault();
    setSendLoading(true);
    setSendError('');
    try {
      const res = await sendConnectionRequest({
        userId: Number(sendForm.userId),
        connectedUserId: Number(sendForm.connectedUserId),
      });
      setSendResult(res.data);
    } catch (err) {
      setSendError(err.message);
    } finally {
      setSendLoading(false);
    }
  };

  const handleAcceptConnection = async (e) => {
    e.preventDefault();
    setAcceptLoading(true);
    setAcceptError('');
    try {
      const res = await acceptConnection(Number(acceptId));
      setAcceptResult(res.data);
    } catch (err) {
      setAcceptError(err.message);
    } finally {
      setAcceptLoading(false);
    }
  };

  const handleListConnections = async (e) => {
    e.preventDefault();
    setListLoading(true);
    setListError('');
    try {
      const res = await getConnections(
        Number(listUserId),
        listStatus === 'ALL' ? null : listStatus
      );
      setListResult(res.data);
    } catch (err) {
      setListError(err.message);
    } finally {
      setListLoading(false);
    }
  };

  return (
    <div>
      <h1>Connections</h1>

      <div className="section">
        <h2>Send Connection Request</h2>
        <form className="form" onSubmit={handleSendRequest}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              name="userId"
              value={sendForm.userId}
              onChange={handleSendFormChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Connected User ID</label>
            <input
              type="number"
              name="connectedUserId"
              value={sendForm.connectedUserId}
              onChange={handleSendFormChange}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={sendLoading}>
            {sendLoading ? 'Sending...' : 'Send Request'}
          </button>
        </form>
        {sendError && <div className="error">{sendError}</div>}
        {sendResult && (
          <div className="success">
            Connection request sent (ID: {sendResult.id}, Status: {sendResult.status})
          </div>
        )}
      </div>

      <div className="section">
        <h2>Accept Connection</h2>
        <form className="form" onSubmit={handleAcceptConnection}>
          <div className="form-group">
            <label>Connection ID</label>
            <input
              type="number"
              value={acceptId}
              onChange={(e) => setAcceptId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={acceptLoading}>
            {acceptLoading ? 'Accepting...' : 'Accept Connection'}
          </button>
        </form>
        {acceptError && <div className="error">{acceptError}</div>}
        {acceptResult && (
          <div className="success">
            Connection accepted (ID: {acceptResult.id}, Status: {acceptResult.status})
          </div>
        )}
      </div>

      <div className="section">
        <h2>List Connections</h2>
        <form className="form" onSubmit={handleListConnections}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={listUserId}
              onChange={(e) => setListUserId(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Status</label>
            <select value={listStatus} onChange={(e) => setListStatus(e.target.value)}>
              <option value="ALL">ALL</option>
              <option value="PENDING">PENDING</option>
              <option value="ACCEPTED">ACCEPTED</option>
            </select>
          </div>
          <button className="btn" type="submit" disabled={listLoading}>
            {listLoading ? 'Loading...' : 'List Connections'}
          </button>
        </form>
        {listError && <div className="error">{listError}</div>}
        {listResult && (
          <div>
            {listResult.length === 0 && <p>No connections found.</p>}
            {listResult.length > 0 && (
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>User ID</th>
                    <th>Connected User ID</th>
                    <th>Status</th>
                    <th>Created At</th>
                  </tr>
                </thead>
                <tbody>
                  {listResult.map((conn) => (
                    <tr key={conn.id}>
                      <td>{conn.id}</td>
                      <td>{conn.userId}</td>
                      <td>{conn.connectedUserId}</td>
                      <td>{conn.status}</td>
                      <td>{conn.createdAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default ConnectionsPage;
