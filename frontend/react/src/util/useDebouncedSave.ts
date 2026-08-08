import { useEffect, useRef } from "react";

/**
 * Debounces per-key save calls so rapid edits (e.g. tapping +/- several times)
 * coalesce into a single request. Each key keeps its own timer; scheduling a key
 * again replaces its pending save with the latest one.
 *
 * `flush()` immediately runs every pending save and awaits all in-flight saves —
 * call it before advancing a phase so no edit is lost to the transition.
 *
 * Saves for the same key are chained so they commit in the order they were made:
 * submits send absolute values, so an out-of-order commit would leave a stale one
 * as the winner. Different keys still save in parallel.
 */
export function useDebouncedSave(delayMs: number = 500) {
  const timers = useRef(new Map<string, ReturnType<typeof setTimeout>>());
  const pending = useRef(new Map<string, () => Promise<void>>());
  const inFlight = useRef(new Set<Promise<void>>());
  // Tail of each key's save chain, so that key's next save can await it.
  const chains = useRef(new Map<string, Promise<void>>());

  const run = (key: string, save: () => Promise<void>): void => {
    // Cancel the debounce timer: when flush() runs a save early, the original
    // timer would otherwise still fire and re-send the value after the phase
    // has advanced — which the server then rejects as the wrong phase.
    const timer = timers.current.get(key);
    if (timer) clearTimeout(timer);
    timers.current.delete(key);
    pending.current.delete(key);

    const previous = chains.current.get(key) ?? Promise.resolve();
    const promise = previous
      .catch(() => {}) // a failed earlier save must not block the ones after it
      .then(() => save())
      .finally(() => {
        inFlight.current.delete(promise);
        if (chains.current.get(key) === promise) chains.current.delete(key);
      });
    chains.current.set(key, promise);
    inFlight.current.add(promise);
  };

  const schedule = (key: string, save: () => Promise<void>): void => {
    const existing = timers.current.get(key);
    if (existing) clearTimeout(existing);
    pending.current.set(key, save);
    timers.current.set(key, setTimeout(() => run(key, save), delayMs));
  };

  // allSettled, not all: a single failed save shouldn't leave the others unawaited.
  // Callers learn about failures through each save's own error path.
  const flush = async (): Promise<void> => {
    pending.current.forEach((save, key) => run(key, save));
    await Promise.allSettled(Array.from(inFlight.current));
  };

  useEffect(() => {
    const active = timers.current;
    return () => { active.forEach((timer) => clearTimeout(timer)); };
  }, []);

  return { schedule, flush };
}
