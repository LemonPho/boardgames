export interface RegisterErrors{
    username: string,
    email: string,
    password: string
}

export interface LoginErrors{
    userExists: string,
    password: string
}