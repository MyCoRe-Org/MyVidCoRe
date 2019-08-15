import { Injectable } from "@angular/core";

import { Subject } from "rxjs";
import { map, retry } from "rxjs/operators";

import { WebsocketService } from "../_services/websocket.service";

const WS_CONTEXT = "/nvmonitor";

export interface Attrib {
    name: string;
    unit: string;
    value: string;
}

export interface Attribs {
    attribs: Array<Attrib>;
}

export interface NVMonitorMessage {
    entries: Array<Attribs>;
}

@Injectable()
export class NVMonitorService {

    public events: Subject<NVMonitorMessage>;

    constructor(wsService: WebsocketService<NVMonitorMessage>) {
        this.events = wsService.connect(WebsocketService.buildWSURL(WS_CONTEXT));
    }
}
