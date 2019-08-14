import { Injectable } from "@angular/core";

import { Subject } from "rxjs";
import { map, retry } from "rxjs/operators";

import { WebsocketService } from "../_services/websocket.service";

const WS_CONTEXT = "/sysmonitor";

export interface Attrib {
    name: string;
    unit: string;
    value: string;
}

export interface SystemMonitorMessage {
    attribs: Array<Attrib>;
}

@Injectable()
export class SystemMonitorService {

    public events: Subject<SystemMonitorMessage>;

    constructor(wsService: WebsocketService) {
        this.events = <Subject<SystemMonitorMessage>>wsService.connect(WebsocketService.buildWSURL(WS_CONTEXT)).pipe(
            map((response: MessageEvent): SystemMonitorMessage => {
                const data = JSON.parse(response.data);
                return data;
            }),
            retry(3)
        );
    }
}
