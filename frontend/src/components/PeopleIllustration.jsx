// Editable SVG characters: separate arm/door groups allow gentle, local animation.
export default function PeopleIllustration({ variant }) {
  const doorway = variant === 'login';
  const alternate = variant === 'laptop-alt';
  return (
    <span
      className={`doodle people-illustration ${doorway ? 'person-arriving' : 'person-working'}`}
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 420 270"
        fill="none"
        stroke="#485d4d"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <ellipse cx="214" cy="248" rx="155" ry="7" fill="#dce3d1" stroke="none" />
        {doorway ? (
          <>
            <path d="M211 241V26h118v215" fill="#faf8ef" />
            <path d="M224 239V39h92v200" fill="#d8e1cc" stroke="none" />
            <g className="opening-door">
              <path d="M225 39l66 23v179l-66-2z" fill="#a8bca0" />
              <path d="M235 58l44 15v72l-44-7zm0 95 44 6v62l-44-2z" stroke="#8da386" />
              <path d="M276 153l-13-2" strokeWidth="3" />
            </g>
            <path d="M128 170l9 64 20 1 9-65" fill="#536958" />
            <path d="M162 171l21 54 19-7-18-60" fill="#6d8169" />
            <path
              d="M136 233l-2 11h30c3-5-7-9-9-11m29-10 4 14 28-10c0-7-12-9-16-8"
              fill="#384c40"
            />
            <path d="M126 111c8-15 19-20 33-18 19 0 28 14 34 34l-9 45-58 1-5-36z" fill="#d0b18c" />
            <path d="M143 91l-1 9c5 8 14 8 21-1l-2-15" fill="#e5c5a4" />
            <path d="M138 55c-8 6-8 27 2 34 7 6 22 4 26-8l3-24" fill="#ebceb0" />
            <path
              d="M134 69c-8-20 5-36 20-34 23-1 25 16 15 30l-7-17c-7 8-17 11-28 10z"
              fill="#3c5045"
            />
            <path d="M166 69h6l-3 7m-7 7-7 2" />
            <g className="welcoming-arm">
              <path d="M180 113l21 24 54 3-1 12-61-2c-8-1-22-18-26-25" fill="#d0b18c" />
              <path d="M252 140l15-1 8 5-4 8-18 0" fill="#ebceb0" />
            </g>
            <path d="M129 121l-8 37 13 23 9-5-9-22 7-28" fill="#ebceb0" />
            <path d="M104 182h35v46h-35z" fill="#9aab8d" />
            <path d="M114 182v-8h16v8" />
            <g className="doodle-sway">
              <path
                d="M348 240c-4-39-3-62 11-85m-12 49c-21-2-31-15-30-27 20 2 29 13 30 27zm3-19c19-1 32-13 33-27-21 0-30 12-33 27z"
                fill="#adbf9d"
              />
            </g>
            <path d="M333 220h32l-5 26h-22z" fill="#eee3ce" />
            <path d="M304 15l7-6m0 17 11-1" stroke="#bfa57e" />
          </>
        ) : (
          <>
            <path d="M125 158c-18-4-31 8-27 26l10 43h71l10-55" fill="#c9d4bf" />
            <path d="M116 226l-7 19m61-19 9 19" />
            <path d="M150 180l-3 54 17 1 15-49" fill="#687d66" />
            <path d="M174 181l19 54 17-6-9-55" fill="#516953" />
            <path
              d="M148 231l-4 14h31c0-7-10-12-13-12m31-3 4 14 29-7c-1-7-10-10-18-8"
              fill="#3b5143"
            />
            {alternate ? (
              <path d="M116 103c-8-36-1-68 26-69 27-1 39 22 27 66l-24 22z" fill="#3d5145" />
            ) : (
              <path d="M122 76c-14-28-2-46 20-46 23-1 34 20 20 40z" fill="#3d5145" />
            )}
            <path d="M126 59c-2 17 1 33 15 35 15 2 23-11 21-30-11-1-20-6-25-13" fill="#e9c9a8" />
            <path d="M135 89l-2 15 22 1-3-15" fill="#e4c09e" />
            <path
              d="M111 115c3-12 16-16 26-15l16 0c21 2 29 14 33 36l-5 46-68 1-9-39z"
              fill={alternate ? '#c5aa84' : '#a6b99b'}
            />
            <path d="M130 101l12 13 12-13m-12 13 1 20" stroke={alternate ? '#a88b66' : '#879f7d'} />
            <g className="typing-arm">
              <path d="M112 128l-2 34c7 8 41 12 57 10l3-10-41-10 2-20" fill="#e9c9a8" />
              <path d="M165 162l17-2 12 9-5 6-23-3" fill="#e9c9a8" />
            </g>
            <path d="M173 125l14 26 27 7-3 12-39-10-10-17" fill="#e9c9a8" />
            <path d="M75 184h263m-247 0-6 62m236-62 6 62" strokeWidth="3" />
            <path d="M201 177l-15-65h91l18 65z" fill="#f5f4e9" />
            <path d="M203 126h64l11 43h-64z" fill="#d7dfcb" stroke="none" />
            <path d="M190 179h115" strokeWidth="3" />
            <circle cx="242" cy="145" r="5" stroke="#9bad8f" />
            <path d="M309 162h17l-2 20h-13z" fill="#d5bb96" />
            <path d="M326 165c11-1 10 12-1 11m-10-22c-5-7 6-7 1-13" />
            <g className="doodle-sway">
              <path
                d="M346 182c-3-27-1-48 12-62m-12 34c-18-2-24-10-24-21 15 3 23 9 24 21zm3-14c18-2 25-11 26-22-17 3-24 10-26 22z"
                fill="#b3c4a2"
              />
            </g>
            <path d="M336 168h24l-4 16h-16z" fill="#eee4d1" />
            <path d="M210 53l11-7m-8 20 13-1" stroke="#c0a57c" />
            <path d="M279 53h45v37h-45z" stroke="#b6c2aa" />
            <path d="M286 65h29m-29 8h17" stroke="#b6c2aa" />
          </>
        )}
      </svg>
    </span>
  );
}
