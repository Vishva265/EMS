import { useState } from 'react';
import {
  ArrowRightLeft,
  BookOpen,
  ChevronRight,
  LogOut,
  Menu,
  Shield,
  UsersRound,
} from 'lucide-react';

export default function Workspace({ user, section, setSection, onGuide, logout, children }) {
  const [mobileNav, setMobileNav] = useState(false);
  const transferMode = section === 'transfers';
  return (
    <div className="app-shell directory-preview">
      {mobileNav && <div className="nav-scrim" onClick={() => setMobileNav(false)} />}
      <aside className={`sidebar ${mobileNav ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-mark">
            <span className="office-monogram">
              p<span>o</span>
            </span>
          </div>
          <div>
            PERSONNEL<small>OFFICE</small>
          </div>
        </div>
        <div className="sidebar-section-label">WORKSPACE</div>
        <nav aria-label="Main navigation">
          <button
            className={section === 'directory' ? 'selected' : ''}
            onClick={() => {
              setSection('directory');
              setMobileNav(false);
            }}
          >
            <span className="nav-index">01</span>
            {user.role === 'Employee' ? 'My employee record' : 'Employee directory'}
            <ChevronRight size={15} />
          </button>
          {user.role !== 'Employee' && (
            <button
              className={transferMode ? 'selected' : ''}
              onClick={() => {
                setSection('transfers');
                setMobileNav(false);
              }}
            >
              <span className="nav-index">02</span>
              Department transfers
              <ChevronRight size={15} />
            </button>
          )}
        </nav>
        <div className="sidebar-note">
          <div className="mini-grid" />
          <span className="eyebrow">ORDER IN EVERY DETAIL</span>
          <p>
            One record.
            <br />
            Every transition accounted for.
          </p>
        </div>
        <button className="sidebar-guide" onClick={() => onGuide()}>
          <BookOpen size={17} />
          Workspace guide
          <ChevronRight size={14} />
        </button>
        <div className="sidebar-footer">
          <span className="tricolour">
            <i />
            <i />
            <i />
          </span>
          <span>
            IAIP 2026<small>Internship assessment project</small>
          </span>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <button
              className="icon-button mobile-menu"
              aria-label="Open navigation"
              onClick={() => setMobileNav(true)}
            >
              <Menu size={20} />
            </button>
            <span>Personnel administration</span>
            <ChevronRight size={13} />
            <strong>{transferMode ? 'Transfers' : 'Directory'}</strong>
          </div>
          <div className="topbar-right">
            <span className="workspace-badge">
              <span />
              Personnel workspace
            </span>
            <span className="topbar-divider" />
            <div className="user-avatar">{user.username.slice(0, 2).toUpperCase()}</div>
            <div className="user-label">
              <strong>{user.username}</strong>
              <span>{user.role}</span>
            </div>
            <button className="icon-button" aria-label="Sign out" title="Sign out" onClick={logout}>
              <LogOut size={17} />
            </button>
          </div>
        </header>
        {children}
        <footer className="workspace-footer">
          <span>PERSONNEL OFFICE</span>
          <span>IAIP 2026 · Assessment prototype</span>
        </footer>
      </div>
    </div>
  );
}
