import { Injectable } from "@angular/core";

import { Subject } from "rxjs";

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

    constructor(wsService: WebsocketService<SystemMonitorMessage>) {
        this.events = wsService.connect(WebsocketService.buildWSURL(WS_CONTEXT));
    }
}
