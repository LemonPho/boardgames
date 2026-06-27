import { createContext, useContext, useState } from "react";
import type { User } from "../types/user";
import { useAlertsContext } from "./AlertsContext";

interface ApplicationContextType{
    user: User | null,
}

const ApplicationContext = createContext<ApplicationContextType | null>(null);

export function ApplicationContextProvider({ children }: { children: React.ReactNode}){
    const { setErrorMessage } = useAlertsContext();

    const [user, setUser] = useState<User | null>(null);

    return(
        <ApplicationContext.Provider 
            value={{ user }}
        >
            {children}
        </ApplicationContext.Provider>
    )
}

export function useApplicationContext(){
    const ctx = useContext(ApplicationContext);
    if(!ctx) throw new Error("Application context not loaded");
    return ctx;
}