export interface Plugin {
    name: string;
    enabled: boolean;
}

export interface Plugins {
    plugins: Array<Plugin>;
}

export interface Attrib {
    name?: string;
    value: string;
    unit: string;
}
