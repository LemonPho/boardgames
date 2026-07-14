import { useEffect, useState } from "react";
import { getRoundHistory, correctBids, correctTricks, correctBonus, setKraken } from "../../../../api/skullKing";
import { useAlertsContext } from "../../../../context/AlertsContext";
import { useRoomContext } from "../../../../context/RoomContext";
import { EMPTY_BONUS, bonusEligibility, type RoundHistory, type TeamBonus } from "../../../../types/skull-king";
import { AdminCounterCard } from "./shared/AdminCounterCard";
import { BonusCard } from "./shared/BonusCard";
import { KrakenToggle } from "./shared/KrakenToggle";
import type { TeamResponse } from "../../../../types/teams";

type Section = "bids" | "tricks" | "bonus";

interface PastRoundViewProps {
  roomName: string;
  round: number;
  onBack: () => void;
}

export default function PastRoundView({ roomName, round, onBack }: PastRoundViewProps) {
  const { setErrorMessage } = useAlertsContext();
  const { currentPlayer } = useRoomContext();
  const [history, setHistory] = useState<RoundHistory | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<Section | null>(null);
  const [saving, setSaving] = useState(false);

  // Section drafts, seeded when editing starts.
  const [bidDrafts, setBidDrafts] = useState<Map<string, number>>(new Map());
  const [trickDrafts, setTrickDrafts] = useState<Map<string, number>>(new Map());
  const [bonusDrafts, setBonusDrafts] = useState<Map<string, TeamBonus>>(new Map());
  const [krakenDraft, setKrakenDraft] = useState<boolean>(false);

  const isAdmin = currentPlayer?.role === "ADMIN";

  const load = async () => {
    try {
      setLoading(true);
      const data = await getRoundHistory(roomName, round, setErrorMessage);
      setHistory(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const data = await getRoundHistory(roomName, round, setErrorMessage);
        if (!cancelled) setHistory(data);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [roomName, round]);

  if (loading) return <Frame round={round} onBack={onBack}><p className="p-4 text-center text-sm text-gray-500">Loading…</p></Frame>;
  if (!history) return <Frame round={round} onBack={onBack}><p className="p-4 text-center text-sm text-gray-500">Couldn't load this round.</p></Frame>;

  const teams = history.teams;
  const cardCount = history.cardCount;

  const startEdit = (section: Section): void => {
    setBidDrafts(new Map(teams.map((t) => [t.teamId, t.bid ?? 0])));
    setTrickDrafts(new Map(teams.map((t) => [t.teamId, t.tricksWon ?? 0])));
    setBonusDrafts(new Map(teams.map((t) => [t.teamId, t.bonus ?? EMPTY_BONUS])));
    setKrakenDraft(history.krakenPlayed);
    setEditing(section);
  };

  const cancelEdit = (): void => setEditing(null);

  const saveBids = async (): Promise<void> => {
    setSaving(true);
    try {
      await correctBids(roomName, round, teams.map((t) => ({ teamId: t.teamId, value: bidDrafts.get(t.teamId) ?? 0 })), setErrorMessage);
      setEditing(null);
      await load();
    } catch { /* error surfaced */ } finally { setSaving(false); }
  };

  const saveTricks = async (): Promise<void> => {
    setSaving(true);
    try {
      // Set the Kraken flag first so the trick-total validation uses the new
      // expected sum (cardCount − 1 when a Kraken destroyed a trick).
      if (krakenDraft !== history.krakenPlayed) {
        await setKraken(roomName, round, krakenDraft, setErrorMessage);
      }
      await correctTricks(roomName, round, teams.map((t) => ({ teamId: t.teamId, value: trickDrafts.get(t.teamId) ?? 0 })), setErrorMessage);
      setEditing(null);
      await load();
    } catch { /* error surfaced */ } finally { setSaving(false); }
  };

  const saveBonus = async (): Promise<void> => {
    setSaving(true);
    try {
      await correctBonus(roomName, round, teams.map((t) => ({ teamId: t.teamId, ...(bonusDrafts.get(t.teamId) ?? EMPTY_BONUS) })), setErrorMessage);
      setEditing(null);
      await load();
    } catch { /* error surfaced */ } finally { setSaving(false); }
  };

  // Values shown: drafts while editing that section, otherwise the loaded round.
  const bidValue = (id: string) => editing === "bids" ? (bidDrafts.get(id) ?? 0) : (teams.find(t => t.teamId === id)?.bid ?? 0);
  const trickValue = (id: string) => editing === "tricks" ? (trickDrafts.get(id) ?? 0) : (teams.find(t => t.teamId === id)?.tricksWon ?? 0);
  const bonusValue = (id: string): TeamBonus => editing === "bonus" ? (bonusDrafts.get(id) ?? EMPTY_BONUS) : (teams.find(t => t.teamId === id)?.bonus ?? EMPTY_BONUS);

  const pseudoTeam = (t: RoundHistory["teams"][number]): TeamResponse => ({
    id: t.teamId, name: null,
    player: { id: t.teamId, displayName: t.playerName ?? "—" } as TeamResponse["player"],
    finalScore: 0, winner: false, createdAt: new Date(),
  });

  return (
    <Frame round={round} cardCount={cardCount} onBack={onBack}>
      <div className="max-w-md mx-auto p-4 flex flex-col gap-6">
        {/* Bids */}
        <Section
          title="Bids"
          canEdit={isAdmin}
          editing={editing === "bids"}
          disabled={editing !== null && editing !== "bids"}
          saving={saving}
          onEdit={() => startEdit("bids")}
          onCancel={cancelEdit}
          onSave={saveBids}
        >
          <div className="grid grid-cols-2 gap-3">
            {teams.map((t) => (
              <AdminCounterCard
                key={`${t.teamId}-${editing === "bids"}`}
                playerName={t.playerName ?? "—"}
                editable={editing === "bids"}
                team={pseudoTeam(t)}
                serverValue={bidValue(t.teamId)}
                max={cardCount}
                change={(id, v) => setBidDrafts((prev) => new Map(prev).set(id, v))}
              />
            ))}
          </div>
        </Section>

        {/* Tricks */}
        <Section
          title="Tricks won"
          canEdit={isAdmin}
          editing={editing === "tricks"}
          disabled={editing !== null && editing !== "tricks"}
          saving={saving}
          onEdit={() => startEdit("tricks")}
          onCancel={cancelEdit}
          onSave={saveTricks}
        >
          <div className="mb-3">
            <KrakenToggle
              krakenPlayed={editing === "tricks" ? krakenDraft : history.krakenPlayed}
              editable={editing === "tricks"}
              onToggle={setKrakenDraft}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            {teams.map((t) => (
              <AdminCounterCard
                key={`${t.teamId}-${editing === "tricks"}`}
                playerName={t.playerName ?? "—"}
                editable={editing === "tricks"}
                team={pseudoTeam(t)}
                serverValue={trickValue(t.teamId)}
                max={cardCount}
                change={(id, v) => setTrickDrafts((prev) => new Map(prev).set(id, v))}
              />
            ))}
          </div>
        </Section>

        {/* Bonus */}
        <Section
          title="Bonus points"
          canEdit={isAdmin}
          editing={editing === "bonus"}
          disabled={editing !== null && editing !== "bonus"}
          saving={saving}
          onEdit={() => startEdit("bonus")}
          onCancel={cancelEdit}
          onSave={saveBonus}
        >
          <div className="flex flex-col gap-3">
            {teams.map((t) => {
              const { eligible, reason } = bonusEligibility(bidValue(t.teamId), trickValue(t.teamId));
              return (
                <BonusCard
                  key={t.teamId}
                  playerName={t.playerName ?? "—"}
                  bonus={bonusValue(t.teamId)}
                  eligible={eligible}
                  ineligibleReason={reason}
                  editable={editing === "bonus"}
                  onChange={(next) => setBonusDrafts((prev) => new Map(prev).set(t.teamId, next))}
                />
              );
            })}
          </div>
        </Section>
      </div>
    </Frame>
  );
}

function Frame({ round, cardCount, onBack, children }: { round: number; cardCount?: number; onBack: () => void; children: React.ReactNode }) {
  return (
    <div>
      <header className="flex items-center gap-3 px-4 h-12 border-b border-neutral-200">
        <button type="button" onClick={onBack} className="text-sm font-medium text-gray-500 hover:text-gray-800 transition-colors">
          ← Back
        </button>
        <span className="text-sm text-neutral-500">
          Round {round}{cardCount ? ` · ${cardCount} cards` : ""}
        </span>
      </header>
      {children}
    </div>
  );
}

function Section({
  title, canEdit, editing, disabled, saving, onEdit, onCancel, onSave, children,
}: {
  title: string;
  canEdit: boolean;
  editing: boolean;
  disabled: boolean;
  saving: boolean;
  onEdit: () => void;
  onCancel: () => void;
  onSave: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className={disabled ? "opacity-50 pointer-events-none" : ""}>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-base font-semibold">{title}</h2>
        {canEdit && (
          editing ? (
            <div className="flex items-center gap-2">
              <button type="button" onClick={onCancel} disabled={saving} className="text-sm text-gray-500 hover:text-gray-800 disabled:opacity-40">
                Cancel
              </button>
              <button type="button" onClick={onSave} disabled={saving} className="text-sm font-medium px-3 py-1 rounded-lg bg-gray-800 text-white disabled:opacity-40">
                {saving ? "Saving…" : "Save"}
              </button>
            </div>
          ) : (
            <button type="button" onClick={onEdit} disabled={disabled} className="text-sm font-medium px-3 py-1 rounded-lg border border-gray-200 hover:border-gray-400 disabled:opacity-40">
              Edit
            </button>
          )
        )}
      </div>
      {children}
    </div>
  );
}
