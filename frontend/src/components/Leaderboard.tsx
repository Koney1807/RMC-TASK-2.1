import { useEffect, useRef, useState } from "react";
import { fetchLeaderboard, ScoreEntry } from "../api/client";

const POLL_INTERVAL_MS = 5000;
const PAGE_SIZE = 50;

interface Props {
  // When true, renders just the table/controls (no page wrapper, title, or
  // "take the quiz" link) so it can be dropped inline into another page -
  // e.g. under the History Lesson tab on the results screen.
  embedded?: boolean;
}

export default function Leaderboard({ embedded = false }: Props) {
  const [entries, setEntries] = useState<ScoreEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [visibleSize, setVisibleSize] = useState(PAGE_SIZE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false);
  // Always fetch with the latest requested size, even from inside the
  // polling interval closure (which captures whatever visibleSize was at
  // mount time otherwise).
  const visibleSizeRef = useRef(visibleSize);
  visibleSizeRef.current = visibleSize;

  async function load() {
    if (inFlight.current) return; // previous poll still in flight - skip this tick
    inFlight.current = true;
    try {
      const result = await fetchLeaderboard(visibleSizeRef.current);
      setEntries(result.items);
      setTotal(result.total);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load the leaderboard.");
    } finally {
      setLoading(false);
      inFlight.current = false;
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visibleSize]);

  useEffect(() => {
    // Poll every few seconds so new submissions from other participants
    // appear without anyone needing to manually refresh the page - but
    // only while the tab is actually visible, to avoid burning battery
    // and requests on a backgrounded tab.
    let interval: ReturnType<typeof setInterval> | null = null;

    function startPolling() {
      if (interval) return;
      interval = setInterval(load, POLL_INTERVAL_MS);
    }
    function stopPolling() {
      if (interval) {
        clearInterval(interval);
        interval = null;
      }
    }
    function handleVisibilityChange() {
      if (document.hidden) {
        stopPolling();
      } else {
        load();
        startPolling();
      }
    }

    if (!document.hidden) startPolling();
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      stopPolling();
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, []);

  const body = (
    <>
      {!embedded && (
        <div className="brand-row">
          <span className="brand-mark" />
          <p className="eyebrow">Technical Team · Club Quiz</p>
        </div>
      )}
      {!embedded && <h1 style={{ marginBottom: 6 }}>Leaderboard</h1>}
      <p className="subtitle" style={{ textAlign: "left", marginBottom: 20 }}>
        <span className="live-dot" />
        Updates automatically every {POLL_INTERVAL_MS / 1000} seconds
        {total > 0 && ` · ${total} participant${total === 1 ? "" : "s"}`}
      </p>

      {error && <div className="banner-error" role="alert">{error}</div>}

      <div className="table-wrap">
        {loading ? (
          <div className="empty-state" role="status" aria-live="polite">
            <span className="spinner" />
            Loading leaderboard…
          </div>
        ) : entries.length === 0 ? (
          <div className="empty-state">No scores yet — be the first to take the quiz!</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Rank</th>
                <th>Name</th>
                <th>Score</th>
                <th>Submitted</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e, i) => (
                <tr key={e.id}>
                  <td>
                    <span className="rank-badge">#{i + 1}</span>
                  </td>
                  <td>{e.name}</td>
                  <td>
                    {e.score}/{e.totalQuestions}
                  </td>
                  <td>{new Date(e.submittedAt).toLocaleTimeString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {!loading && entries.length < total && (
        <div className="pagination">
          <button
            className="btn-secondary"
            type="button"
            onClick={() => setVisibleSize((s) => s + PAGE_SIZE)}
          >
            Show more ({total - entries.length} remaining)
          </button>
        </div>
      )}

      {!embedded && (
        <a className="footer-link" href="/">
          ← Take the quiz
        </a>
      )}
    </>
  );

  if (embedded) {
    return <div className="dashboard dashboard-embedded">{body}</div>;
  }

  return (
    <div className="page">
      <div className="dashboard" style={{ maxWidth: 1100 }}>
        {body}
      </div>
    </div>
  );
}
