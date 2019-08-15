import { Component, OnInit } from "@angular/core";

import { Subscription } from "rxjs";

import { WebsocketService } from "../_services/websocket.service";

import { NVMonitorService, NVMonitorMessage } from "./nvmonitor.service";

interface GPUAttrib {
    value: string;
    unit: string;
}

interface GPUEntry {
    [name: string]: GPUAttrib;
}

@Component({
    selector: "ui-nvmonitor-plugin",
    templateUrl: "./nvmonitor.component.html",
    providers: [WebsocketService, NVMonitorService]
})
export class NVMonitorComponent implements OnInit {

    socket: Subscription;

    entries: Array<GPUEntry> = new Array();

    constructor(private $svc: NVMonitorService) {
    }

    ngOnInit() {
        this.socket = this.$svc.events.subscribe((msg: NVMonitorMessage) => {
            this.handleMessage(msg);
        });
    }

    trackByGPU(index: number, item: GPUEntry) {
        return item["gpu"].value || index;
    }

    private findByGPUIndex(entries: Array<GPUEntry>, index: number) {
        return entries.findIndex(e => e["gpu"] && parseInt(e["gpu"].value, 10) === index);
    }

    private handleMessage(msg: NVMonitorMessage) {
        const entries: Array<GPUEntry> = msg.entries.map(e => {
            const ge: GPUEntry = {};
            e.attribs.forEach(a =>
                ge[a.name] = <GPUAttrib>{
                    value: a.value,
                    unit: a.unit
                }
            );
            return ge;
        });

        entries.sort((a: GPUEntry, b: GPUEntry) => {
            return a["gpu"].value < b["gpu"].value ? -1 : 1;
        });

        entries.forEach((e, i) => {
            if (this.entries[i]) {
                Object.keys(e).forEach(n => {
                    if (!this.entries[i][n]) {
                        this.entries[i][n] = e[n];
                    } else {
                        let val: any = e[n].value.indexOf(".") !== -1 ? parseFloat(e[n].value) : parseInt(e[n].value, 10);
                        val = isNaN(val) ? e[n].value : val;

                        this.entries[i][n].value = val === 0 ?  this.entries[i][n].value : val;
                    }
                });
            } else {
                const idx = e["gpu"] && parseInt(e["gpu"].value, 10);
                if (this.findByGPUIndex(this.entries, idx) === -1) {
                    this.entries.push(e);
                }
            }
        });
    }

    gaugeLabel(attrib: GPUAttrib) {
        return attrib && ["C", "F"].indexOf(attrib.unit) !== -1 ?
            (value: number): string => {
                return `${Math.round(value)} °` + attrib.unit;
            }
            :
            (value: number): string => {
                return `${Math.round(value)} ` + attrib.unit;
            };
    }

}
