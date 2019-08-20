import { Component, OnInit } from "@angular/core";

import { Observable } from "rxjs";

import { AuthService } from "../_services/auth.service";
import { ConverterApiService } from "../converter/api.service";
import { PluginApiService } from "../plugins/api.service";

import { SystemMonitorComponent } from "../plugins/sysmonitor.component";
import { NVMonitorComponent } from "../plugins/nvmonitor.component";
import { HWAccel } from "../converter/definitions";

@Component({
    selector: "ui-dashboard",
    templateUrl: "./dashboard.component.html",
    providers: [ConverterApiService]
})
export class DashboardComponent implements OnInit {

    static maxValues = {};

    hwaccels: Array<HWAccel>;

    sysmon: Observable<Boolean>;

    nvmon: Observable<Boolean>;

    constructor(public $auth: AuthService, public $api: PluginApiService, public $capi: ConverterApiService) { }

    ngOnInit() {
        this.sysmon = this.$api.isPluginEnabled(SystemMonitorComponent.PLUGIN_NAME);
        this.nvmon = this.$api.isPluginEnabled(NVMonitorComponent.PLUGIN_NAME);

        this.$capi.getHWAccels().subscribe(res => this.hwaccels = res);
    }

    trackByGPU(index: number, item: Attr) {
        return item["gpu"].value || index;
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
