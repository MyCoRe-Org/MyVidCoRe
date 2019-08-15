import { Injectable } from "@angular/core";

import { environment } from "../../environments/environment";

import { Subject, timer, throwError, Observable } from "rxjs";
import { webSocket } from "rxjs/webSocket";
import { retryWhen, mergeMap, finalize } from "rxjs/operators";

@Injectable()
export class WebsocketService<T> {

    constructor() { }

    private subject: Subject<T>;

    static buildWSURL(context: string) {
        const l = !environment.production && environment.apiBaseUrl.length !== 0 && new URL(environment.apiBaseUrl) || window.location;
        return ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname +
            (!environment.production && environment.apiBaseUrl.length === 0 ?
                ":8085" : ((l.port !== "80") && (l.port !== "443")) ? ":" + l.port : ""
            ) + "/ws" + context;
    }

    public connect(url: string, autoReconnect: boolean = true, excludedStatusCodes = [], scalingDuration: number = 1000): Subject<T> {
        if (!this.subject) {
            this.subject = <Subject<T>>webSocket(url).pipe(
                retryWhen((attempts: Observable<any>) => {
                    return attempts.pipe(
                        mergeMap((error, i) => {
                            const retryAttempt = i + 1;

                            if (autoReconnect === true && excludedStatusCodes.find(e => e === error.status)) {
                                console.warn(`Retry to connect to ${url} in ${retryAttempt * scalingDuration}ms`);
                                return timer(retryAttempt * scalingDuration);
                            } else {
                                return throwError(error);
                            }
                        })
                    );
                })
            );
        }
        return this.subject;
    }

}
