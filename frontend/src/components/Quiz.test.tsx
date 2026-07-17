import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import Quiz from "./Quiz";
import * as client from "../api/client";
import type { Question } from "../api/client";

const TWO_QUESTIONS: Question[] = [
  {
    id: 1,
    text: "What does HTML stand for?",
    // Deliberately shuffled: the correct option (id 1) is NOT first.
    options: [
      { id: 3, text: "Hyperlink Text Markup Language" },
      { id: 1, text: "HyperText Markup Language" },
      { id: 0, text: "Hyper Trainer Marking Language" },
      { id: 2, text: "HighText Machine Language" },
    ],
  },
  {
    id: 2,
    text: "Which planet is known as the Red Planet?",
    options: [
      { id: 2, text: "Mars" },
      { id: 0, text: "Venus" },
      { id: 1, text: "Jupiter" },
      { id: 3, text: "Saturn" },
    ],
  },
];

describe("Quiz", () => {
  it("disables Next until an option is selected, then advances", async () => {
    vi.spyOn(client, "fetchQuestions").mockResolvedValue(TWO_QUESTIONS);
    const user = userEvent.setup();
    render(<Quiz onFinished={vi.fn()} />);

    await screen.findByText("What does HTML stand for?");
    expect(screen.getByRole("button", { name: /next/i })).toBeDisabled();

    await user.click(screen.getByRole("radio", { name: "HyperText Markup Language" }));
    expect(screen.getByRole("button", { name: /next/i })).toBeEnabled();

    await user.click(screen.getByRole("button", { name: /next/i }));
    expect(await screen.findByText("Which planet is known as the Red Planet?")).toBeInTheDocument();
  });

  it("submits answers keyed by option id (not display position) on the final question", async () => {
    vi.spyOn(client, "fetchQuestions").mockResolvedValue(TWO_QUESTIONS);
    const submitSpy = vi.spyOn(client, "submitScore").mockResolvedValue({
      id: "abc123",
      name: "ada",
      score: 2,
      totalQuestions: 2,
      submittedAt: new Date().toISOString(),
    });
    const onFinished = vi.fn();
    const user = userEvent.setup();
    render(<Quiz onFinished={onFinished} />);

    await screen.findByText("What does HTML stand for?");
    // "HyperText Markup Language" is displayed second but has stable id 1.
    await user.click(screen.getByRole("radio", { name: "HyperText Markup Language" }));
    await user.click(screen.getByRole("button", { name: /next/i }));

    await screen.findByText("Which planet is known as the Red Planet?");
    // "Mars" is displayed first but has stable id 2.
    await user.click(screen.getByRole("radio", { name: "Mars" }));
    await user.click(screen.getByRole("button", { name: /finish quiz/i }));

    await waitFor(() => expect(submitSpy).toHaveBeenCalledWith({ 1: 1, 2: 2 }));
    await waitFor(() => expect(onFinished).toHaveBeenCalledWith(2, 2));
  });

  it("shows the server's message (e.g. already submitted) instead of crashing", async () => {
    vi.spyOn(client, "fetchQuestions").mockResolvedValue(TWO_QUESTIONS);
    vi.spyOn(client, "submitScore").mockRejectedValue(
      new Error("You've already taken this quiz. Only one attempt per account is allowed.")
    );
    const user = userEvent.setup();
    render(<Quiz onFinished={vi.fn()} />);

    await screen.findByText("What does HTML stand for?");
    await user.click(screen.getByRole("radio", { name: "HyperText Markup Language" }));
    await user.click(screen.getByRole("button", { name: /next/i }));

    await screen.findByText("Which planet is known as the Red Planet?");
    await user.click(screen.getByRole("radio", { name: "Mars" }));
    await user.click(screen.getByRole("button", { name: /finish quiz/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/already taken this quiz/i);
  });
});
