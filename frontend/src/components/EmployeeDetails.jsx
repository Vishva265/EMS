import { useEffect, useState } from 'react';
import { ArrowRight, ArrowRightLeft, Eye, EyeOff, Pencil, ShieldCheck } from 'lucide-react';
import { api, dateLabel, money } from '../api';
import Modal from './Modal';
import EmployeeAccount from './EmployeeAccount';

export default function EmployeeDetails({ employee, user, onClose, onEdit, onTransfer }) {
  const [data, setData] = useState(employee);
  const [history, setHistory] = useState([]);
  const [revealed, setRevealed] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    api(`/employees/${employee.id}/transfers`)
      .then(setHistory)
      .catch((e) => setError(e.message));
  }, [employee.id]);
  async function toggleIdentity() {
    try {
      setData(await api(`/employees/${employee.id}?reveal=${!revealed}`));
      setRevealed(!revealed);
    } catch (e) {
      setError(e.message);
    }
  }
  const row = (label, value) => (
    <div className="detail-row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
  return (
    <Modal
      wide
      title="Employee profile"
      subtitle="Personal and employment record"
      onClose={onClose}
    >
      <div className="modal-body">
        {error && (
          <div role="alert" className="alert error">
            {error}
          </div>
        )}
        <div className="profile-heading">
          <div className="avatar avatar-large">
            {data.fullName
              .split(' ')
              .map((n) => n[0])
              .slice(0, 2)
              .join('')}
          </div>
          <div>
            <h2>{data.fullName}</h2>
            <p>
              {data.employeeCode} <span className="dot-separator">·</span> {data.role}
            </p>
          </div>
          <span className={`status ${data.status.toLowerCase()}`}>{data.status}</span>
        </div>
        <div className="detail-columns">
          <section>
            <h3>Employment details</h3>
            <dl>
              {row('Employee code', data.employeeCode)}
              {row('Department', data.department)}
              {row('Designation', data.role)}
              {row('Joining date', dateLabel(data.joiningDate))}
              {row('Annual salary', money(data.annualSalary))}
            </dl>
          </section>
          <section>
            <h3>Personal details</h3>
            <dl>
              {row('Email', data.email)}
              {row('Mobile', data.mobile)}
              {row('Date of birth', dateLabel(data.dateOfBirth))}
              {row('Gender', data.gender)}
              {row('Address', data.address)}
            </dl>
          </section>
        </div>
        <section className="identity-box">
          <div className="split-row">
            <h3>
              <ShieldCheck size={17} /> Protected identity
            </h3>
            {user.role === 'Admin' && (
              <button className="text-button" onClick={toggleIdentity}>
                {revealed ? <EyeOff size={15} /> : <Eye size={15} />}
                {revealed ? 'Hide numbers' : 'Reveal numbers'}
              </button>
            )}
          </div>
          <dl className="identity-grid">
            {row('Aadhaar', data.aadhaar)}
            {row('PAN', data.pan)}
          </dl>
        </section>
        {user.role === 'Admin' && <EmployeeAccount employee={data} />}
        <section className="history-section">
          <h3>
            Department transfer history <span className="count-pill">{history.length}</span>
          </h3>
          {!history.length && (
            <div className="empty-history">No department transfers recorded.</div>
          )}
          {history.map((h) => (
            <article className="history-item" key={h.id}>
              <div className="history-dot">
                <ArrowRightLeft size={15} />
              </div>
              <div>
                <strong>
                  {h.oldDepartment} <ArrowRight size={14} /> {h.newDepartment}
                </strong>
                <p className="mono">
                  {h.oldEmployeeCode} → {h.newEmployeeCode}
                </p>
                <p>{h.reason}</p>
                <small>
                  {dateLabel(h.transferDate)} · Recorded by {h.performedBy}
                </small>
              </div>
            </article>
          ))}
        </section>
      </div>
      <footer className="modal-footer">
        <span>Internal record #{data.id}</span>
        <div className="button-row">
          <button className="button secondary" onClick={onClose}>
            Close
          </button>
          {user.role !== 'Employee' && (
            <button className="button secondary" onClick={() => onEdit(data)}>
              <Pencil size={15} /> Edit record
            </button>
          )}
          {user.role === 'Admin' && data.status === 'Active' && (
            <button className="button primary" onClick={() => onTransfer(data)}>
              <ArrowRightLeft size={15} /> Transfer
            </button>
          )}
        </div>
      </footer>
    </Modal>
  );
}
