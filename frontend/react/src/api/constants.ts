import axios from "axios";

export const baseUrl = "/api";
export const api = axios.create({
    baseURL: baseUrl
});
export const auth = axios.create({
    baseURL: "/auth"
})