import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { HoldCountdown } from "./HoldCountdown";

describe("HoldCountdown", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-09-29T10:00:00Z"));
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("counts down and reports when the hold runs out", () => {
    const onExpire = vi.fn();
    render(<HoldCountdown expiresAt="2026-09-29T10:01:05Z" onExpire={onExpire} />);

    expect(screen.getByText("1:05")).toBeTruthy();

    act(() => {
      vi.advanceTimersByTime(5_000);
    });
    expect(screen.getByText("1:00")).toBeTruthy();
    expect(onExpire).not.toHaveBeenCalled();

    act(() => {
      vi.advanceTimersByTime(60_000);
    });
    expect(onExpire).toHaveBeenCalledTimes(1);
  });

  it("blocks payment straight away when the hold is already over", () => {
    const onExpire = vi.fn();
    render(<HoldCountdown expiresAt="2026-09-29T09:59:00Z" onExpire={onExpire} />);

    expect(onExpire).toHaveBeenCalledTimes(1);
  });
});
