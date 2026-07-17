interface Props {
  username: string | null;
  onLogout: () => void;
}

// A persistent, full-width bar shown on every page once logged in, so
// logging out is never more than one obvious click away - no more hunting
// for a small text link tucked in a corner.
export default function TopBar({ username, onLogout }: Props) {
  if (!username) return null;

  return (
    <div className="topbar">
      <div className="topbar-inner">
        <span className="topbar-brand">
          <span className="topbar-dot" aria-hidden="true" />
          Technical Team &middot; Club Quiz
        </span>
        <div className="topbar-right">
          <span className="topbar-username">{username}</span>
          <button
            type="button"
            className="btn-logout"
            onClick={onLogout}
            aria-label={`Log out of ${username}'s account`}
          >
            Log Out
          </button>
        </div>
      </div>
    </div>
  );
}
