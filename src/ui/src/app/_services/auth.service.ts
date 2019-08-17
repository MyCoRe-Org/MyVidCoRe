import { Injectable } from "@angular/core";
import { StateService } from "@uirouter/core";

import { from as observableFrom, of as observableOf, Observable } from "rxjs";
import { map } from "rxjs/operators";

import { ApiService } from "./api.service";

export interface Principal {
    buildIn?: boolean;
    readonly?: boolean;
    name?: string;
    uid?: string;
    description?: string;
    email?: string;
    lastLogin?: Date;
    attributes?: any;
}

export interface Session {
    id?: string;
    validFrom?: Date;
    validTo?: Date;
}

export interface RoleAllowed {
    name: string;
    allowed?: boolean;
}

@Injectable()
export class AuthService {

    private cachedRoles: Array<RoleAllowed> = new Array();

    public formAuth = true;
    public token: string;
    public user: Principal = {};
    public session: Session = {};

    constructor(private $api: ApiService, private $state: StateService) {
        this.refreshAuth();
        this.getAppSettings();
    }

    isLoggedIn() {
        return this.user !== null && this.token !== null;
    }

    private cachedRole(role: string) {
        return this.cachedRoles.filter((r) => r.name === role)[0] || null;
    }

    isAllowed(roles: Array<string>): Observable<boolean> {
        if (this.isLoggedIn()) {
            roles = roles || ["*"];

            const cached = this.cachedRoles.filter((r) => roles.indexOf(r.name) !== -1);
            if (cached.length > 0 && roles.length === cached.length) {
                return observableOf(cached.filter((r) => r.allowed).length > 0);
            } else {
                const promises: Array<Promise<any>> = new Array();
                for (const role of roles) {
                    const isCached = this.cachedRoles.filter((r) => role === r.name).length > 0;
                    if (!isCached) {
                        this.cachedRoles.push({ name: role });
                        promises.push(new Promise((r) => {
                            this.$api.isAllowed([role]).toPromise().then((allowed: boolean) => {
                                this.cachedRole(role).allowed = allowed;
                                r(allowed);
                            }).catch((error) => {
                                if (error.status === 403) {
                                    this.cachedRole(role).allowed = false;
                                }
                                r(false);
                            });
                        }));
                    }
                }
                return observableFrom(Promise.all(promises)).pipe(map((result) => {
                    return result.filter((b) => b).length > 0;
                }));
            }
        } else {
            return observableOf(false);
        }
    }

    getAttributeValue(key: string) {
        if (this.user && this.user.attributes) {
            const attr = this.user.attributes.attribute.find((a) => a.name === key);
            return attr ? attr.value.length === 1 ? attr.value[0] : attr.value : null;
        }

        return null;
    }

    login(username: string, password: string) {
        return this.$api.login(username, password).toPromise()
            .then((session: any) => {
                this.session = session;
                this.token = session.id;
                this.cachedRoles = new Array();
                window.localStorage.setItem("accessToken", session.id);
                return this.refreshAuth();
            });
    }

    logout() {
        this.$api.logout().toPromise().then((res) => {
            this.token = null;
            this.user = null;
            this.session = null;
            this.cachedRoles = new Array();
            window.localStorage.removeItem("accessToken");
            window.localStorage.removeItem("principal");
            this.$state.reload();
        });
    }

    refreshAuth() {
        this.token = window.localStorage.getItem("accessToken");
        return this.$api.getAuthorized().toPromise().then((principal) => {
            this.user = principal;

            if (this.user.attributes) {
                this.user.readonly = true;
            }

            window.localStorage.setItem("principal", JSON.stringify(principal));
        }).catch((err) => {
            this.token = null;
            this.user = null;
            this.session = null;
            this.cachedRoles = new Array();
            window.localStorage.removeItem("accessToken");
            window.localStorage.removeItem("principal");
        });
    }

    getAppSettings() {
        this.formAuth = true;
    }

}
