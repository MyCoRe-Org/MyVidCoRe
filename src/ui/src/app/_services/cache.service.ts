import { EventEmitter, Injectable } from "@angular/core";

export interface Cache {
    obj: any;
    validUntil: number |null;
    removeEvent?: EventEmitter<Cache>;
}

@Injectable()
export class CacheService {

    private static caches: Map<string, Cache> = new Map();

    private static interval: any;

    public static DEFAULT_LIFETIME = 1000 * 60 * 5;

    public static set(key: string, obj: any, livetime?: number, removeEvent?: EventEmitter<Cache>) {
        const cobj: Cache = {
            obj: obj,
            validUntil: livetime ? Date.now() + livetime : null,
            removeEvent: removeEvent
        };
        this.caches.set(key, cobj);
        this.periodicalCleanup();
    }

    public static get(key: string): any {
        if (this.caches.has(key)) {
            const cobj: Cache = this.caches.get(key);
            if (cobj.validUntil && cobj.validUntil < Date.now()) {
                this.delete(key);
                return null;
            }
            return cobj.obj;
        }
        return null;
    }

    public static delete(key: string) {
        const cache = this.caches.get(key);
        if (cache.removeEvent) {
            cache.removeEvent.emit(cache);
        }
        this.caches.delete(key);
    }

    public static getAllByPrefix(prefix: string): Map<string, Cache> {
        const res = new Map();
        this.caches.forEach((v, k) => {
            if (k.startsWith(prefix)) {
                res.set(k, v);
            }
        });
        return res;
    }

    public static buildCacheKey(prefix: string, obj: any) {
        const reRE = new RegExp("([\\[\\]\\(\\)\/\\:\\|])", "g");
        const key = [prefix];

        if (typeof obj === "string") {
            key.push(decodeURIComponent(obj.trim()).replace(reRE, ""));
        } else {
            Object.keys(obj).forEach((k) =>
                obj[k] && key.push(decodeURIComponent(obj[k]).replace(reRE, ""))
            );
        }

        return key.join("");
    }

    private static periodicalCleanup() {
        if (!this.interval) {
            this.interval = setInterval(() => this.periodicalCleanup(), 60000);
        }

        this.caches.forEach((cobj, key) => {
            if (cobj.validUntil && cobj.validUntil < Date.now()) {
                this.delete(key);
            }
        });
    }

}
