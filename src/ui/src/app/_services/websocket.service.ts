import { Injectable } from "@angular/core";

import { Subject, Observable, Observer } from "rxjs";
import { AnonymousSubject } from "rxjs/internal/Subject";

@Injectable()
export class WebsocketService {
    constructor() { }

    private subject: Subject<MessageEvent>;

    static buildWSURL(context: string) {
        const l = window.location;
        return ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname +
            (((l.port !== "80") && (l.port !== "443")) ? ":" + l.port : "") + "/ws" + context;
    }

    public connect(url: string): Subject<MessageEvent> {
        if (!this.subject) {
            this.subject = this.create(url);
        }
        return this.subject;
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
            },
            complete: () => {
                console.log("closed");
            }
        };

        return new AnonymousSubject<MessageEvent>(observer, observable);
    }
}
