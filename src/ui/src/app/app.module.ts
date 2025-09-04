import { BrowserModule } from "@angular/platform-browser";
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { provideTranslateCompiler, provideTranslateService, TranslateModule } from "@ngx-translate/core";
import { provideTranslateHttpLoader } from "@ngx-translate/http-loader";
import { MESSAGE_FORMAT_CONFIG, TranslateMessageFormatCompiler } from "ngx-translate-messageformat-compiler";

import { NgPipesModule } from "ngx-pipes";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { NgSelectModule } from "@ng-select/ng-select";
import { NgOptionHighlightDirective } from "@ng-select/ng-option-highlight";
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
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";

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

@NgModule({
    declarations: [
        AppComponent,
        LoginComponent,
        DashboardComponent,
        ConfirmComponent
    ],
    bootstrap: [AppComponent], imports: [BreadcrumbModule,
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        SpinnerModule,
        NgPipesModule,
        NgSelectModule,
        NgOptionHighlightDirective,
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
        TranslateModule.forRoot(),
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
        })], providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideTranslateService({
            compiler: provideTranslateCompiler(TranslateMessageFormatCompiler),
            loader: provideTranslateHttpLoader({prefix: "./assets/i18n/", suffix: ".json"}),
        }),
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthHttpInterceptor,
            multi: true
        },
        {
            provide: MESSAGE_FORMAT_CONFIG,
            useValue: {
                biDiSupport: false,
                intlSupport: false,
                strictNumberSign: false
            }
        },
        AuthService,
        ApiService,
        CacheService,
        ErrorService
    ]
})
export class AppModule {
}
