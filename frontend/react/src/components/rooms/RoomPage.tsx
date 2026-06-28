import { Outlet } from "react-router-dom";
import { RoomContextProvider } from "../../context/RoomContext";

export default function RoomPage(){

    return(
        <RoomContextProvider>
            <Outlet />
        </RoomContextProvider>
    );
}