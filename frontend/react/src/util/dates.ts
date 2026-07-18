// JSON has no date type, so timestamps arrive as ISO strings. These helpers
// revive them into real Date objects at the API boundary, so components can
// type such fields as `Date` and use them directly.

/** Revive an ISO date string into a Date (null-safe). */
export function toDate(value: string | null | undefined): Date | null {
  return value ? new Date(value) : null;
}

/**
 * Return a shallow copy of `obj` with the given keys converted from ISO strings
 * to Date objects. Use in API functions to revive date fields on a response.
 *   reviveDates(row, "playedAt")
 */
export function reviveDates<T>(obj: T, ...keys: (keyof T)[]): T {
  const copy = { ...obj };
  for (const key of keys) {
    const value = copy[key];
    if (typeof value === "string") {
      copy[key] = new Date(value) as T[keyof T];
    }
  }
  return copy;
}
