import { useState } from "react";
import { ScoreResultData } from "../api/client";
import Leaderboard from "./Leaderboard";

interface Props {
  result: ScoreResultData;
  onViewLeaderboard: () => void;
  onRetake?: () => void;
}

type Tab = "review" | "history";

export default function ScoreResult({ result, onViewLeaderboard, onRetake }: Props) {
  const { name, score, totalQuestions, review, attemptsUsed, attemptsRemaining } = result;
  const [tab, setTab] = useState<Tab>("review");

  return (
    <div className="page-wide">
      <div className="card" style={{ textAlign: "center", width: "100%", maxWidth: "none" }}>
        <p className="eyebrow">Nice work, {name}</p>
        <p className="score-headline">
          {score}/{totalQuestions}
        </p>
        <p className="score-sub">Your best score has been saved to the leaderboard.</p>
        <p className="attempt-badge" style={{ marginBottom: 18 }}>
          Attempt {attemptsUsed} of {attemptsUsed + attemptsRemaining} used
          {attemptsRemaining > 0 ? ` — ${attemptsRemaining} left` : " — none left"}
        </p>
        <div style={{ display: "flex", gap: 10, justifyContent: "center", flexWrap: "wrap" }}>
          {onRetake && (
            <button className="btn-secondary" onClick={onRetake}>
              Retake Quiz ({attemptsRemaining} left)
            </button>
          )}
          <button className="btn-primary" onClick={onViewLeaderboard}>
            View Leaderboard
          </button>
        </div>
      </div>

      <div style={{ width: "100%", marginTop: 32 }}>
        <div className="tab-row" role="tablist" aria-label="Result sections">
          <button
            type="button"
            role="tab"
            aria-selected={tab === "review"}
            className={`tab-btn${tab === "review" ? " active" : ""}`}
            onClick={() => setTab("review")}
          >
            Answer Review
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === "history"}
            className={`tab-btn${tab === "history" ? " active" : ""}`}
            onClick={() => setTab("history")}
          >
            History Lesson
          </button>
        </div>

        {tab === "review" && (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
              gap: 16,
              width: "100%",
            }}
          >
            {review.map((r, idx) => (
              <div
                key={r.questionId}
                className="card"
                style={{
                  textAlign: "left",
                  width: "100%",
                  maxWidth: "none",
                  margin: 0,
                  borderLeft: `4px solid ${r.correct ? "var(--success)" : "var(--error)"}`,
                }}
              >
                <p className="question-number">
                  Question {idx + 1} of {review.length}
                </p>
                <p className="question-text" style={{ marginBottom: 14, fontSize: 17 }}>
                  {r.questionText}
                </p>

                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    flexWrap: "wrap",
                    padding: "10px 14px",
                    borderRadius: 10,
                    marginBottom: r.correct ? 0 : 8,
                    background: r.correct ? "var(--success-bg)" : "var(--error-bg)",
                    color: r.correct ? "var(--success)" : "var(--error)",
                    fontSize: 14.5,
                  }}
                >
                  <strong style={{ minWidth: 90 }}>Your answer:</strong>
                  <span>{r.yourAnswer ?? "No answer given"}</span>
                  <span style={{ marginLeft: "auto", fontWeight: 700 }}>
                    {r.correct ? "✓ Correct" : "✗ Incorrect"}
                  </span>
                </div>

                {!r.correct && (
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 8,
                      padding: "10px 14px",
                      borderRadius: 10,
                      marginBottom: 8,
                      background: "var(--success-bg)",
                      color: "var(--success)",
                      fontSize: 14.5,
                    }}
                  >
                    <strong style={{ minWidth: 90 }}>Correct answer:</strong>
                    <span>{r.correctAnswer}</span>
                  </div>
                )}

                <p
                  style={{
                    fontSize: 13.5,
                    color: "var(--text-dim)",
                    marginTop: 10,
                    lineHeight: 1.5,
                  }}
                >
                  <strong style={{ color: "var(--text)" }}>Why: </strong>
                  {r.explanation}
                </p>
              </div>
            ))}
          </div>
        )}

        {tab === "history" && (
          <>
            <div style={{ display: "flex", flexDirection: "column", gap: 16, width: "100%" }}>
              {review.map((r, idx) => (
                <div
                  key={r.questionId}
                  className="card"
                  style={{
                    textAlign: "left",
                    width: "100%",
                    maxWidth: "none",
                    margin: 0,
                    borderLeft: "4px solid var(--amber)",
                  }}
                >
                  <p className="question-number">
                    Question {idx + 1} &middot; History Lesson
                  </p>
                  <p className="question-text" style={{ marginBottom: 10, fontSize: 16 }}>
                    {r.questionText}
                  </p>
                  <p style={{ fontSize: 14.5, color: "var(--text)", lineHeight: 1.65 }}>
                    {r.history}
                  </p>
                </div>
              ))}
            </div>

            <div style={{ marginTop: 32 }}>
              <h2
                style={{
                  fontFamily: "var(--display)",
                  color: "var(--amber-bright)",
                  fontSize: 22,
                  letterSpacing: "0.03em",
                  marginBottom: 16,
                  textTransform: "uppercase",
                }}
              >
                Leaderboard
              </h2>
              <Leaderboard embedded />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
