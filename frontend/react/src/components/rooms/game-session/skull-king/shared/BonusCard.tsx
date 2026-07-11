import { bonusTotal, type TeamBonus } from "../../../../../types/skull-king";

interface BonusCardProps {
  playerName: string;
  bonus: TeamBonus;
  // Bonuses are only available to a team that made a bid of one or more.
  eligible: boolean;
  // Why the team is ineligible, shown when eligible is false.
  ineligibleReason?: string;
  submitted: boolean;
  editable: boolean;
  onChange: (next: TeamBonus) => void;
  onSubmit: () => void;
}

export function BonusCard({
  playerName,
  bonus,
  eligible,
  ineligibleReason,
  submitted,
  editable,
  onChange,
  onSubmit,
}: BonusCardProps) {
  const locked = submitted || !editable;

  const setCount = (key: keyof TeamBonus, delta: number, max: number): void => {
    const current = bonus[key] as number;
    const next = current + delta;
    if (next < 0 || next > max) return;
    onChange({ ...bonus, [key]: next });
  };

  const toggle = (key: keyof TeamBonus): void => {
    onChange({ ...bonus, [key]: !(bonus[key] as boolean) });
  };

  return (
    <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-2xl overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-neutral-100 dark:border-neutral-800">
        <span className="font-semibold truncate">{playerName}</span>
        {eligible ? (
          <span className="text-sm font-semibold tabular-nums px-2.5 py-1 rounded-full bg-neutral-100 dark:bg-neutral-800">
            +{bonusTotal(bonus)}
          </span>
        ) : (
          <span className="text-xs font-medium text-amber-600 dark:text-amber-500 px-2.5 py-1 rounded-full bg-amber-50 dark:bg-amber-950/40">
            no bonus
          </span>
        )}
      </div>

      {!eligible ? (
        <p className="px-4 py-6 text-sm text-center text-neutral-500 dark:text-neutral-400">
          {ineligibleReason ?? "No bonus points this round."}
        </p>
      ) : (
        <div className="p-4 flex flex-col gap-4">
          <Section title="Cards captured">
            <Stepper
              label="Standard 14s" points="+10 each" locked={locked}
              value={bonus.standardFourteens} max={3}
              onDec={() => setCount("standardFourteens", -1, 3)}
              onInc={() => setCount("standardFourteens", 1, 3)}
            />
            <Toggle
              label="Black 14" points="+20" locked={locked}
              active={bonus.blackFourteen} onToggle={() => toggle("blackFourteen")}
            />
          </Section>

          <Section title="Character captures">
            <Stepper
              label="Mermaid × Pirate" points="+20 each" locked={locked}
              value={bonus.mermaidsByPirate} max={2}
              onDec={() => setCount("mermaidsByPirate", -1, 2)}
              onInc={() => setCount("mermaidsByPirate", 1, 2)}
            />
            <Stepper
              label="Pirate × Skull King" points="+30 each" locked={locked}
              value={bonus.piratesBySkullKing} max={5}
              onDec={() => setCount("piratesBySkullKing", -1, 5)}
              onInc={() => setCount("piratesBySkullKing", 1, 5)}
            />
            <Toggle
              label="Skull King × Mermaid" points="+40" featured locked={locked}
              active={bonus.skullKingByMermaid} onToggle={() => toggle("skullKingByMermaid")}
            />
          </Section>

          <Section title="Loot">
            <Stepper
              label="Coins collected" points="+20 each" locked={locked}
              value={bonus.loot} max={2}
              onDec={() => setCount("loot", -1, 2)}
              onInc={() => setCount("loot", 1, 2)}
            />
          </Section>

          <button
            type="button"
            onClick={onSubmit}
            disabled={locked}
            className="w-full h-10 rounded-xl text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-50 active:scale-[0.98] transition"
          >
            {submitted ? "Locked in" : "Submit"}
          </button>
        </div>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-[11px] font-semibold uppercase tracking-wide text-neutral-400 dark:text-neutral-500">
        {title}
      </span>
      {children}
    </div>
  );
}

interface StepperProps {
  label: string;
  points: string;
  value: number;
  max: number;
  locked: boolean;
  onDec: () => void;
  onInc: () => void;
}

function Stepper({ label, points, value, max, locked, onDec, onInc }: StepperProps) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl bg-neutral-50 dark:bg-neutral-800/50 px-3 py-2">
      <div className="min-w-0">
        <div className="text-sm truncate">{label}</div>
        <div className="text-xs text-neutral-400">{points}</div>
      </div>
      <div className="flex items-center gap-2 shrink-0">
        <button
          type="button"
          aria-label={`Decrease ${label}`}
          onClick={onDec}
          disabled={locked || value <= 0}
          className="w-8 h-8 rounded-full flex items-center justify-center text-base border border-neutral-300 dark:border-neutral-700 disabled:opacity-30 active:scale-95 transition"
        >
          −
        </button>
        <span className="text-base font-semibold min-w-[20px] text-center tabular-nums">
          {value}
        </span>
        <button
          type="button"
          aria-label={`Increase ${label}`}
          onClick={onInc}
          disabled={locked || value >= max}
          className="w-8 h-8 rounded-full flex items-center justify-center text-base border border-neutral-300 dark:border-neutral-700 disabled:opacity-30 active:scale-95 transition"
        >
          +
        </button>
      </div>
    </div>
  );
}

interface ToggleProps {
  label: string;
  points: string;
  active: boolean;
  locked: boolean;
  featured?: boolean;
  onToggle: () => void;
}

function Toggle({ label, points, active, locked, featured, onToggle }: ToggleProps) {
  const base = "w-full flex items-center justify-between gap-3 rounded-xl px-3 border transition active:scale-[0.99] disabled:active:scale-100";
  const height = featured ? "py-3" : "py-2";
  const activeStyle = "bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 border-neutral-900 dark:border-neutral-100";
  const inactiveStyle = "bg-neutral-50 dark:bg-neutral-800/50 border-neutral-200 dark:border-neutral-700";

  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={active}
      onClick={onToggle}
      disabled={locked}
      className={`${base} ${height} ${active ? activeStyle : inactiveStyle} disabled:opacity-50`}
    >
      <div className="flex items-center gap-2.5 min-w-0">
        <span
          className={`w-5 h-5 rounded-md border flex items-center justify-center text-xs shrink-0 ${
            active
              ? "bg-white/20 border-white/40 dark:bg-black/10 dark:border-black/30"
              : "border-neutral-300 dark:border-neutral-600"
          }`}
        >
          {active ? "✓" : ""}
        </span>
        <span className={`truncate ${featured ? "text-sm font-medium" : "text-sm"}`}>{label}</span>
      </div>
      <span className={`text-xs shrink-0 ${active ? "opacity-80" : "text-neutral-400"}`}>{points}</span>
    </button>
  );
}
