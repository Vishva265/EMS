import { useEffect, useState } from 'react';
import { api } from '../api';
import { Field } from './Modal';

export default function EmployeeAccount({ employee }) {
  const [account, setAccount] = useState(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState(false);
  useEffect(() => {
    api(`/employees/${employee.id}/account`)
      .then(setAccount)
      .catch((e) => setError(e.message));
  }, [employee.id]);
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      setAccount(
        await api(`/employees/${employee.id}/account`, {
          method: 'POST',
          body: { username, password },
        }),
      );
      setCreated(true);
      setPassword('');
      setVisible(false);
    } catch (e) {
      setError([e.message, ...Object.values(e.fields || {})].join(' '));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="employee-account">
      <h3>Employee login</h3>
      {error && (
        <div className="alert error" role="alert">
          {error}
        </div>
      )}
      {account?.exists ? (
        <>
          {created && (
            <p className="alert success" role="status">
              Login created. Give the employee the username below and the initial password you just
              set.
            </p>
          )}
          <dl>
            <div className="detail-row">
              <dt>Username</dt>
              <dd>{account.username}</dd>
            </div>
          </dl>
          <p className="form-note">
            {employee.status === 'Inactive'
              ? 'Login is blocked while this employee is inactive.'
              : 'This account can sign in and view only this employee’s profile and history.'}{' '}
            Passwords are securely hashed and cannot be displayed or recovered.
          </p>
        </>
      ) : account ? (
        employee.status !== 'Active' ? (
          <p>Activate this employee before creating a login.</p>
        ) : (
          <form onSubmit={submit}>
            <p className="account-description">
              Create sign-in credentials for this employee. Share the initial password with them
              privately.
            </p>
            <div className="form-grid">
              <Field label="Login username *">
                <input
                  required
                  autoComplete="off"
                  minLength={3}
                  maxLength={50}
                  pattern="[A-Za-z0-9][A-Za-z0-9._-]{2,49}"
                  placeholder="e.g. rahul.sharma"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </Field>
              <Field label="Initial password *">
                <input
                  required
                  autoComplete="new-password"
                  type={visible ? 'text' : 'password'}
                  minLength={12}
                  maxLength={64}
                  placeholder="At least 12 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </Field>
            </div>
            <div className="account-actions">
              <button type="button" className="text-button" onClick={() => setVisible(!visible)}>
                {visible ? 'Hide password' : 'Show password'}
              </button>
              <button className="button primary" disabled={busy}>
                {busy ? 'Creating…' : 'Create employee login'}
              </button>
            </div>
          </form>
        )
      ) : (
        !error && <p>Checking login account…</p>
      )}
    </section>
  );
}
