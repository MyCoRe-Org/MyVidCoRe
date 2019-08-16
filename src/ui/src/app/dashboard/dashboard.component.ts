import { Component, OnInit } from "@angular/core";

import { Observable } from "rxjs";

import { AuthService } from "../_services/auth.service";
import { PluginApiService } from "../plugins/api.service";

import { SystemMonitorComponent } from "../plugins/sysmonitor.component";
import { NVMonitorComponent } from "../plugins/nvmonitor.component";

@Component({
    selector: "ui-dashboard",
    templateUrl: "./dashboard.component.html"
})
export class DashboardComponent implements OnInit {

    sysmon: Observable<Boolean>;

    nvmon: Observable<Boolean>;

    constructor(public $auth: AuthService, public $api: PluginApiService) { }

    ngOnInit() {
        this.sysmon = this.$api.isPluginEnabled(SystemMonitorComponent.PLUGIN_NAME);
        this.nvmon = this.$api.isPluginEnabled(NVMonitorComponent.PLUGIN_NAME);
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
