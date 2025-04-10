import { Component, OnInit } from "@angular/core";

import { AuthService } from "../_services/auth.service";
import { ConverterApiService } from "../converter/api.service";
import { PluginApiService } from "../plugins/api.service";

import { MonitorAttribs } from "../plugins/sysmonitor.component";
import { HWAccel } from "../converter/definitions";
import { GPUEntry } from "../plugins/nvmonitor.component";

@Component({
    selector: "ui-dashboard",
    templateUrl: "./dashboard.component.html",
    providers: [ConverterApiService],
    standalone: false
})
export class DashboardComponent implements OnInit {

    static maxValues = {};

    hwaccels: Array<HWAccel>;

    sysmon: MonitorAttribs;

    nvmon: Array<GPUEntry>;

    constructor(public $auth: AuthService, public $api: PluginApiService, public $capi: ConverterApiService) { }

    ngOnInit() {
        this.$capi.getHWAccels().subscribe(res => this.hwaccels = res);
    }

    trackByGPU(index: number, item: Attr) {
        return item["gpu"].value || index;
    }

    onSystemMonitorEntries(entries: MonitorAttribs) {
        this.sysmon = entries;
    }

    onNVMonitorEntries(entries: Array<GPUEntry>) {
        this.nvmon = entries;
    }

    isEmpty(value: any) {
        return value instanceof Array ?
            (<Array<any>>value).length === 0 : typeof value === "object" ?
                Object.values(value).length === 0 : !value;
    }

    maxValue(name: string, value: any, defaultMax: number = 100) {
        const val: number = typeof value === "string" ?
            value.indexOf(".") !== -1 ? parseFloat(value) : parseInt(value, 10) : value;

        DashboardComponent.maxValues[name] = Math.max(isNaN(val) ? val : 0, DashboardComponent.maxValues[name] || defaultMax);
        return DashboardComponent.maxValues[name];
    }

}

export const DashboardStates = {
    name: "dashboard",
    url: "/",
    component: DashboardComponent,
    data: {
        breadcrumb: "dashboard.breadcrumb",
        requiresAuth: false
    }
};
