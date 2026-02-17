import { useState } from 'react';
import { createHealthRecord, getHealthRecordByUserId } from '../../api/healthApi';

export default function HealthRecordsPage() {
  // Create/Update form state
  const [formData, setFormData] = useState({
    userId: '',
    bloodType: '',
    heightCm: '',
    weightKg: '',
    allergies: '',
    conditions: '',
    medications: '',
    lastCheckupDate: '',
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createResult, setCreateResult] = useState(null);

  // Lookup state
  const [lookupUserId, setLookupUserId] = useState('');
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState('');
  const [lookupResult, setLookupResult] = useState(null);

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    setCreateError('');
    setCreateResult(null);
    try {
      const payload = {
        userId: Number(formData.userId),
        bloodType: formData.bloodType,
        heightCm: formData.heightCm ? Number(formData.heightCm) : undefined,
        weightKg: formData.weightKg ? Number(formData.weightKg) : undefined,
        allergies: formData.allergies,
        conditions: formData.conditions,
        medications: formData.medications,
        lastCheckupDate: formData.lastCheckupDate,
      };
      const res = await createHealthRecord(payload);
      setCreateResult(res.data);
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleLookup = async (e) => {
    e.preventDefault();
    setLookupLoading(true);
    setLookupError('');
    setLookupResult(null);
    try {
      const res = await getHealthRecordByUserId(Number(lookupUserId));
      setLookupResult(res.data);
    } catch (err) {
      setLookupError(err.message);
    } finally {
      setLookupLoading(false);
    }
  };

  return (
    <div>
      <h1>Health Records</h1>

      <div className="section">
        <h2>Create / Update Health Record</h2>
        <form className="form" onSubmit={handleCreate}>
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
            <label>Blood Type</label>
            <input
              type="text"
              name="bloodType"
              value={formData.bloodType}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Height (cm)</label>
            <input
              type="number"
              name="heightCm"
              value={formData.heightCm}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Weight (kg)</label>
            <input
              type="number"
              name="weightKg"
              value={formData.weightKg}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Allergies</label>
            <textarea
              name="allergies"
              value={formData.allergies}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Conditions</label>
            <textarea
              name="conditions"
              value={formData.conditions}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Medications</label>
            <textarea
              name="medications"
              value={formData.medications}
              onChange={handleFormChange}
            />
          </div>
          <div className="form-group">
            <label>Last Checkup Date</label>
            <input
              type="date"
              name="lastCheckupDate"
              value={formData.lastCheckupDate}
              onChange={handleFormChange}
            />
          </div>
          <button className="btn" type="submit" disabled={createLoading}>
            {createLoading ? 'Saving...' : 'Save Health Record'}
          </button>
        </form>
        {createError && <p className="error">{createError}</p>}
        {createResult && (
          <p className="success">Health record saved successfully for user {createResult.userId}.</p>
        )}
      </div>

      <div className="section">
        <h2>Lookup by User ID</h2>
        <form className="form" onSubmit={handleLookup}>
          <div className="form-group">
            <label>User ID</label>
            <input
              type="number"
              value={lookupUserId}
              onChange={(e) => setLookupUserId(e.target.value)}
              required
            />
          </div>
          <button className="btn" type="submit" disabled={lookupLoading}>
            {lookupLoading ? 'Loading...' : 'Lookup'}
          </button>
        </form>
        {lookupError && <p className="error">{lookupError}</p>}
        {lookupResult && (
          <div className="result-card">
            <h3>Health Record</h3>
            <p><strong>User ID:</strong> {lookupResult.userId}</p>
            <p><strong>Blood Type:</strong> {lookupResult.bloodType}</p>
            <p><strong>Height:</strong> {lookupResult.heightCm} cm</p>
            <p><strong>Weight:</strong> {lookupResult.weightKg} kg</p>
            <p><strong>Allergies:</strong> {lookupResult.allergies}</p>
            <p><strong>Conditions:</strong> {lookupResult.conditions}</p>
            <p><strong>Medications:</strong> {lookupResult.medications}</p>
            <p><strong>Last Checkup:</strong> {lookupResult.lastCheckupDate}</p>
            <p><strong>Created At:</strong> {lookupResult.createdAt}</p>
            <p><strong>Updated At:</strong> {lookupResult.updatedAt}</p>
          </div>
        )}
      </div>
    </div>
  );
}
