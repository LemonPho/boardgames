import axios from "axios";
import Cookies from "js-cookie";

export const setAxiosError = (error: unknown, setErrorMessage: (message: string) => void): void => {
    if(axios.isAxiosError(error) && error.response?.data == ""){
        setErrorMessage("An error occured");
    } else {
        setErrorMessage(error.response?.data);
    }
}

export function getCsrfCookie(){
    return Cookies.get("XSRF-TOKEN");
}