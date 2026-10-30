import { useState } from 'react';
import { LockKeyhole, Save } from 'lucide-react';
import { api } from '../api';
import Modal, { Field } from './Modal';

const blank = {
  fullName: '',
  email: '',
  mobile: '',
  aadhaar: '',
  pan: '',
  dateOfBirth: '',
  gender: '',
  address: '',
  departmentId: '',
  roleId: '',
  joiningDate: '',
  annualSalary: '',
  status: 'Active',
};
const today = new Date().toLocaleDateString('en-CA');

export default function EmployeeForm({ employee, user, departments, roles, onClose, onSaved }) {
  const [form, setForm] = useState(employee || blank);
  const [error, setError] = useState('');
  const [fields, setFields] = useState({});
  const [saving, setSaving] = useState(false);
  const manager = user.role === 'Manager';
  function change(name, value) {
    setForm((old) => ({ ...old, [name]: value }));
    setFields((old) => ({ ...old, [name]: '' }));
  }
  const input = (name, type = 'text', extra = {}) => (
    <input
      type={type}
      name={name}
      value={form[name] ?? ''}
      required
      onChange={(e) => change(name, e.target.value)}
      {...extra}
    />
  );
  async function submit(event) {
    event.preventDefault();
    setError('');
    setFields({});
    setSaving(true);
    try {
      const employment = {
        roleId: Number(form.roleId),
        joiningDate: form.joiningDate,
        annualSalary: Number(form.annualSalary),
        version: form.version,
      };
      const body = manager
        ? employment
        : {
            ...employment,
            fullName: form.fullName.trim(),
            email: form.email.trim(),
            mobile: form.mobile,
            aadhaar: form.aadhaar.replace(/\D/g, ''),
            pan: form.pan,
            dateOfBirth: form.dateOfBirth,
            gender: form.gender,
            address: form.address.trim(),
            departmentId: Number(form.departmentId),
            status: form.status,
          };
      const path = employee
        ? `/employees/${employee.id}${manager ? '/employment' : ''}`
        : '/employees';
      await api(path, { method: employee ? (manager ? 'PATCH' : 'PUT') : 'POST', body });
      onSaved(employee ? 'Employee details updated.' : 'Employee registered. Open their profile and use Employee login to create sign-in credentials.');
    } catch (e) {
      setError(e.message);
      setFields(e.fields);
    } finally {
      setSaving(false);
    }
  }
  return (
    <Modal
      wide
      title={employee ? 'Edit employee' : 'Register employee'}
      subtitle={
        employee
          ? `${employee.employeeCode} · ${employee.fullName}`
          : 'Create a personnel record. The employee code is assigned automatically.'
      }
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <div className="modal-body">
          {error && (
            <div className="alert error" role="alert">
              {error}
            </div>
          )}
          {!manager && (
            <>
              <div className="section-label">
                <span>01</span> Personal information
              </div>
              <div className="form-grid">
                <Field label="Full name *" error={fields.fullName}>
                  {input('fullName', 'text', {
                    pattern: '[A-Za-z ]+',
                    maxLength: 100,
                    placeholder: 'e.g. Aarav Sharma',
                    autoComplete: 'name',
                  })}
                </Field>
                <Field label="Email address *" error={fields.email}>
                  {input('email', 'email', {
                    maxLength: 160,
                    placeholder: 'name@example.com',
                    autoComplete: 'email',
                  })}
                </Field>
                <Field label="Mobile number *" error={fields.mobile}>
                  {input('mobile', 'tel', {
                    pattern: '[0-9]{10}',
                    maxLength: 10,
                    placeholder: '10-digit mobile number',
                  })}
                </Field>
                <Field label="Date of birth *" error={fields.dateOfBirth}>
                  {input('dateOfBirth', 'date', { max: today })}
                </Field>
                <Field label="Gender *" error={fields.gender}>
                  <select
                    required
                    value={form.gender}
                    onChange={(e) => change('gender', e.target.value)}
                  >
                    <option value="">Select gender</option>
                    {['Male', 'Female', 'Other'].map((g) => (
                      <option key={g}>{g}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Address *" error={fields.address}>
                  {input('address', 'text', { maxLength: 500, placeholder: 'Street, city, state' })}
                </Field>
              </div>
              <div className="section-label">
                <span>02</span> Identity information <LockKeyhole size={14} />
              </div>
              <p className="form-note">
                Encrypted in storage. Restricted to authorized administrators.
              </p>
              <div className="form-grid">
                <Field label="Aadhaar number *" error={fields.aadhaar}>
                  <input
                    required
                    inputMode="numeric"
                    placeholder="XXXX-XXXX-XXXX"
                    pattern="[0-9]{4}-[0-9]{4}-[0-9]{4}"
                    maxLength={14}
                    value={
                      form.aadhaar
                        .replace(/\D/g, '')
                        .match(/.{1,4}/g)
                        ?.join('-') || ''
                    }
                    onChange={(e) =>
                      change('aadhaar', e.target.value.replace(/\D/g, '').slice(0, 12))
                    }
                  />
                </Field>
                <Field label="PAN number *" error={fields.pan}>
                  <input
                    required
                    value={form.pan}
                    placeholder="ABCDE1234F"
                    pattern="[A-Z]{5}[0-9]{4}[A-Z]"
                    maxLength={10}
                    onChange={(e) => change('pan', e.target.value.toUpperCase())}
                  />
                </Field>
              </div>
            </>
          )}
          <div className="section-label">
            <span>{manager ? '01' : '03'}</span> Employment information
          </div>
          <div className="form-grid">
            {!manager && (
              <Field label="Department *" error={fields.departmentId}>
                <select
                  required
                  disabled={!!employee}
                  value={form.departmentId}
                  onChange={(e) => change('departmentId', e.target.value)}
                >
                  <option value="">Select department</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
                {employee && <small>Use a department transfer to change this.</small>}
              </Field>
            )}
            <Field label="Designation *" error={fields.roleId}>
              <select
                required
                value={form.roleId}
                onChange={(e) => change('roleId', e.target.value)}
              >
                <option value="">Select designation</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Joining date *" error={fields.joiningDate}>
              {input('joiningDate', 'date', { min: form.dateOfBirth })}
            </Field>
            <Field label="Annual salary (INR) *" error={fields.annualSalary}>
              {input('annualSalary', 'number', {
                min: '0.01',
                max: '999999999999.99',
                step: '0.01',
                placeholder: 'e.g. 600000',
              })}
            </Field>
            {!employee && (
              <Field label="Employee status *">
                <select value={form.status} onChange={(e) => change('status', e.target.value)}>
                  <option>Active</option>
                  <option>Inactive</option>
                </select>
              </Field>
            )}
          </div>
        </div>
        <footer className="modal-footer">
          <span>* Required fields</span>
          <div className="button-row">
            <button type="button" className="button secondary" onClick={onClose}>
              Cancel
            </button>
            <button className="button primary" disabled={saving}>
              <Save size={16} />
              {saving ? 'Saving…' : employee ? 'Save changes' : 'Register employee'}
            </button>
          </div>
        </footer>
      </form>
    </Modal>
  );
}
