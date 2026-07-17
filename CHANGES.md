# Fixes applied — Quiz + Leaderboard (Task 2)

## Login credentials — verdict
Same underlying design as Task 1 (real bcrypt-hashed accounts, RS256 JWT,
no hardcoded credentials) and it's sound. The username here doubles as the
public leaderboard display name, which made the duplicate-username race
condition below more visible (two people could end up sharing one leaderboard
identity), so it was fixed the same way. See "Known trade-offs" at the bottom
for what wasn't changed and why.

## Bugs fixed

1. **Race condition on signup (duplicate usernames possible).** Same root
   cause as Task 1: `usernameTaken()` then `persist()` is a check-then-act
   that two concurrent signups could both pass, letting two accounts share
   one leaderboard name.
   - Added `MongoIndexInitializer` — creates a **unique index** on
     `users.username` at startup.
   - `AuthResource.signup` now catches the duplicate-key error and returns a
     normal `409` instead of a 500.

2. **Usernames could contain arbitrary characters**, and since the username
   is shown directly on the public leaderboard, this mattered more here than
   in Task 1 (e.g. whitespace-only or control-character names would look
   broken on the board). Added `@Pattern` restricting it to letters,
   numbers, `_ . -`, with a matching client-side check in `Signup.tsx`.

3. **No signup confirm-password field** — added, with a client-side match
   check, same as Task 1.

4. **Quiz options used `aria-pressed` instead of proper radio semantics.**
   Each question is a single-choice pick from a list, but the buttons were
   marked up as independent toggle buttons (`aria-pressed`), which doesn't
   tell a screen reader "these are mutually exclusive" or announce a count
   like "2 of 4". Changed to `role="radiogroup"` / `role="radio"` with
   `aria-checked`, `aria-labelledby` pointing at the question text, and
   arrow-key navigation between options (standard behavior for a radio
   group, since assistive tech expects it once that role is used).

5. **Leaderboard polled every 5s even when the tab was backgrounded**, and a
   slow response could overlap with the next scheduled poll. Now the poll:
   - pauses via the `visibilitychange` event when the tab isn't visible
     (and refreshes immediately + resumes when it becomes visible again),
   - skips starting a new request if the previous one hasn't resolved yet.

6. **Whitespace wasn't trimmed** before validating usernames on signup/
   login, same fix as Task 1.

## Accessibility fixes

- **Insufficient color contrast.** `--text-faint` was `#56705f` on the dark
  green background — ~3.4:1, below the WCAG AA minimum of 4.5:1 for the hint
  text that uses it (e.g. "At least 8 characters."). Lightened to `#8fada0`
  (~7.6:1).
- **Missing `autocomplete` attributes** on login/signup fields — added
  `username`, `current-password`, `new-password`, `email`.
- **Quiz radiogroup fix above** is itself an accessibility fix as much as a
  correctness one.

## Known trade-offs (not changed, flagged for awareness)

- **JWT kept in `localStorage`** for the same reason as Task 1 (keeps the
  session across a refresh); same XSS caveat applies.
- **The 6-question bank is hardcoded in `Question.java`.** That's fine per
  the assignment brief ("any topic - general knowledge is fine"), just
  noting it's not currently editable without a redeploy.

---

# Round 2 — resubmission guard, option shuffling, tests, rate limiting, ops

## New backend capabilities

- **One attempt per account.** Previously nothing stopped someone from
  retaking the quiz and submitting again, racking up multiple leaderboard
  entries under their own name. `POST /api/scores` now checks
  `ScoreEntry.hasSubmitted()` and rejects a second submission with `409`;
  a unique index on `scores.name` (via `MongoIndexInitializer`) backs that
  up against the same check-then-act race the username-uniqueness fix in
  Round 1 addressed.
- **Answer options are shuffled per fetch.** `GET /api/questions` now
  returns each question's options in a random order every time (so two
  people comparing screens - or a shared screenshot - don't show identical
  option positions). This didn't require any server-side session state:
  each option DTO carries a stable `id` (its original index) independent of
  display position, so the frontend submits answers by that id rather than
  by position, and scoring is unaffected by how a particular request's
  options happened to be shuffled. See `QuestionDTO`.
- **Rate limiting** on `/api/auth/login` (10/min per IP+username) and
  `/api/auth/signup` (20/min per IP), same implementation as Task 1.
- **Leaderboard pagination.** `GET /api/leaderboard` accepts `page`/`size`
  (default 50, capped at 200) and returns `X-Total-Count`; the frontend has
  a "Show more" button instead of always pulling every score ever
  submitted.
- **Health check** via `quarkus-smallrye-health` (`/q/health`, `/live`,
  `/ready`), same as Task 1.
- **Backend tests.** `AuthResourceTest` (mirrors Task 1's) plus
  `QuizResourceTest`, which specifically verifies scoring is correct
  *despite* the options coming back shuffled (submits answers by option id,
  not position), that a second submission from the same account is
  rejected, and that the leaderboard is public and paginated. Same Dev
  Services setup as Task 1 - needs Docker for `mvn test`.
- **Fresh JWT keypair** generated for this project, same reasoning as
  Task 1.

## New frontend capabilities

- **`Quiz.tsx`** updated for the new `{id, text}` option shape: selection
  state and submission now key off the option's stable `id`, not its
  position in the (now shuffled) display order. Keyboard arrow-navigation
  between options was adjusted to compute display position separately from
  the stored id.
- **`Leaderboard.tsx`** shows a total participant count and a "Show more"
  button once there are more entries than currently displayed.
- **Frontend tests** (Vitest + RTL): `Quiz.test.tsx` (Next disabled until a
  selection is made, submission is keyed by option id even when options are
  displayed out of their "natural" order, and the "already submitted" server
  error is surfaced without crashing) and `Signup.test.tsx` (same coverage
  as Task 1).

## CI

- Added `.github/workflows/ci.yml`, same structure as Task 1.

## Still not done (flagged, not silently skipped)

- **Rate limiter is per-instance, not distributed** - same caveat as Task 1.
- **JWT in localStorage** - unchanged, same reasoning as Round 1.
