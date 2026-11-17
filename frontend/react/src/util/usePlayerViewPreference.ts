import { useState } from "react";

const storageKey = (roomName: string): string => `sk:playerView:${roomName}`;

// localStorage can throw (e.g. Safari private mode, storage disabled), and the
// preference is purely cosmetic, so any failure just means "off" / "not saved".
const readPreference = (roomName: string | undefined): boolean => {
  if (!roomName) return false;
  try {
    return localStorage.getItem(storageKey(roomName)) === "1";
  } catch {
    return false;
  }
};

// Admin's "player view" display preference for one room, persisted in localStorage so it
// survives reloads and round changes (LiveRoundDataProvider remounts every round).
// Client-side only: the admin still receives the full admin state; we just render less.
export function usePlayerViewPreference(roomName: string | undefined): [boolean, (v: boolean) => void] {
  const [enabled, setEnabled] = useState<boolean>(() => readPreference(roomName));

  const setPreference = (value: boolean): void => {
    setEnabled(value);
    if (!roomName) return;
    try {
      if (value) localStorage.setItem(storageKey(roomName), "1");
      else localStorage.removeItem(storageKey(roomName));
    } catch {
      // Not persisted; the in-memory value still applies for this page load.
    }
  };

  return [enabled, setPreference];
}
