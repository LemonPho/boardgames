import { useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { acceptInvite } from "../../../api/rooms";
import { useAlertsContext } from "../../../context/AlertsContext";

export default function AcceptInvitePage() {
  const { setErrorMessage } = useAlertsContext();
  const [ searchParams ] = useSearchParams();
  const token = searchParams.get("token");

  console.log("accept data page")

  const navigate = useNavigate();

  const putAcceptInvite = async (): Promise<void> => {
    if(token == undefined) return;
    const response = await acceptInvite(token, setErrorMessage);
    if(response) navigate(`/rooms/${response}`);
  }

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      await putAcceptInvite();
    }

    fetchData();
  }, []);

  return(null);
}