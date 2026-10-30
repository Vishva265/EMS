import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ArrowRightLeft,
  ChevronLeft,
  ChevronRight,
  Eye,
  Pencil,
  UsersRound,
} from 'lucide-react';
import { money } from '../api';

export default function EmployeeTable({
  result,
  loading,
  sort,
  toggleSort,
  working,
  openEmployee,
  user,
  transferMode,
  onStatus,
  page,
  size,
  setPage,
  setSize,
  onReset,
}) {
  const headings = [
    ['employeeCode', 'Employee code'],
    ['fullName', 'Employee'],
    ['department', 'Department'],
    ['role', 'Designation'],
    ['email', 'Email'],
    ['mobile', 'Mobile'],
    ['annualSalary', 'Annual salary'],
    ['status', 'Status'],
  ];

  return (
    <>
      <div className="table-scroll">
        <table aria-busy={loading}>
          <thead>
            <tr>
              {headings.map(([key, label]) => (
                <th
                  key={key}
                  aria-sort={
                    sort.key === key
                      ? sort.direction === 'asc'
                        ? 'ascending'
                        : 'descending'
                      : 'none'
                  }
                >
                  <button onClick={() => toggleSort(key)}>
                    {label}
                    {sort.key === key ? (
                      sort.direction === 'asc' ? (
                        <ArrowUp size={12} />
                      ) : (
                        <ArrowDown size={12} />
                      )
                    ) : (
                      <ArrowUpDown size={12} />
                    )}
                  </button>
                </th>
              ))}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={9}>
                  <div className="table-state">
                    <span className="spinner" />
                    Loading employee records…
                  </div>
                </td>
              </tr>
            ) : result.items.length === 0 ? (
              <tr>
                <td colSpan={9}>
                  <div className="table-state">
                    <UsersRound size={28} />
                    <strong>No employee records found</strong>
                    <p>Try another search or adjust your filters.</p>
                    <button
                      className="button secondary"
                      onClick={() => {
                        onReset();
                      }}
                    >
                      Clear search & filters
                    </button>
                  </div>
                </td>
              </tr>
            ) : (
              result.items.map((e) => (
                <tr key={e.id}>
                  <td>
                    <button
                      className="code-link"
                      onClick={() => openEmployee(e)}
                      disabled={working}
                    >
                      {e.employeeCode}
                    </button>
                  </td>
                  <td>
                    <div className="employee-cell">
                      <span className={`avatar tone-${e.departmentId % 5}`}>
                        {e.fullName
                          .split(' ')
                          .map((n) => n[0])
                          .slice(0, 2)
                          .join('')}
                      </span>
                      <strong>{e.fullName}</strong>
                    </div>
                  </td>
                  <td>
                    <span className={`department-badge dept-${e.departmentId}`}>
                      {e.department}
                    </span>
                  </td>
                  <td className="designation-cell">{e.role}</td>
                  <td className="email-cell">{e.email}</td>
                  <td className="mono mobile-cell">{e.mobile}</td>
                  <td className="salary-cell">{money(e.annualSalary)}</td>
                  <td>
                    <span className={`status ${e.status.toLowerCase()}`}>{e.status}</span>
                  </td>
                  <td>
                    <div className="row-actions">
                      <button
                        aria-label={`View ${e.fullName}`}
                        title="View profile"
                        disabled={working}
                        onClick={() => openEmployee(e)}
                      >
                        <Eye size={16} />
                        <span className="directory-action-label">View</span>
                      </button>
                      {!transferMode && user.role !== 'Employee' && (
                        <button
                          aria-label={`Edit ${e.fullName}`}
                          title="Edit employee"
                          disabled={working}
                          onClick={() => openEmployee(e, 'edit')}
                        >
                          <Pencil size={15} />
                          <span className="directory-action-label">Edit</span>
                        </button>
                      )}
                      {user.role === 'Admin' &&
                        (transferMode ? (
                          e.status === 'Active' && (
                            <button
                              aria-label={`Transfer ${e.fullName}`}
                              title="Transfer department"
                              disabled={working}
                              onClick={() => openEmployee(e, 'transfer')}
                            >
                              <ArrowRightLeft size={16} />
                            </button>
                          )
                        ) : (
                          <button
                            className="status-action"
                            title={e.status === 'Active' ? 'Deactivate' : 'Activate'}
                            onClick={() => onStatus(e)}
                          >
                            {e.status === 'Active' ? 'Deactivate' : 'Activate'}
                          </button>
                        ))}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <footer className="pagination">
        <p>
          {result.totalCount ? (
            <>
              Showing{' '}
              <strong>
                {(page - 1) * size + 1}–{Math.min(page * size, result.totalCount)}
              </strong>{' '}
              of <strong>{result.totalCount}</strong> records
            </>
          ) : (
            '0 matching records'
          )}
        </p>
        <div className="pagination-controls">
          <label>
            Rows per page{' '}
            <select
              aria-label="Rows per page"
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(1);
              }}
            >
              {[5, 10, 20].map((n) => (
                <option key={n}>{n}</option>
              ))}
            </select>
          </label>
          <span className="pagination-divider" />
          <button
            className="page-arrow"
            aria-label="Previous page"
            disabled={page <= 1 || loading}
            onClick={() => setPage(page - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          <span className="page-current">{page}</span>
          <span className="page-total">of {Math.max(result.totalPages, 1)}</span>
          <button
            className="page-arrow"
            aria-label="Next page"
            disabled={page >= result.totalPages || loading}
            onClick={() => setPage(page + 1)}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </footer>
    </>
  );
}
