import { Injectable, Injector } from "@angular/core";
import { StateDeclaration, StateService, Transition } from "@uirouter/core";

import { Observable } from "rxjs";

export interface Breadcrumb {
    name: string;
    params?: Object;
    labelResolver?: Observable<string> | null;
}

@Injectable()
export class BreadcrumbService {

    private $state: StateService;

    public breadcrumbs: Array<Breadcrumb>;

    public root: Breadcrumb;

    constructor(private $injector: Injector) {
        this.breadcrumbs = new Array();
        this.root = { name: "dashboard", params: { "#": null } };
        this.breadcrumbs.push(this.root);
    }

    setBreadcrumb(transition: Transition) {
        if (!this.$state) {
            this.$state = transition.router.stateService;
        }

        const f = transition.from();
        const t = transition.to();

        const toBC = {
            name: t.name,
            params: this.decodeParams(transition.params("to")),
            labelResolver: t.data && t.data.breadcrumbLabelResolver ?
                t.data.breadcrumbLabelResolver(this.$injector, this.decodeParams(transition.params("to")))
                : null
        };
        const bIdx = this.findBreadcrumbIndex(toBC);

        if (bIdx !== -1) {
            this.setBreadcrumbAtIndex(toBC, bIdx);
        } else {
            this.setBreadcrumbByState(f, this.decodeParams(transition.params("from")));
            this.setBreadcrumbByState(t, this.decodeParams(transition.params("to")));
        }
    }

    private findBreadcrumbIndex(breadcrumb: Breadcrumb, withParams: boolean = true): number {
        return this.breadcrumbs
            .findIndex((b) => b.name === breadcrumb.name &&
                (breadcrumb.name === "login" || withParams && this.paramsMatch(b.params, breadcrumb.params) || !withParams));
    }

    private setBreadcrumbAtIndex(breadcrumb: Breadcrumb | null, index: number = 0) {
        this.breadcrumbs.splice(index, this.breadcrumbs.length);
        if (breadcrumb) {
            this.breadcrumbs.push(breadcrumb);
        }
    }

    private setBreadcrumbByState(state: StateDeclaration, params?: Object) {
        if (state.name && state.name.indexOf("**") === -1) {
            const parent: any = state.parent || (/^(.+)\.[^.]+$/.exec(state.name) || [])[1] ||
                state.data ? state.data.parentState : null;

            const sBC = {
                name: state.name,
                params: params,
                labelResolver: state.data && state.data.breadcrumbLabelResolver ?
                    state.data.breadcrumbLabelResolver(this.$injector, params) : null
            };
            const sIdx = this.findBreadcrumbIndex(sBC);

            let pIdx = -1;
            if (parent) {
                const pBC = {
                    name: parent.name || parent,
                };
                pIdx = this.findBreadcrumbIndex(pBC, false);
            }

            if (pIdx === -1 && parent) {
                this.setBreadcrumbByState(this.$state.get(parent));
            } else if (pIdx !== -1 && (sIdx === -1 || pIdx < sIdx)) {
                this.setBreadcrumbAtIndex(sBC, pIdx + 1);
                return;
            } else if (sIdx !== -1) {
                this.setBreadcrumbAtIndex(sBC, sIdx);
                return;
            }

            this.breadcrumbs.push(sBC);
        }
    }

    private paramsMatch(p1, p2) {
        if (!p1 && !p2) {
            return true;
        }

        if (!p1 || !p2) {
            return false;
        }

        return Object.keys(p1)
            .filter((k) => k !== "#" && Object.keys(p2).find((k2) => k === k2 && p1[k] === p2[k]) !== undefined)
            .length === Object.keys(p1).filter((k) => k !== "#").length;
    }

    private decodeParams(params) {
        if (params) {
            const p = {};
            Object.keys(params).forEach((k) =>
                p[k] = params[k] && params[k] !== null ?
                    typeof params[k] === "object" ? params[k] : decodeURIComponent(params[k])
                    : null
            );
            return p;
        }

        return params;
    }
}
