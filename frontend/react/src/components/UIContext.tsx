import { createContext, useContext, useState, type ReactNode } from "react";

interface UIContextType {
    activePanel: string | null,
    togglePanel: (panel: string) => void,
    closePanel: () => void,
}

const UIContext = createContext<UIContextType | null>(null);

export function UIProvider({ children }: { children: ReactNode}){
    const [activePanel, setActivePanel] = useState<string | null>(null);

    const togglePanel = (panel: string): void => {
        console.log("Panel: " + panel + " toggled");
        if(activePanel == panel){
            closePanel();
            return;
        }
        setActivePanel(panel);
    }

    const closePanel = (): void => {
        setActivePanel(null);
    }

    return(
        <UIContext.Provider
            value = {{ activePanel, togglePanel, closePanel }}
        >
            {children}
        </UIContext.Provider>
    )
}

export function useUIContext() {
  const ctx = useContext(UIContext)
  if (!ctx) throw new Error('useUI must be used within UIProvider')
  return ctx
}