import Doodle from './Doodle';
export default function DirectoryWelcome({
  user,
  count,
  departments,
  loading,
  filtered,
  transfer = false,
}) {
  return (
    <>
      <div className="directory-intro">
        <div>
          <span className="directory-caption">PERSONNEL OFFICE / RECORDS</span>
          <h1>
            {transfer
              ? 'Department transfers'
              : user.role === 'Employee'
                ? 'My employee record'
                : 'Employee directory'}
          </h1>
        </div>
        <span className="directory-date">
          {new Date().toLocaleDateString('en-IN', {
            day: 'numeric',
            month: 'long',
            year: 'numeric',
          })}
        </span>
      </div>
      <section className="directory-welcome">
        <div className="welcome-copy">
          <span className="welcome-tag">YOUR PERSONNEL WORKSPACE</span>
          <h2>
            {transfer ? 'A new department.' : 'Welcome back,'}{' '}
            <span>{transfer ? 'The same person.' : `${user.username}.`}</span>
          </h2>
          <p>
            {transfer
              ? 'Record each move without losing the history.'
              : 'Keep employee details up to date.'}
            <br />
            {transfer
              ? 'Employee identity stays linked through every change.'
              : 'Make the next department move with a clear record.'}
          </p>
          <span className="welcome-signoff">A place for people. A record of their progress.</span>
        </div>
        <Doodle variant={transfer ? 'laptop-alt' : 'laptop'} />
      </section>
      <div className="directory-summary">
        <div>
          <span className="summary-number">{loading ? '—' : count}</span>
          <span>
            {filtered ? 'Matching employees' : 'Employee records'}
            <small>
              {filtered
                ? 'With your current search and filters'
                : 'Personal and employment information'}
            </small>
          </span>
        </div>
        <div>
          <span className="summary-number">{departments}</span>
          <span>
            Departments<small>Across the organization</small>
          </span>
        </div>
        <div className="summary-note">
          <span className="note-number">01 —</span>
          <p>
            Find a person below.
            <br />
            Open their record to view the details.
          </p>
        </div>
      </div>
    </>
  );
}
