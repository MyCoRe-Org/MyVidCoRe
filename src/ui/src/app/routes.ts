export enum RouteFlag {
    ACTIVE_EQ,
    AUTO_HIDE,
    DIVIDER,
}

export interface RouteItem {
    id?: string;
    ref?: string;
    i18n?: string;
    icon?: string;
    flag?: number;
    roles?: Array<string>;
}

export interface RouteMenu {
    id: string;
    i18n?: string;
    ref?: string;
    icon?: string;
    flag?: number;
    items?: Array<RouteItem>;
    roles?: Array<string>;
}

export const ROUTES: Array<RouteMenu> = [
    {
        id: "dashboard",
        ref: "dashboard",
        i18n: "dashboard.breadcrumb",
        icon: "fas fa-tachometer-alt fa-fw"
    },
    {
        id: "converter",
        ref: "converter",
        i18n: "converter.breadcrumb",
        icon: "fas fa-compress-arrows-alt fa-fw"
    },
    {
        id: "settings",
        ref: "settings",
        i18n: "settings.breadcrumb",
        icon: "fas fa-wrench fa-fw"
    },
    {
        id: "menuManagement",
        i18n: "nav.management",
        icon: "fas fa-cogs fa-fw",
        items: [
            {
                ref: "management.users",
                i18n: "nav.users",
                roles: ["administrator"]
            },
            {
                ref: "management.groups",
                i18n: "nav.groups",
                roles: ["administrator"]
            },
            {
                flag: RouteFlag.DIVIDER,
                roles: ["administrator"]
            },
            {
                ref: "management.processes",
                i18n: "nav.processes",
                roles: ["administrator"]
            }
        ]
    }
];
