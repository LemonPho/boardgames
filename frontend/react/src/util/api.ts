import axios from "axios";
import Cookies from "js-cookie";

export const setAxiosError = (error: unknown, setErrorMessage: (message: string) => void): void => {
    if(axios.isAxiosError(error)){
        const data = error.response?.data;
        if(data == null || data === ""){
            setErrorMessage("An error occured");
        } else if(typeof data === "object"){
            // Validation errors come back as a { field: message } map; show the messages.
            setErrorMessage(Object.values(data as Record<string, string>).join(" "));
        } else {
            setErrorMessage(String(data));
        }
    } else {
        setErrorMessage("An error occured");
    }
}

export function getCsrfCookie(){
    return Cookies.get("XSRF-TOKEN");
}