import { NgModule, Injectable, Injector } from "@angular/core";
import { CommonModule } from "@angular/common";
import { TranslateModule } from "@ngx-translate/core";
import { UIRouter, UIRouterModule } from "@uirouter/angular";

import { breadcrumbHook } from "./breadcrumb.hook";
import { BreadcrumbService } from "./breadcrumb.service";
import { BreadcrumbComponent } from "./breadcrumb.component";

export function breadcrumbConfigFn(router: UIRouter, injector: Injector) {
    const transitionService = router.transitionService;
    const breadcrumbService = injector.get(BreadcrumbService);

    breadcrumbHook(injector, transitionService, breadcrumbService);
}

@NgModule({
    imports: [
        CommonModule,
        TranslateModule,
        UIRouterModule.forChild({
            config: breadcrumbConfigFn,
        })
    ],
    declarations: [
        BreadcrumbComponent
    ],
    exports: [
        BreadcrumbComponent
    ],
    providers: [
        BreadcrumbService
    ]
})
@Injectable()
export class BreadcrumbModule { }
