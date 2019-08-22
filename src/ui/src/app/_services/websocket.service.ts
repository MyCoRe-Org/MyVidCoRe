import { Injectable } from "@angular/core";

import { environment } from "../../environments/environment";

import { Subject, timer, throwError, Observable } from "rxjs";
import { webSocket } from "rxjs/webSocket";
import { retryWhen, mergeMap } from "rxjs/operators";

@Injectable()
export class WebsocketService<T> {

    url: string;

    constructor() { }

    private subject: Subject<T>;

    static buildWSURL(context: string) {
        const l = !environment.production && environment.apiBaseUrl.length !== 0 && new URL(environment.apiBaseUrl) || window.location;
        return ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname +
            (!environment.production && environment.apiBaseUrl.length === 0 ?
                ":8085" : (l.port && (l.port !== "80") && (l.port !== "443")) ? ":" + l.port : ""
            ) + "/ws" + context;
    }

    public connect(url: string, autoReconnect: boolean = true, scalingDuration: number = 1000): Subject<T> {
        this.url = url;

        if (!this.subject) {
            this.subject = <Subject<T>>webSocket(url).pipe(
                retryWhen(this.reconnectStrategy(autoReconnect, scalingDuration)),
            );
        }
        return this.subject;
    }

    private reconnectStrategy(autoReconnect: boolean = true, scalingDuration: number = 1000) {
        return (attempts: Observable<any>) => {
            return attempts.pipe(
                mergeMap((error, i) => {
                    if (autoReconnect !== true) {
                        return throwError(error);
                    }

                    const retryAttempt = i + 1;
                    console.warn(`Retry connect to ${this.url} in ${retryAttempt * scalingDuration}ms`);
                    return timer(retryAttempt * scalingDuration);
                })
            );
        };
    }

}
