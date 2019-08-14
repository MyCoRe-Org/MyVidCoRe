import { Injectable } from "@angular/core";

import { environment } from "../../environments/environment";

import { Subject, Observable, Observer } from "rxjs";
import { AnonymousSubject } from "rxjs/internal/Subject";

@Injectable()
export class WebsocketService {
    constructor() { }

    private url: string;

    private autoReconnect: boolean;

    private subject: Subject<MessageEvent>;

    static buildWSURL(context: string) {
        const l = !environment.production && new URL(environment.apiBaseUrl) || window.location;
        return ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname +
            (!environment.production && environment.apiBaseUrl.length === 0 ?
                ":8085" : ((l.port !== "80") && (l.port !== "443")) ? ":" + l.port : ""
            ) + "/ws" + context;
    }

    public connect(url: string, reconnect: boolean = true): Subject<MessageEvent> {
        this.url = url;
        this.autoReconnect = reconnect;

        if (!this.subject) {
            this.subject = this.create(url);
        }
        return this.subject;
    }

    public disconnect() {
        this.subject = null;
    }

    public reconnect() {
        this.disconnect();
        this.connect(this.url);
    }

    private create(url: string): Subject<MessageEvent> {
        const ws = new WebSocket(url);

        const observable = new Observable((obs: Observer<MessageEvent>) => {
            ws.onmessage = obs.next.bind(obs);
            ws.onerror = obs.error.bind(obs);
            ws.onclose = obs.complete.bind(obs);
            return ws.close.bind(ws);
        });

        const observer: Observer<Object> = {
            next: (value: Object) => {
                if (ws.readyState === WebSocket.OPEN) {
                    ws.send(JSON.stringify(value));
                }
            },
            error: (err: any) => {
                console.error(err);

                setTimeout(() => {
                    if (this.autoReconnect) {
                        console.log(`Try to reconnect to ${this.url}.`);
                        this.reconnect();
                    }
                }, 1000);
            },
            complete: () => {
                this.disconnect();
            }
        };

        return new AnonymousSubject<MessageEvent>(observer, observable);
    }
}
