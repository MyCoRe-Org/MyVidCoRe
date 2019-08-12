import { TransitionService, TargetState } from "@uirouter/core";

import { AuthService } from "../_services/auth.service";

export function authHook(transitionService: TransitionService) {

    const requiresAuthCriteria = {
        to: (state) => {
            return state.data && state.data.requiresAuth === true;
        }
    };

    const requiresRoleCriteria = {
        to: (state) => {
            return state.data && state.data.requiresRole && state.data.requiresRole.length !== 0;
        }
    };

    const redirectToLogin = (transition) => {
        const $auth: AuthService = transition.injector().get(AuthService);
        const $state = transition.router.stateService;
        const targetState: TargetState = transition.targetState();
        const data = transition.$to().data;

        if (!$auth.isLoggedIn()) {
            return $state.target("login", { returnTo: targetState }, { location: false });
        } else if (data.requiresRole) {
            return $auth.isAllowed(data.requiresRole).subscribe((allowed) => {
                if (!allowed) {
                    return $state.target("login", { returnTo: targetState }, { location: false });
                } else {
                    return $auth.refreshAuth();
                }
            });
        }
    };

    transitionService.onBefore(requiresAuthCriteria, redirectToLogin, { priority: 1000000 });
    transitionService.onBefore(requiresRoleCriteria, redirectToLogin, { priority: 2000000 });
}
