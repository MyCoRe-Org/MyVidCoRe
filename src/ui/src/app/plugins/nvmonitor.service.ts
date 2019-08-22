import { Injectable } from "@angular/core";

import { Subject, ReplaySubject } from "rxjs";

import { PluginApiService } from "./api.service";
import { WebsocketService } from "../_services/websocket.service";

import { Attrib } from "./definitions";

const PLUGIN_NAME = "Nvidia Monitor Plugin";

const WS_CONTEXT = "/nvmonitor";

export interface Attribs {
    attribs: Array<Attrib>;
}

export interface NVMonitorMessage {
    entries: Array<Attribs>;
}

@Injectable()
export class NVMonitorService {

    private enabled = new ReplaySubject<Boolean>();

    private subject: Subject<NVMonitorMessage>;

    constructor($api: PluginApiService, wsService: WebsocketService<NVMonitorMessage>) {
        $api.isPluginEnabled(PLUGIN_NAME).subscribe((enabled: boolean) => {
            if (enabled) {
                this.subject = wsService.connect(WebsocketService.buildWSURL(WS_CONTEXT));
                this.enabled.next(true);
            } else {
                this.enabled.next(false);
            }
        });
    }

    informIsEnabled() {
        return this.enabled;
    }

    getSubject() {
        return this.subject;
    }

}
