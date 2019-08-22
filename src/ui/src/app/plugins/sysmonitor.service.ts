import { Injectable } from "@angular/core";

import { Subject, ReplaySubject } from "rxjs";

import { PluginApiService } from "./api.service";
import { WebsocketService } from "../_services/websocket.service";

import { Attrib } from "./definitions";

const PLUGIN_NAME = "System Monitor Plugin";

const WS_CONTEXT = "/sysmonitor";

export interface SystemMonitorMessage {
    attribs: Array<Attrib>;
}

@Injectable()
export class SystemMonitorService {

    private enabled = new ReplaySubject<Boolean>();

    private subject: Subject<SystemMonitorMessage>;

    constructor($api: PluginApiService, wsService: WebsocketService<SystemMonitorMessage>) {
        $api.isPluginEnabled(PLUGIN_NAME).toPromise().then((enabled: boolean) => {
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
