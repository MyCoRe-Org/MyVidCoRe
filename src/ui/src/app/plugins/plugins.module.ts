import { NgModule, Injectable } from "@angular/core";

import { CommonModule } from "@angular/common";
import { GaugeModule } from "angular-gauge";
import { TranslateModule } from "@ngx-translate/core";

import { NVMonitorComponent } from "./nvmonitor.component";
import { SystemMonitorComponent } from "./sysmonitor.component";

@NgModule({
    imports: [
        CommonModule,
        GaugeModule.forRoot(),
        TranslateModule
    ],
    entryComponents: [
    ],
    declarations: [
        NVMonitorComponent,
        SystemMonitorComponent
    ],
    exports: [
        NVMonitorComponent,
        SystemMonitorComponent
    ],
    providers: [
    ]
})

@Injectable()
export class PluginsModule { }
