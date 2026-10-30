import PeopleIllustration from './PeopleIllustration';
// Small original illustrations. Animation is CSS-only and respects reduced motion.
export default function Doodle({ variant = 'notes' }) {
  if (['login', 'laptop', 'laptop-alt'].includes(variant))
    return <PeopleIllustration variant={variant} />;
  if (variant === 'notes')
    return (
      <span className="doodle doodle-notes" aria-hidden="true">
        <img src="/illustrations/personnel-notes.svg" alt="" />
      </span>
    );
  return (
    <span className={`doodle doodle-${variant}`} aria-hidden="true">
      <svg
        viewBox="0 0 360 220"
        fill="none"
        stroke="#65765c"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M39 196c90-4 186 4 281-1" stroke="#c0c9b4" />
        {variant === 'transfer' ? (
          <>
            <g className="doodle-drift">
              <path d="M43 100V77h53l13 14h53v94H43z" fill="#f8f5e9" />
              <path d="M43 104h119l-12 81H32z" fill="#c8d3bb" />
              <path d="M65 125h53m-53 13h31" />
            </g>
            <path d="M209 82V59h47l15 14h45v112H209z" fill="#f8f5e9" />
            <path d="M209 95h116l-10 90H198z" fill="#dfc7a2" />
            <path d="M232 120h49m-49 12h30" />
            <g className="doodle-sway">
              <path d="M131 55c30-34 65-37 99-15m-14-14 15 14-20 5" />
              <path d="M222 166c-20 28-49 29-72 7m3 15-5-16 17 2" stroke="#9c9f7b" />
            </g>
          </>
        ) : variant === 'login' ? (
          <>
            <path d="M114 190V42h124v148" fill="#f9f6eb" />
            <path d="M128 190V54l84 12v124" fill="#cbd6bf" />
            <path d="M129 54l83 12m-3 54h-13" />
            <circle cx="190" cy="120" r="3" />
            <g className="doodle-drift">
              <circle cx="272" cy="124" r="19" fill="#dfc7a2" />
              <path d="M258 138l-29 31 7 7 10-11 7 5 9-10-6-5 13-14" fill="#dfc7a2" />
            </g>
            <g className="doodle-sway">
              <path
                d="M74 190c-3-37-5-59 8-87m-8 49c-20-2-31-14-30-29 20 2 29 15 30 29zm4-18c23-1 35-12 36-27-20 0-32 11-36 27z"
                fill="#b3c3a4"
              />
            </g>
            <path d="M61 171h31l-4 24H65z" fill="#ede3d0" />
          </>
        ) : variant === 'profile' ? (
          <>
            <path d="M87 56l178-8 6 137-180 8z" fill="#faf7ed" />
            <path d="M99 67l51-2 4 75-51 2z" fill="#d5ddc9" />
            <circle cx="126" cy="88" r="12" fill="#e5cfb1" />
            <path d="M110 125c0-26 34-26 34 0" fill="#aebfa0" />
            <path d="M173 79l62-3m-61 19 46-2m-45 21 62-3m-61 21 32-1m-97 30 119-5" />
            <g className="doodle-sway">
              <path
                d="M262 195c3-30 13-58 32-75m-22 50c-18-5-26-15-24-29 19 5 24 15 24 29zm9-22c19 0 29-9 32-22-19-1-29 10-32 22z"
                fill="#b9c9aa"
              />
            </g>
          </>
        ) : (
          <>
            <path
              d="M180 66c-44-25-86-23-123-8v119c40-17 80-15 123 9 43-24 83-26 123-9V58c-37-15-79-17-123 8z"
              fill="#faf7ed"
            />
            <path d="M180 66v120M76 78c29-9 57-5 84 8m-84 11c29-9 57-5 84 8m-84 11c29-9 57-5 84 8m40-38c27-13 55-17 84-8m-84 27c27-13 55-17 84-8m-84 27c27-13 55-17 84-8" />
            <g className="doodle-drift">
              <path d="M246 44l15-15m-1 29 19-1m-45-23 1-18" stroke="#b79d73" />
            </g>
          </>
        )}
      </svg>
    </span>
  );
}
