import { TransitionService, Transition } from "@uirouter/core";
import { CacheService } from "../_services/cache.service";

export function routerHook(transitionService: TransitionService) {

    const injectReturnToCriteria = {
        to: (state) => {
            return state.name === "login" && state.params.returnTo && state.params.returnTo.value() == null;
        }
    };

    const injectReturnTo = (transition: Transition) => {
        transition.to().params.returnTo = transition.router
            .stateService.target(transition.from().name, transition.params("from"));
        return transition.to();
    };

    const allStateCriteria = {
        to: (state) => {
            return true;
        }
    };

    const closeSidebar = () => {
        const sidebar = window.document.querySelector("#sidebar");
        const content = window.document.querySelector("#content");
        if (sidebar && content) {
            sidebar.classList.remove("active");
            content.classList.remove("active");
        }
    };

    const saveScrollPosition = (transition: Transition) => {
        if (transition.from().name) {
            if ("sref|unknown".indexOf(transition.options().source) !== -1) {
                CacheService.set(
                    CacheService.buildCacheKey("sp" + transition.from().name, transition.params("from")),
                    { x: window.scrollX, y: window.scrollY }, CacheService.DEFAULT_LIFETIME
                );
            }
        }
    };

    const scrollTo = (transition: Transition) => {
        let sp = CacheService.get(CacheService.buildCacheKey("sp" + transition.to().name, transition.params("to")))
            || { x: 0, y: 0 };
        if ("sref|unknown".indexOf(transition.options().source) !== -1) {
            sp = { x: 0, y: 0 };
        }

        setTimeout(() => window.scroll(sp.x, sp.y));
    };

    transitionService.onCreate(injectReturnToCriteria, injectReturnTo, { priority: 10 });

    transitionService.onStart(allStateCriteria, closeSidebar, { priority: 10 });
    transitionService.onStart(allStateCriteria, saveScrollPosition, { priority: 20 });
    transitionService.onFinish(allStateCriteria, scrollTo, { priority: 10 });
}
