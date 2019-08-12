import { UIRouter } from "@uirouter/core";

import { authHook } from "./auth.hook";
import { routerHook } from "./router.hook";

export function routerConfigFn(router: UIRouter) {
    const transitionService = router.transitionService;

    authHook(transitionService);
    routerHook(transitionService);
}
