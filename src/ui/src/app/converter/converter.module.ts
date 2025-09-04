import { Injectable, NgModule } from "@angular/core";

import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { NgPipesModule } from "ngx-pipes";
import { NgSelectModule } from "@ng-select/ng-select";
import { NgOptionHighlightDirective } from "@ng-select/ng-option-highlight";
import { PipesModule } from "../_pipes/pipes.module";
import { TranslateModule } from "@ngx-translate/core";

import { UIRouterModule } from "@uirouter/angular";

import { ConverterApiService } from "./api.service";
import { ConverterJobService } from "./converterJob.service";
import { WebsocketService } from "../_services/websocket.service";

import { ConverterComponent, ConverterStates } from "./converter.component";
import { FileDropZoneDirective, FileUploadComponent } from "./fileUpload.component";

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NgbModule,
        NgPipesModule,
        NgSelectModule,
        NgOptionHighlightDirective,
        PipesModule,
        TranslateModule,
        UIRouterModule.forChild({
            states: [
                ConverterStates
            ],
        })
    ],
    declarations: [
        ConverterComponent,
        FileUploadComponent,
        FileDropZoneDirective
    ],
    exports: [
        ConverterComponent,
        FileUploadComponent,
        FileDropZoneDirective
    ],
    providers: [
        ConverterApiService,
        ConverterJobService,
        WebsocketService
    ]
})
@Injectable()
export class ConverterModule {
}
