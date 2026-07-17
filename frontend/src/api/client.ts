const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

export interface QuestionOption {
  id: number;
  text: string;
}

export interface Question {
  id: number;
  text: string;
  // Options arrive in a fresh shuffled order on every fetch; each option's
  // `id` is stable regardless of display order, so answers must be recorded
  // and submitted by option id, not by position in this array.
  options: QuestionOption[];
}

export interface ScoreEntry {
  id: string;
  name: string;
  score: number;
  totalQuestions: number;
  submittedAt: string;
}

export interface QuestionReview {
  questionId: number;
  questionText: string;
  yourAnswer: string | null;
  correctAnswer: string;
  correct: boolean;
  explanation: string;
  history: string;
}

export interface ScoreResultData {
  name: string;
  score: number;
  totalQuestions: number;
  attemptsUsed: number;
  attemptsRemaining: number;
  review: QuestionReview[];
}

export interface AttemptsStatus {
  attemptsUsed: number;
  attemptsRemaining: number;
  maxAttempts: number;
}

export async function fetchAttempts(): Promise<AttemptsStatus> {
  const res = await fetch(`${API_BASE}/api/attempts`, { headers: { ...authHeader() } });
  if (res.status === 401) {
    clearSession();
    throw new Error("Your session has expired. Please log in again.");
  }
  if (!res.ok) throw new Error("Could not check attempt status.");
  return res.json();
}

export async function fetchQuestions(): Promise<Question[]> {
  const res = await fetch(`${API_BASE}/api/questions`);
  if (!res.ok) throw new Error("Could not load quiz questions.");
  return res.json();
}

// ---- Real accounts: signup / login / JWT session ----
// Token lives in localStorage so a logged-in participant stays logged in
// across a refresh; it's a short-lived (12h) JWT, not a raw password.
const TOKEN_KEY = "auth_token";
const USERNAME_KEY = "auth_username";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUsername(): string | null {
  return localStorage.getItem(USERNAME_KEY);
}

function storeSession(token: string, username: string) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USERNAME_KEY, username);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
}

async function handleAuthResponse(res: Response): Promise<{ token: string; username: string }> {
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message || "Authentication failed.");
  }
  const data = await res.json();
  storeSession(data.token, data.username);
  return data;
}

export async function signup(username: string, email: string, password: string) {
  const res = await fetch(`${API_BASE}/api/auth/signup`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, email, password }),
  });
  return handleAuthResponse(res);
}

export async function login(username: string, password: string) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  return handleAuthResponse(res);
}

// ---- Forgot / reset password ----
// forgotPassword always resolves with the server's generic message (it
// never reveals whether the email matched an account); resetPassword throws
// if the token is invalid or expired.

export async function forgotPassword(email: string): Promise<string> {
  const res = await fetch(`${API_BASE}/api/auth/forgot-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(body?.message || "Could not process that request.");
  }
  return body?.message ?? "If an account with that email exists, we've sent a password reset link.";
}

export async function resetPassword(token: string, newPassword: string): Promise<string> {
  const res = await fetch(`${API_BASE}/api/auth/reset-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, newPassword }),
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(body?.message || "Could not reset your password.");
  }
  return body?.message ?? "Your password has been reset. You can now log in.";
}

function authHeader(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// The quiz no longer takes a free-text name - the submitter's identity comes
// from their JWT, so the backend attaches the name server-side.
export async function submitScore(answers: Record<number, number>): Promise<ScoreResultData> {
  const res = await fetch(`${API_BASE}/api/scores`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeader() },
    body: JSON.stringify({ answers }),
  });
  if (res.status === 401) {
    clearSession();
    throw new Error("Your session has expired. Please log in again.");
  }
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message || "Could not submit your score.");
  }
  return res.json();
}

export interface LeaderboardPage {
  items: ScoreEntry[];
  total: number;
}

export async function fetchLeaderboard(size = 50): Promise<LeaderboardPage> {
  const url = new URL(`${API_BASE}/api/leaderboard`);
  url.searchParams.set("size", String(size));
  const res = await fetch(url.toString());
  if (!res.ok) throw new Error("Could not load the leaderboard.");
  const items = await res.json();
  const total = Number(res.headers.get("X-Total-Count") ?? items.length);
  return { items, total };
}
