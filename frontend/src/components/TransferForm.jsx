import { useState } from 'react';
import { ArrowRightLeft, Info } from 'lucide-react';
import { api } from '../api';
import Modal, { Field } from './Modal';

export default function TransferForm({ employee, departments, onClose, onSaved }) {
  const [form, setForm] = useState({
    newDepartmentId: '',
    transferDate: new Date().toLocaleDateString('en-CA'),
    reason: '',
  });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const updated = await api(`/employees/${employee.id}/transfers`, {
        method: 'POST',
        body: { ...form, newDepartmentId: Number(form.newDepartmentId), version: employee.version },
      });
      onSaved(`Transfer completed. New employee code: ${updated.employeeCode}.`);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }
  return (
    <Modal
      title="Department transfer"
      subtitle="Move an employee and preserve their service history."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <div className="modal-body">
          {error && (
            <div className="alert error" role="alert">
              {error}
            </div>
          )}
          <div className="transfer-summary">
            <span className="eyebrow">CURRENT RECORD</span>
            <h3>{employee.fullName}</h3>
            <p>
              <span className="mono">{employee.employeeCode}</span>
              <span>·</span>
              {employee.department}
            </p>
          </div>
          <div className="form-grid single">
            <Field label="New department *">
              <select
                required
                value={form.newDepartmentId}
                onChange={(e) => setForm({ ...form, newDepartmentId: e.target.value })}
              >
                <option value="">Choose destination department</option>
                {departments
                  .filter((d) => d.id !== employee.departmentId)
                  .map((d) => (
                    <option value={d.id} key={d.id}>
                      {d.name}
                    </option>
                  ))}
              </select>
            </Field>
            <Field label="Transfer date *">
              <input
                required
                type="date"
                min={employee.joiningDate}
                max={new Date().toLocaleDateString('en-CA')}
                value={form.transferDate}
                onChange={(e) => setForm({ ...form, transferDate: e.target.value })}
              />
            </Field>
            <Field label="Transfer reason *">
              <textarea
                required
                rows={3}
                maxLength={500}
                placeholder="Briefly describe the reason for this transfer"
                value={form.reason}
                onChange={(e) => setForm({ ...form, reason: e.target.value })}
              />
            </Field>
          </div>
          <div className="info-box">
            <Info size={18} />
            <p>
              A new department code will be assigned automatically. The internal employee ID and
              previous transfer records remain linked.
            </p>
          </div>
        </div>
        <footer className="modal-footer">
          <span>* Required fields</span>
          <div className="button-row">
            <button type="button" className="button secondary" onClick={onClose}>
              Cancel
            </button>
            <button className="button primary" disabled={saving}>
              <ArrowRightLeft size={16} />
              {saving ? 'Transferring…' : 'Confirm transfer'}
            </button>
          </div>
        </footer>
      </form>
    </Modal>
  );
}
