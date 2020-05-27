import {
    Component, OnInit, Directive, TemplateRef, OnDestroy, AfterContentChecked,
    ContentChildren, QueryList, Output, EventEmitter
} from "@angular/core";

import { Subscription, Observable } from "rxjs";
import { throttleTime } from "rxjs/operators";

import { Attrib } from "./definitions";
import { WebsocketService } from "../_services/websocket.service";
import { NVMonitorService, NVMonitorMessage } from "./nvmonitor.service";

export interface GPUEntry {
    [name: string]: Attrib;
}

@Directive({ selector: "ng-template[nvMonContent]" })
export class NVMonitorContentDirective {

    @Output()
    entries: EventEmitter<Array<GPUEntry>> = new EventEmitter();

    constructor(public templateRef: TemplateRef<any>) { }
}

@Component({
    selector: "ui-nvmonitor-plugin",
    templateUrl: "./nvmonitor.component.html",
    providers: [WebsocketService, NVMonitorService]
})
export class NVMonitorComponent implements OnInit, OnDestroy, AfterContentChecked {

    debounceObservable: Observable<NVMonitorMessage>;

    socket: Subscription;

    entries: Array<GPUEntry> = new Array();

    contentTpl: NVMonitorContentDirective | null;

    @ContentChildren(NVMonitorContentDirective, { descendants: false })
    contentTpls: QueryList<NVMonitorContentDirective>;

    constructor(private $svc: NVMonitorService) {
    }

    ngOnInit() {
        this.$svc.informIsEnabled().subscribe(enabled => {
            if (enabled && !this.socket) {
                this.debounceObservable = this.$svc.getSubject().pipe(throttleTime(1000));
                this.socket = this.debounceObservable.subscribe((msg: NVMonitorMessage) => {
                    this.handleMessage(msg);
                });
            }
        });
    }

    ngOnDestroy() {
        if (this.socket) {
            this.socket.unsubscribe();
        }
    }

    ngAfterContentChecked() {
        this.contentTpl = this.contentTpls.first;
    }

    trackByGPU(index: number, item: GPUEntry) {
        return item["gpu"].value || index;
    }

    private findByGPUIndex(entries: Array<GPUEntry>, index: number) {
        return entries.findIndex(e => e["gpu"] && parseInt(e["gpu"].value, 10) === index);
    }

    private handleMessage(msg: NVMonitorMessage) {
        if (msg && msg.entries) {
            const entries: Array<GPUEntry> = msg.entries.map(e => {
                const ge: GPUEntry = {};
                e.attribs.forEach(a =>
                    ge[a.name] = <Attrib>{
                        value: a.value,
                        unit: a.unit
                    }
                );
                return ge;
            });

            entries.sort((a: GPUEntry, b: GPUEntry) => {
                return a["gpu"].value < b["gpu"].value ? -1 : 1;
            });

            entries.forEach(e => {
                if (e["gpu"]) {
                    const idx = parseInt(e["gpu"].value, 10);
                    const fi = this.findByGPUIndex(this.entries, idx);
                    if (fi !== -1) {
                        Object.keys(e).forEach(n => {
                            if (!this.entries[fi][n]) {
                                this.entries[fi][n] = e[n];
                            } else {
                                const val: any = e[n].value.indexOf(".") !== -1 ? parseFloat(e[n].value) : parseInt(e[n].value, 10);

                                this.entries[fi][n].value = isNaN(val) ? e[n].value : val;
                            }
                        });
                    } else {
                        this.entries.push(e);
                    }
                }
            });

            if (this.contentTpl) {
                this.contentTpl.entries.emit(this.entries);
            }
        }
    }

}
