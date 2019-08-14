import { Component, OnInit } from "@angular/core";

import { Subscription } from "rxjs";

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
    providers: [NVMonitorService]
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
                this.entries[i] = e;
            } else {
                this.entries.push(e);
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
