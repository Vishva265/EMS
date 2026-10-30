import { RotateCcw } from 'lucide-react';
import { Field } from './Modal';

export const defaultFilters = {
  departmentId: '',
  roleId: '',
  status: '',
  gender: '',
  joiningDateFrom: '',
  joiningDateTo: '',
  salary: '',
  minSalary: '',
  maxSalary: '',
};
export function salaryParameters(filters) {
  const bands = {
    '1-3': [100000, 300000],
    '3-5': [300000, 500000],
    '5-10': [500000, 1000000],
    '10+': [1000000, ''],
  };
  if (bands[filters.salary]) {
    const [minSalary, maxSalary] = bands[filters.salary];
    return { minSalary, maxSalary, salaryMaxExclusive: maxSalary !== '' };
  }
  return filters.salary === 'custom'
    ? { minSalary: filters.minSalary, maxSalary: filters.maxSalary }
    : {};
}

export default function Filters({ values, departments, roles, onChange }) {
  const change = (key, value) => onChange({ ...values, [key]: value });
  return (
    <div className="advanced-filters" id="advanced-filters">
      <div className="filter-grid">
        <Field label="Department">
          <select
            value={values.departmentId}
            onChange={(e) => change('departmentId', e.target.value)}
          >
            <option value="">All departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Designation">
          <select value={values.roleId} onChange={(e) => change('roleId', e.target.value)}>
            <option value="">All designations</option>
            {roles.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Status">
          <select value={values.status} onChange={(e) => change('status', e.target.value)}>
            <option value="">All statuses</option>
            <option>Active</option>
            <option>Inactive</option>
          </select>
        </Field>
        <Field label="Gender">
          <select value={values.gender} onChange={(e) => change('gender', e.target.value)}>
            <option value="">All genders</option>
            {['Male', 'Female', 'Other'].map((g) => (
              <option key={g}>{g}</option>
            ))}
          </select>
        </Field>
        <Field label="Joined from">
          <input
            type="date"
            value={values.joiningDateFrom}
            onChange={(e) => change('joiningDateFrom', e.target.value)}
          />
        </Field>
        <Field label="Joined until">
          <input
            type="date"
            min={values.joiningDateFrom}
            value={values.joiningDateTo}
            onChange={(e) => change('joiningDateTo', e.target.value)}
          />
        </Field>
        <Field label="Annual salary">
          <select value={values.salary} onChange={(e) => change('salary', e.target.value)}>
            <option value="">All salary ranges</option>
            <option value="1-3">₹1–3 LPA</option>
            <option value="3-5">₹3–5 LPA</option>
            <option value="5-10">₹5–10 LPA</option>
            <option value="10+">₹10+ LPA</option>
            <option value="custom">Custom range</option>
          </select>
        </Field>
        <div className="filter-reset">
          <button className="text-button" onClick={() => onChange(defaultFilters)}>
            <RotateCcw size={14} />
            Reset filters
          </button>
        </div>
        {values.salary === 'custom' && (
          <>
            <Field label="Minimum annual salary (₹)">
              <input
                type="number"
                min="0"
                step="0.01"
                placeholder="No minimum"
                value={values.minSalary}
                onChange={(e) => change('minSalary', e.target.value)}
              />
            </Field>
            <Field label="Maximum annual salary (₹)">
              <input
                type="number"
                min={values.minSalary || 0}
                step="0.01"
                placeholder="No maximum"
                value={values.maxSalary}
                onChange={(e) => change('maxSalary', e.target.value)}
              />
            </Field>
          </>
        )}
      </div>
      <p className="filter-footnote">
        Filters work together. Salary is annual INR; preset upper limits are exclusive.
      </p>
    </div>
  );
}
