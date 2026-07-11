import { useEffect, useRef } from "react";

/**
 * Debounces per-key save calls so rapid edits (e.g. tapping +/- several times)
 * coalesce into a single request. Each key keeps its own timer; scheduling a key
 * again replaces its pending save with the latest one.
 *
 * `flush()` immediately runs every pending save and awaits all in-flight saves —
 * call it before advancing a phase so no edit is lost to the transition.
 */
export function useDebouncedSave(delayMs: number = 500) {
  const timers = useRef(new Map<string, ReturnType<typeof setTimeout>>());
  const pending = useRef(new Map<string, () => Promise<void>>());
  const inFlight = useRef(new Set<Promise<void>>());

  const run = (key: string, save: () => Promise<void>): void => {
    timers.current.delete(key);
    pending.current.delete(key);
    const promise = save().finally(() => inFlight.current.delete(promise));
    inFlight.current.add(promise);
  };

  const schedule = (key: string, save: () => Promise<void>): void => {
    const existing = timers.current.get(key);
    if (existing) clearTimeout(existing);
    pending.current.set(key, save);
    timers.current.set(key, setTimeout(() => run(key, save), delayMs));
  };

  const flush = async (): Promise<void> => {
    pending.current.forEach((save, key) => run(key, save));
    await Promise.all(Array.from(inFlight.current));
  };

  useEffect(() => {
    const active = timers.current;
    return () => { active.forEach((timer) => clearTimeout(timer)); };
  }, []);

  return { schedule, flush };
}
