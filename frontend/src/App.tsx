import { useState } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";
import Login from "./components/Login";
import Signup from "./components/Signup";
import ForgotPassword from "./components/ForgotPassword";
import ResetPassword from "./components/ResetPassword";
import Quiz from "./components/Quiz";
import ScoreResult from "./components/ScoreResult";
import Leaderboard from "./components/Leaderboard";
import NotFound from "./components/NotFound";
import TopBar from "./components/TopBar";
import { clearSession, getStoredUsername, getToken, ScoreResultData } from "./api/client";

type Stage = "quiz" | "result";

function QuizFlow() {
  const [stage, setStage] = useState<Stage>("quiz");
  const [result, setResult] = useState<ScoreResultData | null>(null);
  const navigate = useNavigate();

  if (stage === "quiz") {
    return (
      <Quiz
        onFinished={(res) => {
          setResult(res);
          setStage("result");
        }}
      />
    );
  }

  if (!result) {
    return null;
  }

  return (
    <ScoreResult
      result={result}
      onViewLeaderboard={() => navigate("/leaderboard")}
      onRetake={
        result.attemptsRemaining > 0
          ? () => {
              setResult(null);
              setStage("quiz");
            }
          : undefined
      }
    />
  );
}

export default function App() {
  // Real accounts: sign up / log in, then take the quiz. The JWT is stored
  // in localStorage so a refresh mid-session doesn't log the user out.
  const [username, setUsername] = useState<string | null>(() =>
    getToken() ? getStoredUsername() : null
  );

  function handleLogout() {
    clearSession();
    setUsername(null);
  }

  return (
    <BrowserRouter>
      <TopBar username={username} onLogout={handleLogout} />
      <Routes>
        <Route
          path="/login"
          element={username ? <Navigate to="/" replace /> : <Login onLoggedIn={setUsername} />}
        />
        <Route
          path="/signup"
          element={username ? <Navigate to="/" replace /> : <Signup onSignedUp={setUsername} />}
        />
        <Route
          path="/"
          element={username ? <QuizFlow /> : <Navigate to="/login" replace />}
        />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/leaderboard" element={<Leaderboard />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
