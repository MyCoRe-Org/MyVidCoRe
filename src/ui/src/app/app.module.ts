import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientModule, HttpClient, HTTP_INTERCEPTORS } from "@angular/common/http";
import { NgModule, Injectable } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { TranslateCompiler, TranslateModule, TranslateLoader } from "@ngx-translate/core";
import { TranslateHttpLoader } from "@ngx-translate/http-loader";
import { MESSAGE_FORMAT_CONFIG, TranslateMessageFormatCompiler } from "ngx-translate-messageformat-compiler";

import { NgPipesModule } from "ngx-pipes";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { NgSelectModule } from "@ng-select/ng-select";
import { NgOptionHighlightModule } from "@ng-select/ng-option-highlight";
import { ToastrModule } from "ngx-toastr";

import { UIRouterModule } from "@uirouter/angular";

import { routerConfigFn } from "./_helpers/router.config";

import { ApiService } from "./_services/api.service";
import { AuthService } from "./_services/auth.service";
import { AuthHttpInterceptor } from "./_services/http.service";
import { CacheService } from "./_services/cache.service";
import { ErrorService } from "./_services/error.service";

import { BreadcrumbModule } from "./breadcrumb/breadcrumb.module";
import { SpinnerModule } from "./spinner/spinner.module";
import { PipesModule } from "./_pipes/pipes.module";
import { PluginsModule } from "./plugins/plugins.module";

import { AppComponent } from "./app.component";

import { DashboardComponent, DashboardStates } from "./dashboard/dashboard.component";
import { LoginComponent, LoginStates } from "./login/login.component";

import { ConfirmComponent } from "./modals/confirm.component";

export function createTranslateLoader(http: HttpClient) {
    return new TranslateHttpLoader(http, "./assets/i18n/", ".json");
}

// Lazy Loading States
export const ConverterFutureState = {
    name: "converter.**",
    url: "/converter",
    loadChildren: () => import("./converter/converter.module").then(m => m.ConverterModule)
};

export const SettingsFutureState = {
    name: "settings.**",
    url: "/settings",
    loadChildren: () => import("./settings/settings.module").then(m => m.SettingsModule)
};

// @FIXME workaround for ivy build
@Injectable({ providedIn: "root" })
export class InjectableTranslateMessageFormatCompiler extends TranslateMessageFormatCompiler { }

@NgModule({
    declarations: [
        AppComponent,
        LoginComponent,
        DashboardComponent,
        ConfirmComponent
    ],
    imports: [
        BreadcrumbModule,
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        FormsModule,
        ReactiveFormsModule,
        SpinnerModule,
        NgPipesModule,
        NgSelectModule,
        NgOptionHighlightModule,
        PipesModule,
        NgbModule,
        PluginsModule,
        ToastrModule.forRoot({
            autoDismiss: true,
            timeOut: 10000,
            positionClass: "toast-bottom-right",
            preventDuplicates: true,
            maxOpened: 3
        }),
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: (createTranslateLoader),
                deps: [HttpClient]
            },
            compiler: {
                provide: TranslateCompiler,
                // @FIXME workaround for ivy build
                // useClass: TranslateMessageFormatCompiler
                useClass: InjectableTranslateMessageFormatCompiler
            }
        }),
        UIRouterModule.forRoot({
            states: [
                LoginStates,
                DashboardStates,
                ConverterFutureState,
                SettingsFutureState
            ],
            useHash: true,
            config: routerConfigFn,
            otherwise: "/"
        })
    ],
    providers: [
        { provide: HTTP_INTERCEPTORS, useClass: AuthHttpInterceptor, multi: true },
        {
            provide: MESSAGE_FORMAT_CONFIG, useValue: {
                biDiSupport: false,
                intlSupport: false,
                strictNumberSign: false
            }
        },
        AuthService,
        ApiService,
        CacheService,
        ErrorService
    ],
    bootstrap: [AppComponent]
})
export class AppModule { }
