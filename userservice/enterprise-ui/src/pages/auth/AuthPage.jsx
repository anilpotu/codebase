import { useState } from 'react';
import { login, logout, refreshToken, register, validateToken } from '../../api/authApi';
import { setAuthToken } from '../../api/axios';

const unwrap = (res) => res?.data?.data ?? res?.data;

export default function AuthPage() {
  const [registerForm, setRegisterForm] = useState({ username: '', email: '', password: '' });
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [refreshValue, setRefreshValue] = useState('');
  const [validateValue, setValidateValue] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [tokens, setTokens] = useState(null);

  const resetFeedback = () => {
    setError('');
    setMessage('');
  };

  const onRegister = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await register(registerForm);
      setMessage(res.data?.message || 'Registered successfully');
    } catch (err) {
      setError(err.message);
    }
  };

  const onLogin = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await login(loginForm);
      const payload = unwrap(res);
      setTokens(payload);
      if (payload?.accessToken) {
        setAuthToken(payload.accessToken);
      }
      setMessage(res.data?.message || 'Login successful');
    } catch (err) {
      setError(err.message);
    }
  };

  const onRefresh = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await refreshToken(refreshValue);
      const payload = unwrap(res);
      setTokens((prev) => ({ ...prev, ...payload }));
      if (payload?.accessToken) {
        setAuthToken(payload.accessToken);
      }
      setMessage(res.data?.message || 'Token refreshed');
    } catch (err) {
      setError(err.message);
    }
  };

  const onValidate = async (e) => {
    e.preventDefault();
    resetFeedback();
    try {
      const res = await validateToken(validateValue);
      const valid = unwrap(res);
      setMessage(`Token valid: ${String(valid)}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const onLogout = async () => {
    resetFeedback();
    try {
      await logout();
      setAuthToken(null);
      setTokens(null);
      setMessage('Logged out and local token cleared');
    } catch (err) {
      setAuthToken(null);
      setTokens(null);
      setMessage(`Token cleared locally (server logout error: ${err.message})`);
    }
  };

  return (
    <div>
      <h1>Auth Service</h1>

      <div className="section">
        <h2>Register</h2>
        <form className="form" onSubmit={onRegister}>
          <div className="form-group">
            <label>Username</label>
            <input value={registerForm.username} onChange={(e) => setRegisterForm((s) => ({ ...s, username: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={registerForm.email} onChange={(e) => setRegisterForm((s) => ({ ...s, email: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={registerForm.password} onChange={(e) => setRegisterForm((s) => ({ ...s, password: e.target.value }))} required />
          </div>
          <button className="btn" type="submit">Register</button>
        </form>
      </div>

      <div className="section">
        <h2>Login</h2>
        <form className="form" onSubmit={onLogin}>
          <div className="form-group">
            <label>Username</label>
            <input value={loginForm.username} onChange={(e) => setLoginForm((s) => ({ ...s, username: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={loginForm.password} onChange={(e) => setLoginForm((s) => ({ ...s, password: e.target.value }))} required />
          </div>
          <button className="btn" type="submit">Login</button>
        </form>
      </div>

      <div className="section">
        <h2>Refresh / Validate / Logout</h2>
        <form className="form" onSubmit={onRefresh}>
          <div className="form-group">
            <label>Refresh Token</label>
            <input value={refreshValue} onChange={(e) => setRefreshValue(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Refresh Token</button>
        </form>
        <form className="form" onSubmit={onValidate} style={{ marginTop: 12 }}>
          <div className="form-group">
            <label>Access Token</label>
            <input value={validateValue} onChange={(e) => setValidateValue(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Validate Token</button>
        </form>
        <button className="btn btn-danger" style={{ marginTop: 12 }} onClick={onLogout} type="button">Logout</button>
      </div>

      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}
      {tokens && (
        <div className="result-card">
          <h3>Current Tokens</h3>
          <p><strong>Access:</strong> {tokens.accessToken || 'N/A'}</p>
          <p><strong>Refresh:</strong> {tokens.refreshToken || 'N/A'}</p>
          <p><strong>Type:</strong> {tokens.tokenType || 'Bearer'}</p>
        </div>
      )}
    </div>
  );
}
