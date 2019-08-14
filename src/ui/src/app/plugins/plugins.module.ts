import { NgModule, Injectable } from "@angular/core";

import { CommonModule } from "@angular/common";
import { GaugeModule } from "angular-gauge";
import { TranslateModule } from "@ngx-translate/core";

import { WebsocketService } from "../_services/websocket.service";

import { NVMonitorComponent } from "./nvmonitor.component";

@NgModule({
    imports: [
        CommonModule,
        GaugeModule.forRoot(),
        TranslateModule
    ],
    entryComponents: [
    ],
    declarations: [
        NVMonitorComponent
    ],
    exports: [
        NVMonitorComponent
    ],
    providers: [
        WebsocketService
    ]
})

@Injectable()
export class PluginsModule { }
