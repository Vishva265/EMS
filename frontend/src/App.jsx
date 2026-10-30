import { useCallback, useEffect, useState } from 'react';
import {
  ArrowRightLeft,
  Check,
  ChevronDown,
  CircleHelp,
  Plus,
  Search,
  Shield,
  SlidersHorizontal,
  UsersRound,
  X,
} from 'lucide-react';
import { api, setSessionHandler } from './api';
import Login from './components/Login';
import DirectoryWelcome from './components/DirectoryWelcome';

import Workspace from './components/Workspace';
import EmployeeTable from './components/EmployeeTable';
import Modal from './components/Modal';
import Filters, { defaultFilters, salaryParameters } from './components/Filters';
import EmployeeForm from './components/EmployeeForm';
import EmployeeDetails from './components/EmployeeDetails';
import TransferForm from './components/TransferForm';

export default function App() {
  const [user, setUser] = useState(null);
  const [starting, setStarting] = useState(true);
  const [departments, setDepartments] = useState([]);
  const [roles, setRoles] = useState([]);
  const [section, setSection] = useState('directory');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [filters, setFilters] = useState(defaultFilters);
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState({ key: 'employeeCode', direction: 'asc' });
  const [result, setResult] = useState({ items: [], totalCount: 0, totalPages: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [dialog, setDialog] = useState(null);
  const [working, setWorking] = useState(false);
  const close = useCallback(() => setDialog(null), []);
  const clearSession = useCallback(() => {
    setUser(null);
    setDialog(null);
    setResult({ items: [], totalCount: 0, totalPages: 0 });
    setSearch('');
    setFilters(defaultFilters);
    setPage(1);
  }, []);
  useEffect(() => {
    setSessionHandler(clearSession);
    api('/auth/me')
      .then(setUser)
      .catch(() => {})
      .finally(() => setStarting(false));
  }, [clearSession]);
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(1);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);
  useEffect(() => {
    if (user)
      Promise.all([api('/departments'), api('/roles')])
        .then(([d, r]) => {
          setDepartments(d);
          setRoles(r);
        })
        .catch((e) => setError(e.message));
  }, [user]);
  useEffect(() => {
    if (!user) return;
    const controller = new AbortController();
    setLoading(true);
    setError('');
    const { salary, minSalary, maxSalary, ...regular } = filters;
    const params = {
      ...regular,
      ...salaryParameters(filters),
      search: debouncedSearch,
      pageNumber: page,
      pageSize: size,
      sortBy: sort.key,
      sortDirection: sort.direction,
    };
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== '' && v !== undefined),
    );
    api(`/employees?${query}`, { signal: controller.signal })
      .then((data) => {
        if (data.totalPages > 0 && page > data.totalPages) setPage(data.totalPages);
        else setResult(data);
      })
      .catch((e) => {
        if (e.name !== 'AbortError') setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [user, filters, debouncedSearch, page, size, sort, refresh]);
  useEffect(() => {
    if (notice) {
      const timer = setTimeout(() => setNotice(''), 6500);
      return () => clearTimeout(timer);
    }
  }, [notice]);

  function saved(message) {
    close();
    setNotice(message);
    setRefresh((v) => v + 1);
  }
  function changeFilters(value) {
    setFilters(value);
    setPage(1);
  }
  async function openEmployee(employee, mode = 'detail') {
    setWorking(true);
    setError('');
    try {
      const data = await api(
        `/employees/${employee.id}${mode === 'edit' && user.role === 'Admin' ? '?reveal=true' : ''}`,
      );
      setDialog({ mode, employee: data });
    } catch (e) {
      setError(e.message);
    } finally {
      setWorking(false);
    }
  }
  function toggleSort(key) {
    setSort({ key, direction: sort.key === key && sort.direction === 'asc' ? 'desc' : 'asc' });
    setPage(1);
  }
  async function logout() {
    try {
      await api('/auth/logout', { method: 'POST' });
      clearSession();
    } catch (e) {
      setError(e.message);
    }
  }
  async function changeStatus() {
    setWorking(true);
    try {
      await api(`/employees/${dialog.employee.id}/status`, {
        method: 'PATCH',
        body: {
          status: dialog.employee.status === 'Active' ? 'Inactive' : 'Active',
          version: dialog.employee.version,
        },
      });
      saved('Employee status updated.');
    } catch (e) {
      close();
      setError(e.message);
    } finally {
      setWorking(false);
    }
  }
  const filterCount = Object.entries(filters).filter(
    ([k, v]) => v && !['minSalary', 'maxSalary'].includes(k),
  ).length;
  const transferMode = section === 'transfers';

  if (starting)
    return (
      <div className="startup">
        <Shield size={36} />
        <p>Opening Personnel Office…</p>
      </div>
    );
  if (!user) return <Login onLogin={setUser} />;
  return (
    <>
      <Workspace
        user={user}
        section={section}
        setSection={setSection}
        onGuide={() => setDialog({ mode: 'guide' })}
        logout={logout}
      >
        <main>
          {!transferMode ? (
            <>
              <DirectoryWelcome
                user={user}
                count={result.totalCount}
                departments={departments.length}
                loading={loading}
                filtered={Boolean(search || filterCount)}
              />
              <div className="directory-section-heading">
                <div>
                  <h2>Personnel register</h2>
                  <p>Employee information and current assignments.</p>
                </div>
                {user.role === 'Admin' && (
                  <button className="button primary" onClick={() => setDialog({ mode: 'create' })}>
                    <span aria-hidden="true">+</span> Add employee
                  </button>
                )}
              </div>
            </>
          ) : (
            <>
              <DirectoryWelcome
                user={user}
                count={result.totalCount}
                departments={departments.length}
                loading={loading}
                filtered={Boolean(search || filterCount)}
                transfer
              />
              <div className="directory-section-heading">
                <div>
                  <h2>Select an employee</h2>
                  <p>Current assignments and department transfer history.</p>
                </div>
              </div>
            </>
          )}
          {notice && (
            <div className="alert success" role="status">
              <Check size={17} />
              {notice}
              <button onClick={() => setNotice('')} aria-label="Dismiss notification">
                <X size={16} />
              </button>
            </div>
          )}
          {error && (
            <div className="alert error" role="alert">
              {error}
              <button className="text-button" onClick={() => setRefresh((v) => v + 1)}>
                Retry
              </button>
            </div>
          )}
          {transferMode && (
            <div className="transfer-hint">
              <ArrowRightLeft size={19} />
              <p>
                {user.role === 'Admin'
                  ? 'Choose an active employee to begin a department transfer. Open any profile to review its transfer history.'
                  : 'Open an employee profile to review department assignments and transfer history. Transfers are recorded by an Admin.'}
              </p>
            </div>
          )}
          <section className="directory-card" aria-label="Employee records">
            <div className="card-heading">
              <div>
                <h2>
                  {transferMode ? 'Select an employee' : 'Personnel register'}{' '}
                  <span className="count-pill">{result.totalCount}</span>
                </h2>
                <p>
                  {transferMode
                    ? 'Current assignments and employee codes'
                    : 'Personal records, employment details, and current assignments'}
                </p>
              </div>
              <span className="record-label">EMPLOYEE RECORDS</span>
            </div>
            <div className="table-toolbar">
              <div className="search-input">
                <Search size={18} />
                <input
                  aria-label="Search employee records"
                  value={search}
                  placeholder="Search by name, code, email, department…"
                  onChange={(e) => setSearch(e.target.value)}
                />
                {search && (
                  <button aria-label="Clear search" onClick={() => setSearch('')}>
                    <X size={15} />
                  </button>
                )}
                <kbd>SEARCH</kbd>
              </div>
              <button
                className={`button secondary filter-button ${showFilters ? 'is-active' : ''}`}
                aria-expanded={showFilters}
                aria-controls="advanced-filters"
                onClick={() => setShowFilters(!showFilters)}
              >
                <SlidersHorizontal size={16} />
                Filters{filterCount > 0 && <span className="filter-count">{filterCount}</span>}
                <ChevronDown size={14} />
              </button>
            </div>
            {showFilters && (
              <Filters
                values={filters}
                departments={departments}
                roles={roles}
                onChange={changeFilters}
              />
            )}
            <EmployeeTable
              result={result}
              loading={loading}
              sort={sort}
              toggleSort={toggleSort}
              working={working}
              openEmployee={openEmployee}
              user={user}
              transferMode={transferMode}
              onStatus={(employee) => setDialog({ mode: 'status', employee })}
              page={page}
              size={size}
              setPage={setPage}
              setSize={setSize}
              onReset={() => {
                setSearch('');
                changeFilters(defaultFilters);
              }}
            />
          </section>
          <div className="page-footer">
            <span>
              <Shield size={13} />
              Access governed by your assigned role
            </span>
            <button className="text-button" onClick={() => setDialog({ mode: 'guide' })}>
              <CircleHelp size={14} />
              Workspace guide
            </button>
          </div>
        </main>
      </Workspace>
      {dialog?.mode === 'create' || dialog?.mode === 'edit' ? (
        <EmployeeForm
          employee={dialog.employee}
          user={user}
          departments={departments}
          roles={roles}
          onClose={close}
          onSaved={saved}
        />
      ) : null}
      {dialog?.mode === 'detail' && (
        <EmployeeDetails
          employee={dialog.employee}
          user={user}
          onClose={close}
          onEdit={(e) => openEmployee(e, 'edit')}
          onTransfer={(e) => openEmployee(e, 'transfer')}
        />
      )}
      {dialog?.mode === 'transfer' && (
        <TransferForm
          employee={dialog.employee}
          departments={departments}
          onClose={close}
          onSaved={saved}
        />
      )}
      {dialog?.mode === 'status' && (
        <Modal
          title={`${dialog.employee.status === 'Active' ? 'Deactivate' : 'Activate'} employee?`}
          subtitle={dialog.employee.fullName}
          onClose={close}
        >
          <div className="modal-body">
            <p>
              {dialog.employee.status === 'Active'
                ? 'The employee record and transfer history will be retained. Any linked login will be blocked while this employee is inactive.'
                : 'This restores active status. An otherwise enabled linked account can sign in again.'}
            </p>
          </div>
          <footer className="modal-footer">
            <span>{dialog.employee.employeeCode}</span>
            <div className="button-row">
              <button className="button secondary" onClick={close}>
                Cancel
              </button>
              <button className="button primary" disabled={working} onClick={changeStatus}>
                {working ? 'Updating…' : 'Confirm'}
              </button>
            </div>
          </footer>
        </Modal>
      )}
      {dialog?.mode === 'guide' && (
        <Modal
          title="Workspace guide"
          subtitle="A focused employee administration workflow"
          onClose={close}
        >
          <div className="modal-body guide">
            <h3>Find a record</h3>
            <p>
              Search names, codes, email, mobile, department, designation, address, or status.
              Combine filters to narrow results. Click column headings to sort.
            </p>
            <h3>Manage employees</h3>
            <p>
              Admin can register, edit, activate, and deactivate records. Manager can update
              designation, joining date, and annual salary. Employee accounts can view their own
              record.
            </p>
            <h3>Record a transfer</h3>
            <p>
              Admin selects an active employee and destination department. A new code is assigned
              while the internal employee ID and transfer history are preserved.
            </p>
            <h3>Protect personal details</h3>
            <p>
              Aadhaar and PAN are masked by default. Only Admin can reveal them. They are excluded
              from global search.
            </p>
            <div className="info-box">
              IAIP 2026 assessment prototype. Synthetic data only; this is not an official Army
              portal.
            </div>
          </div>
          <footer className="modal-footer">
            <span>Personnel Office</span>
            <button className="button primary" onClick={close}>
              Understood
            </button>
          </footer>
        </Modal>
      )}
    </>
  );
}
