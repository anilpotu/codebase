import { useState } from 'react';
import { recordVital, getVitalsByUserId, getLatestVital } from '../../api/healthApi';

export default function VitalsPage() {
  // Record Vital form state
  const [formData, setFormData] = useState({
    userId: '',
    heartRate: '',
    systolicBp: '',
    diastolicBp: '',
    temperatureCelsius: '',
    oxygenSaturation: '',
  });
  const [recordLoading, setRecordLoading] = useState(false);
  const [recordError, setRecordError] = useState('');
  const [recordResult, setRecordResult] = useState(null);

  // List Vitals state
  const [listUserId, setListUserId] = useState('');
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [listResult, setListResult] = useState(null);

  // Latest Vital state
  const [latestUserId, setLatestUserId] = useState('');
  const [latestLoading, setLatestLoading] = useState(false);
  const [latestError, setLatestError] = useState('');
  const [latestResult, setLatestResult] = useState(null);

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleRecord = async (e) => {
    e.preventDefault();
    setRecordLoading(true);
    setRecordError('');
    setRecordResult(null);
    try {
      const payload = {
        userId: Number(formData.userId),
        heartRate: formData.heartRate ? Number(formData.heartRate) : undefined,
        systolicBp: formData.systolicBp ? Number(formData.systolicBp) : undefined,
        diastolicBp: formData.diastolicBp ? Number(formData.diastolicBp) : undefined,
        temperatureCelsius: formData.temperatureCelsius ? Number(formData.temperatureCelsius) : undefined,
        oxygenSaturation: formData.oxygenSaturation ? Number(formData.oxygenSaturation) : undefined,
      };
      const res = await recordVital(payload);
      setRecordResult(res.data);
    } catch (err) {
      setRecordError(err.message);
    } finally {
      setRecordLoading(false);
    }
  };

  const handleListVitals = async (e) => {
    e.preventDefault();
    setListLoading(true);
    setListError('');
    setListResult(null);
    try {
      const res = await getVitalsByUserId(Number(listUserId));
      setListResult(res.data);
    } catch (err) {
      setListError(err.message);
    } finally {
      setListLoading(false);
    }
  };

  const handleLatestVital = async (e) => {
    e.preventDefault();
    setLatestLoading(true);
    setLatestError('');
    setLatestResult(null);
    try {
      const res = await getLatestVital(Number(latestUserId));
      setLatestResult(res.data);
    } catch (err) {
      setLatestError(err.message);
    } finally {
      setLatestLoading(false);
    }
  };

  return (
    <div>
      <h1>Vitals</h1>

      <div className="section">
        <h2>Record Vital</h2>
        <form className="form" onSubmit={handleRecord}>
          <div className="form-group">
            <label>User ID *</label>
            <input
              type="number"
              name="userId"
              value={formData.userId}
              onChange={handleFormChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Heart Rate (bpm)</label>
            <input
              type="number"
              name="heartRate"
              value={formData.heartRate}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Systolic BP</label>
            <input
              type="number"
              name="systolicBp"
              value={formData.systolicBp}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Diastolic BP</label>
            <input
              type="number"
              name="diastolicBp"
              value={formData.diastolicBp}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Temperature (C)</label>
            <input
              type="number"
              name="temperatureCelsius"
              value={formData.temperatureCelsius}
              onChange={handleFormChange}
              step="0.1"
            />
          </div>
          <div className="form-group">
            <label>Oxygen Saturation (%)</label>
            <input
              type="number"
              name="oxygenSaturation"
              value={formData.oxygenSaturation}
              onChange={handleFormChange}
            />
          </div>
          <button className="btn" type="submit" disabled={recordLoading}>
            {recordLoading ? 'Recording...' : 'Record Vital'}
          </button>
        </form>
        {recordError && <p className="error">{recordError}</p>}
        {recordResult && (
          <p className="success">Vital recorded successfully for user {recordResult.userId}.</p>
        )}
      </div>

      <div className="section">
        <h2>List Vitals by User</h2>
        <form className="form" onSubmit={handleListVitals}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={listUserId}
              onChange={(e) => setListUserId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={listLoading}>
            {listLoading ? 'Loading...' : 'List Vitals'}
          </button>
        </form>
        {listError && <p className="error">{listError}</p>}
        {listResult && listResult.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>Heart Rate</th>
                <th>Systolic BP</th>
                <th>Diastolic BP</th>
                <th>Temp (C)</th>
                <th>O2 Sat (%)</th>
                <th>Recorded At</th>
              </tr>
            </thead>
            <tbody>
              {listResult.map((vital, idx) => (
                <tr key={idx}>
                  <td>{vital.heartRate}</td>
                  <td>{vital.systolicBp}</td>
                  <td>{vital.diastolicBp}</td>
                  <td>{vital.temperatureCelsius}</td>
                  <td>{vital.oxygenSaturation}</td>
                  <td>{vital.recordedAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {listResult && listResult.length === 0 && (
          <p>No vitals found for this user.</p>
        )}
      </div>

      <div className="section">
        <h2>Get Latest Vital</h2>
        <form className="form" onSubmit={handleLatestVital}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={latestUserId}
              onChange={(e) => setLatestUserId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={latestLoading}>
            {latestLoading ? 'Loading...' : 'Get Latest'}
          </button>
        </form>
        {latestError && <p className="error">{latestError}</p>}
        {latestResult && (
          <div className="result-card">
            <h3>Latest Vital</h3>
            <p><strong>User ID:</strong> {latestResult.userId}</p>
            <p><strong>Heart Rate:</strong> {latestResult.heartRate} bpm</p>
            <p><strong>Systolic BP:</strong> {latestResult.systolicBp}</p>
            <p><strong>Diastolic BP:</strong> {latestResult.diastolicBp}</p>
            <p><strong>Temperature:</strong> {latestResult.temperatureCelsius} C</p>
            <p><strong>Oxygen Saturation:</strong> {latestResult.oxygenSaturation}%</p>
            <p><strong>Recorded At:</strong> {latestResult.recordedAt}</p>
          </div>
        )}
      </div>
    </div>
  );
}
