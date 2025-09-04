import { NgModule, Injectable } from "@angular/core";

import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { NgPipesModule } from "ngx-pipes";
import { NgSelectModule } from "@ng-select/ng-select";
import { NgOptionHighlightDirective } from "@ng-select/ng-option-highlight";
import { PipesModule } from "../_pipes/pipes.module";
import { TranslateModule } from "@ngx-translate/core";

import { UIRouterModule } from "@uirouter/angular";

import { ConverterApiService } from "../converter/api.service";
import { SettingsApiService } from "./api.service";

import { SettingsComponent, SettingsStates } from "./settings.component";
import { SettingsExtendedComponent } from "./settings.extended.component";
import { SettingsAudioComponent } from "./settings.audio.component";
import { SettingsVideoComponent } from "./settings.video.component";

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
                SettingsStates
            ],
        })
    ],
    declarations: [
        SettingsComponent,
        SettingsExtendedComponent,
        SettingsAudioComponent,
        SettingsVideoComponent
    ],
    exports: [
        SettingsComponent,
        SettingsExtendedComponent,
        SettingsAudioComponent,
        SettingsVideoComponent
    ],
    providers: [
        ConverterApiService,
        SettingsApiService
    ]
})
@Injectable()
export class SettingsModule { }
