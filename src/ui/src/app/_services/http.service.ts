import { Injectable, Injector } from "@angular/core";
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest } from "@angular/common/http";

import { UIRouterGlobals } from "@uirouter/core";

import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";

import { AuthService } from "./auth.service";

@Injectable()
export class AuthHttpInterceptor implements HttpInterceptor {
    $state: UIRouterGlobals;
    $auth: AuthService;

    constructor(private $injector: Injector) {
        setTimeout(() => {
            this.$state = this.$injector.get<UIRouterGlobals>(UIRouterGlobals);
            this.$auth = this.$injector.get<AuthService>(AuthService);
        });
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const authReq = req.clone({
            headers: req.headers.set("AccessToken", `${window.localStorage.getItem("accessToken")}`)
        });

        return next.handle(authReq).pipe(catchError((error, _caught) => {
            if (error.status === 401 && (!this.$state || this.$state && this.$state.current.name !== "login")) {
                this.$auth.logout();
            }

            return throwError(error);
        })) as any;
    }
}
