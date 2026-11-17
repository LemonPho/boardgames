import { useState } from "react";
import { MoreVertical } from "lucide-react";
import { useUIContext } from "../../../../context/UIContext";
import Dropdown from "../../../util/Dropdown";

export interface AdminMenuItem {
  key: string;
  label: string;
  icon?: React.ReactNode;        // lucide icon, size 16
  destructive?: boolean;         // red styling, rendered after a divider
  hidden?: boolean;
  onSelect: () => Promise<void> | void;
}

/**
 * The admin's "game options" menu in the session header. Data-driven: callers pass
 * the items, so new admin actions don't need changes here. Destructive items are
 * grouped last, below a divider, away from the everyday ones.
 */
export default function SessionAdminMenu({ id, items }: { id: string; items: AdminMenuItem[] }) {
  const { togglePanel, closePanel } = useUIContext();
  // One flag for the whole menu: while any action runs, every item is disabled so a
  // second action can't start on top of it.
  const [busy, setBusy] = useState(false);

  const visible = items.filter((item) => !item.hidden);
  const regular = visible.filter((item) => !item.destructive);
  const destructive = visible.filter((item) => item.destructive);

  const handleSelect = async (event: React.MouseEvent, item: AdminMenuItem): Promise<void> => {
    // Stop the layout's click-anywhere handler; we close the panel ourselves.
    event.stopPropagation();
    if (busy) return;
    closePanel();
    setBusy(true);
    try {
      await item.onSelect();
    } catch {
      // The API layer has already shown the error toast.
    } finally {
      setBusy(false);
    }
  };

  const renderItem = (item: AdminMenuItem) => (
    <button
      key={item.key}
      type="button"
      disabled={busy}
      onClick={(event) => handleSelect(event, item)}
      className={`w-full text-left px-3 py-2 text-sm flex items-center gap-2 hover:bg-neutral-50 disabled:opacity-40 ${item.destructive ? "text-red-500" : "text-neutral-800"}`}
    >
      {item.icon}
      {item.label}
    </button>
  );

  return (
    <div className="relative">
      <button
        type="button"
        aria-label="Game options"
        onClick={(event) => { event.stopPropagation(); togglePanel(id); }}
        className="h-9 w-9 flex items-center justify-center rounded-lg border border-neutral-200 dark:border-neutral-700 hover:border-neutral-400 dark:hover:border-neutral-500 transition"
      >
        <MoreVertical size={18} />
      </button>
      <Dropdown id={id}>
        {regular.map(renderItem)}
        {destructive.length > 0 && regular.length > 0 && <hr className="my-1 border-neutral-100" />}
        {destructive.map(renderItem)}
      </Dropdown>
    </div>
  );
}
