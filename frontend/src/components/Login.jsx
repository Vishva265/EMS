import Doodle from './Doodle';
import { useState } from 'react';
import {
  ArrowRight,
  Shield,
  LockKeyhole,
  UsersRound,
  ArrowRightLeft,
  Eye,
  EyeOff,
} from 'lucide-react';
import { api, resetSecurityToken } from '../api';
import { Field } from './Modal';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      resetSecurityToken();
      await api('/auth/login', { method: 'POST', body: { username, password } });
      onLogin(await api('/auth/me'));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="login-page">
      <section className="login-story">
        <div className="brand">
          <div className="brand-mark">
            <span className="office-monogram">
              p<span>o</span>
            </span>
          </div>
          <div>
            PERSONNEL OFFICE<small>EMPLOYEE MANAGEMENT SYSTEM</small>
          </div>
        </div>
        <div className="login-story-content">
          <span className="eyebrow">CLARITY. ACCOUNTABILITY. CONTINUITY.</span>
          <h1>
            People at the heart.
            <br />
            <em>Precision in every record.</em>
          </h1>
          <p>
            A focused workspace for employee administration, department assignments, and a
            dependable record of every transition.
          </p>
          <Doodle variant="login" />
          <div className="login-features">
            <span>
              <UsersRound size={19} /> Employee records
            </span>
            <span>
              <ArrowRightLeft size={19} /> Department transfers
            </span>
            <span>
              <LockKeyhole size={19} /> Controlled access
            </span>
          </div>
        </div>
        <div className="login-story-footer">
          <span className="tricolour">
            <i />
            <i />
            <i />
          </span>{' '}
          IAIP 2026 · Internship assessment project
        </div>
      </section>
      <section className="login-form-area">
        <div className="login-form">
          <span className="login-kicker">
            <span /> PERSONNEL ADMINISTRATION
          </span>
          <h2>Welcome back.</h2>
          <p>Sign in to access your personnel workspace.</p>
          {error && (
            <div className="alert error" role="alert">
              {error}
            </div>
          )}
          <form onSubmit={submit}>
            <Field label="Username">
              <input
                autoFocus
                required
                autoComplete="username"
                value={username}
                maxLength={100}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
              />
            </Field>
            <Field label="Password">
              <div className="password-input">
                <input
                  required
                  autoComplete="current-password"
                  type={visible ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  aria-label={visible ? 'Hide password' : 'Show password'}
                  onClick={() => setVisible(!visible)}
                >
                  {visible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </Field>
            <button className="button primary login-submit" disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in to workspace'}
              <ArrowRight size={17} />
            </button>
          </form>
          <div className="login-security">
            <Shield size={16} />
            <p>
              Access is limited to provisioned accounts. Contact your project administrator for
              credentials.
            </p>
          </div>
        </div>
        <footer>Assessment prototype · Uses synthetic demonstration data</footer>
      </section>
    </div>
  );
}
