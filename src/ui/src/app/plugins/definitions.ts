export interface Plugin {
    name: string;
    enabled: boolean;
}

export interface Plugins {
    plugins: Array<Plugin>;
}
