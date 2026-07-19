import { useAlertsContext } from "../../context/AlertsContext";

interface SubmitButtonProps {
  // Idle label; replaced by "Loading…" while the submit runs. Optional when
  // `children` is provided (e.g. an icon + label).
  text?: string;
  // Custom idle content (icons, etc.); replaced by "Loading…" while running.
  children?: React.ReactNode;
  // Loading flag lives in the parent so it can also gate other inputs.
  loading: boolean;
  setLoading: (value: boolean) => void;
  // The async work to run on click.
  onSubmit: () => Promise<void>;
  // Extra disable condition (e.g. invalid form), on top of the loading guard.
  disabled?: boolean;
  className?: string;
  type?: "button" | "submit";
}

/**
 * A submit button that manages the loading lifecycle: on click it flips the
 * parent's loading flag, runs onSubmit, shows "Loading…" and disables itself
 * until the work finishes — so it can't be spam-submitted. Loading is always
 * cleared, even if onSubmit throws.
 */
export default function SubmitButton({
  text,
  children,
  loading,
  setLoading,
  onSubmit,
  disabled = false,
  className,
  type = "button",
}: SubmitButtonProps) {
  const { clearAlerts } = useAlertsContext();

  const handleClick = async (): Promise<void> => {
    if (loading) return; // ignore repeat clicks while in flight
    
    clearAlerts();

    setLoading(true);
    try {
      await onSubmit();
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type={type}
      onClick={handleClick}
      disabled={loading || disabled}
      className={
        className ??
        "w-full text-sm font-medium py-2.5 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition disabled:opacity-40"
      }
    >
      {loading ? "Loading…" : (children ?? text)}
    </button>
  );
}
