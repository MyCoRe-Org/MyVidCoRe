import { Component, OnInit } from "@angular/core";

import { Subscription } from "rxjs";

import { WebsocketService } from "../_services/websocket.service";

import { SystemMonitorService, SystemMonitorMessage } from "./sysmonitor.service";

interface MonitorAttrib {
    value: string;
    unit: string;
}

interface MonitorAttribs {
    [name: string]: MonitorAttrib;
}

@Component({
    selector: "ui-sysmonitor-plugin",
    templateUrl: "./sysmonitor.component.html",
    providers: [WebsocketService, SystemMonitorService]
})
export class SystemMonitorComponent implements OnInit {

    socket: Subscription;

    entries: MonitorAttribs = {};

    constructor(private $svc: SystemMonitorService) {
    }

    ngOnInit() {
        this.socket = this.$svc.events.subscribe((msg: SystemMonitorMessage) => {
            this.handleMessage(msg);
        });
    }

    private handleMessage(msg: SystemMonitorMessage) {
        msg.attribs.forEach(a =>
            this.entries[a.name] = <MonitorAttrib>{
                value: a.value,
                unit: a.unit
            }
        );
    }

    gaugeLabel(name: string, attrib: MonitorAttrib) {
        if (attrib) {
            if (["C", "F"].indexOf(attrib.unit) !== -1) {
                return (value: number): string => {
                    return `${Math.round(value)} °` + attrib.unit;
                };
            } else if (["memory", "swap"].indexOf(name) !== -1) {
                return (value: number): string => {
                    if (value >= (1024 * 1024 * 1024)) {
                        return `${Math.round(value / (1024 * 1024 * 1024))} G`;
                    } else if (value >= (1024 * 1024)) {
                        return `${Math.round(value / (1024 * 1024))} M`;
                    }

                    return `${Math.round(value / 1024)} K`;
                };
            } else {
                return (value: number): string => {
                    return `${Math.round(value)} ` + attrib.unit;
                };
            }
        }

        return null;
    }

}
