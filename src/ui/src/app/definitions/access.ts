export class User {
    id?: number;
    buildIn?: boolean;
    name?: string;
    realm?: string;
    givenName?: string;
    surname?: string;
    password?: string;
    description?: string;
    email?: string;
    lastLogin?: Date;
    validFrom?: Date;
    validUntil?: Date;
}

export class Group {
    id?: number;
    buildIn?: boolean;
    name?: string;
    description?: string;
    user?: Array<User>;
}
