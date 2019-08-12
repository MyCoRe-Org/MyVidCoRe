import { Injector } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { TransitionService, Transition } from "@uirouter/core";
import { TranslateService } from "@ngx-translate/core";

import { BreadcrumbService } from "./breadcrumb.service";

export function breadcrumbHook(injector: Injector, transitionService: TransitionService, breadcrumbService: BreadcrumbService) {

    const TITLE_SUFFIX = " - MyVidCoRe";

    const allStateCriteria = {
        to: (state) => {
            return true;
        }
    };

    const beforeTransisition = (transition: Transition) => {
        breadcrumbService.setBreadcrumb(transition);
    };

    const finishTransisition = (transition: Transition) => {
        const translate = injector.get<TranslateService>(TranslateService);
        const title = injector.get<Title>(Title);

        const to = transition.to();
        const breadcrumb = breadcrumbService.breadcrumbs[breadcrumbService.breadcrumbs.length - 1];
        const labelResolver = breadcrumb && breadcrumb.labelResolver;

        if (labelResolver) {
            labelResolver.subscribe(t => title.setTitle(t + TITLE_SUFFIX));
        } else {
            title.setTitle(translate.instant(to.data.breadcrumb || breadcrumb.name || to.name, breadcrumb.params) + TITLE_SUFFIX);
        }
    };

    transitionService.onBefore(allStateCriteria, beforeTransisition, { priority: 10 });
    transitionService.onFinish(allStateCriteria, finishTransisition, { priority: 10 });
}
