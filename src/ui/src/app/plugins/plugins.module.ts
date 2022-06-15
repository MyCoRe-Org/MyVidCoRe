import { NgModule, Injectable } from "@angular/core";

import { CommonModule } from "@angular/common";
import { GaugeModule } from "angular-gauge";
import { TranslateModule } from "@ngx-translate/core";

import { PluginApiService } from "./api.service";

import { GaugeComponent } from "./gauge.component";
import { NVMonitorComponent, NVMonitorContentDirective } from "./nvmonitor.component";
import { SystemMonitorComponent, SystemMonitorContentDirective } from "./sysmonitor.component";

@NgModule({
    imports: [
        CommonModule,
        GaugeModule.forRoot(),
        TranslateModule
    ],
    declarations: [
        GaugeComponent,
        NVMonitorComponent,
        NVMonitorContentDirective,
        SystemMonitorComponent,
        SystemMonitorContentDirective
    ],
    exports: [
        GaugeComponent,
        NVMonitorComponent,
        NVMonitorContentDirective,
        SystemMonitorComponent,
        SystemMonitorContentDirective
    ],
    providers: [
        PluginApiService
    ]
})

@Injectable()
export class PluginsModule { }
