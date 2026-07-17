import { useEffect, useState } from "react";
import { fetchQuestions, submitScore, fetchAttempts, Question, ScoreResultData, AttemptsStatus } from "../api/client";

interface Props {
  onFinished: (result: ScoreResultData) => void;
}

export default function Quiz({ onFinished }: Props) {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [attempts, setAttempts] = useState<AttemptsStatus | null>(null);
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchQuestions(), fetchAttempts()])
      .then(([q, a]) => {
        setQuestions(q);
        setAttempts(a);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load questions."))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="page">
        <div className="card empty-state" role="status" aria-live="polite">
          <span className="spinner" />
          Loading questions…
        </div>
      </div>
    );
  }

  if (error || questions.length === 0) {
    return (
      <div className="page">
        <div className="card">
          <div className="banner-error" role="alert">{error || "No questions available right now."}</div>
        </div>
      </div>
    );
  }

  if (attempts && attempts.attemptsRemaining <= 0) {
    return (
      <div className="page">
        <div className="card" style={{ textAlign: "center" }}>
          <p className="eyebrow">No attempts left</p>
          <p className="score-sub">
            You've used all {attempts.maxAttempts} attempts for this quiz. Your best score is already saved
            to the leaderboard.
          </p>
        </div>
      </div>
    );
  }

  const question = questions[current];
  const isLast = current === questions.length - 1;
  const selected = answers[question.id];
  const selectedPosition = question.options.findIndex((o) => o.id === selected);

  function selectOption(optionId: number) {
    setAnswers((prev) => ({ ...prev, [question.id]: optionId }));
  }

  async function handleNext() {
    if (isLast) {
      setSubmitting(true);
      setError(null);
      try {
        const result = await submitScore(answers);
        onFinished(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Could not submit your score.");
      } finally {
        setSubmitting(false);
      }
    } else {
      setCurrent((c) => c + 1);
    }
  }

  const progressPct = ((current + 1) / questions.length) * 100;
  const attemptLabel = attempts
    ? `Attempt ${attempts.attemptsUsed + 1} of ${attempts.maxAttempts}`
    : null;

  return (
    <div className="page">
      {attemptLabel && (
        <p
          style={{
            fontFamily: "var(--mono)",
            fontSize: 12.5,
            color: "var(--amber)",
            letterSpacing: "0.06em",
            textTransform: "uppercase",
            marginBottom: 10,
          }}
        >
          {attemptLabel}
        </p>
      )}
      <div
        className="progress-track"
        role="progressbar"
        aria-valuenow={current + 1}
        aria-valuemin={1}
        aria-valuemax={questions.length}
        aria-label={`Question ${current + 1} of ${questions.length}`}
      >
        <div className="progress-fill" style={{ width: `${progressPct}%` }} />
      </div>

      <div className="card" style={{ maxWidth: 560 }}>
        {error && <div className="banner-error" role="alert">{error}</div>}
        <p className="question-number">
          Question {current + 1} of {questions.length}
        </p>
        <p className="question-text" id={`question-${question.id}-text`}>
          {question.text}
        </p>

        <div
          className="option-list"
          role="radiogroup"
          aria-labelledby={`question-${question.id}-text`}
          onKeyDown={(e) => {
            if (!["ArrowDown", "ArrowRight", "ArrowUp", "ArrowLeft"].includes(e.key)) return;
            e.preventDefault();
            const dir = e.key === "ArrowDown" || e.key === "ArrowRight" ? 1 : -1;
            const from = selectedPosition >= 0 ? selectedPosition : 0;
            const next = (from + dir + question.options.length) % question.options.length;
            selectOption(question.options[next].id);
            (e.currentTarget.children[next] as HTMLElement)?.focus();
          }}
        >
          {question.options.map((opt, idx) => (
            <button
              key={opt.id}
              type="button"
              role="radio"
              tabIndex={selected === opt.id || (selected === undefined && idx === 0) ? 0 : -1}
              className={`option-btn${selected === opt.id ? " selected" : ""}`}
              onClick={() => selectOption(opt.id)}
              aria-checked={selected === opt.id}
            >
              {opt.text}
            </button>
          ))}
        </div>

        <div className="nav-row">
          <button
            className="btn-secondary"
            type="button"
            disabled={current === 0}
            onClick={() => setCurrent((c) => c - 1)}
          >
            Back
          </button>
          <button
            className="btn-primary"
            type="button"
            disabled={selected === undefined || submitting}
            onClick={handleNext}
          >
            {submitting ? "Submitting…" : isLast ? "Finish Quiz" : "Next"}
          </button>
        </div>
      </div>
    </div>
  );
}
