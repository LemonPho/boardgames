import { createContext, useContext, useState } from "react";
import type { UserResponse } from "../types/user";
import { useAlertsContext } from "./AlertsContext";
import { getCurrentUser } from "../api/user";

interface UserContextType{
    user: UserResponse | null,
    retrieveCurrentUser: () => void,
}

const UserContext = createContext<UserContextType | null>(null);

export function UserContextProvider({ children }: { children: React.ReactNode}){
    const { setErrorMessage } = useAlertsContext();

    const [user, setUser] = useState<UserResponse | null>(null);

    const retrieveCurrentUser = async ():Promise<void> => {
        const user = await getCurrentUser(setErrorMessage);
        setUser(user);
    }

    return(
        <UserContext.Provider 
            value={{ user, retrieveCurrentUser }}
        >
            {children}
        </UserContext.Provider>
    )
}

export function useUserContext(){
    const ctx = useContext(UserContext);
    if(!ctx) throw new Error("Application context not loaded");
    return ctx;
}