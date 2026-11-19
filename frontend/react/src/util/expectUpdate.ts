const DEFAULT_TIMEOUT_MS = 2000;

/**
 * After a successful mutation, wait for the server's WebSocket push; if none arrives within
 * `timeoutMs`, fall back to a REST refresh. Replaces "always re-fetch after every request",
 * which defeated the purpose of the socket, while still self-healing a lost push.
 */
export function createUpdateWaiter(): {
  notify: () => void;
  mark: () => number;
  expect: (refresh: () => Promise<void>, since: number, timeoutMs?: number) => void;
} {
  // Each pending expectation is just its timeout handle; any push satisfies all of them,
  // since every push carries the full current snapshot.
  let pending: ReturnType<typeof setTimeout>[] = [];
  // Count of pushes received. The server publishes after commit, which usually beats the
  // HTTP response, so a caller marks *before* its request and expect() treats any push
  // since that mark as the confirmation, instead of waiting for a second one.
  let seq = 0;

  const notify = (): void => {
    seq++;
    pending.forEach(clearTimeout);
    pending = [];
  };

  const mark = (): number => seq;

  // Fire-and-forget: callers don't await this, so buttons stop loading as soon as the
  // request itself returns instead of sitting through the timeout.
  const expect = (refresh: () => Promise<void>, since: number, timeoutMs: number = DEFAULT_TIMEOUT_MS): void => {
    if (seq > since) return; // the push already arrived (raced ahead of the response)
    const handle = setTimeout(() => {
      pending = pending.filter((h) => h !== handle);
      // Best-effort resync; nobody is awaiting this, so don't leak an unhandled rejection.
      refresh().catch(() => {});
    }, timeoutMs);
    pending.push(handle);
  };

  return { notify, mark, expect };
}
