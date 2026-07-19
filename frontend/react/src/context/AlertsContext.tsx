import { createContext, useContext, useState } from "react";

interface AlertsContextType{
    errorMessage: string | null,
    setErrorMessage: (message: string) => void;
    successMessage: string | null,
    setSuccessMessage: (message: string | null) => void;
    infoMessage: string | null,
    setInfoMessage: (message: string | null) => void;

    clearAlerts: () => void;
}

const AlertsContext = createContext<AlertsContextType | null>(null);

export function AlertsContextProvider({ children }: { children: React.ReactNode }){
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [infoMessage, setInfoMessage] = useState<string | null>(null);

    const clearAlerts = () => {
        setErrorMessage(null);
        setSuccessMessage(null);
        setInfoMessage(null);
    }

    return(
        <AlertsContext.Provider
            value={{ errorMessage, setErrorMessage, successMessage, setSuccessMessage, infoMessage, setInfoMessage, clearAlerts}}
        >
            {children}
        </AlertsContext.Provider>
    )
}

export function useAlertsContext(){
    const ctx = useContext(AlertsContext);
    if (!ctx) throw new Error('useUI must be used within UIProvider');
    return ctx;
}