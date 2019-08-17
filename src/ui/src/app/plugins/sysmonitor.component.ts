import { Component, OnInit, Directive, TemplateRef, ContentChildren, QueryList, AfterContentChecked, OnDestroy } from "@angular/core";

import { Subscription } from "rxjs";

import { WebsocketService } from "../_services/websocket.service";
import { PluginApiService } from "./api.service";

import { Attrib } from "./definitions";
import { SystemMonitorService, SystemMonitorMessage } from "./sysmonitor.service";

interface MonitorAttribs {
    [name: string]: Attrib;
}

@Directive({ selector: "ng-template[sysMonContent]" })
export class SystemMonitorContentDirective {
    constructor(public templateRef: TemplateRef<any>) { }
}

@Component({
    selector: "ui-sysmonitor-plugin",
    templateUrl: "./sysmonitor.component.html",
    providers: [WebsocketService, SystemMonitorService]
})
export class SystemMonitorComponent implements OnInit, OnDestroy, AfterContentChecked {

    static PLUGIN_NAME = "System Monitor Plugin";

    socket: Subscription;

    entries: MonitorAttribs = {};

    contentTpl: SystemMonitorContentDirective | null;

    @ContentChildren(SystemMonitorContentDirective, { descendants: false })
    contentTpls: QueryList<SystemMonitorContentDirective>;

    constructor(private $api: PluginApiService, private $svc: SystemMonitorService) {
    }

    ngOnInit() {
        this.$api.isPluginEnabled(SystemMonitorComponent.PLUGIN_NAME).subscribe((enabled: boolean) => {
            if (enabled) {
                this.socket = this.$svc.events.subscribe((msg: SystemMonitorMessage) => {
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

    private handleMessage(msg: SystemMonitorMessage) {
        msg.attribs.forEach(a => {
            if (!this.entries[a.name]) {
                this.entries[a.name] = <Attrib>{
                    value: a.value,
                    unit: a.unit
                };
            } else {
                let val: any = a.value.indexOf(".") !== -1 ? parseFloat(a.value) : parseInt(a.value, 10);
                val = isNaN(val) ? a.value : val;

                this.entries[a.name].value = ["C", "F"].indexOf(a.unit) !== -1 && val === 0 ? this.entries[a.name].value : val;
            }
        });
    }

}
