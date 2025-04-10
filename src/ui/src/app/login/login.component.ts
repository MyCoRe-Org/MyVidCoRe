import { Component, OnInit } from "@angular/core";
import { ToastrService } from "ngx-toastr";
import { TranslateService } from "@ngx-translate/core";
import { TargetState, StateService, UIRouterGlobals } from "@uirouter/core";
import { UntypedFormBuilder, UntypedFormGroup, Validators } from "@angular/forms";

import { AuthService } from "../_services/auth.service";
import { SpinnerService } from "../spinner/spinner.service";

@Component({
    selector: "ui-login",
    templateUrl: "./login.component.html",
    standalone: false
})
class LoginComponent implements OnInit {
    form: UntypedFormGroup;
    returnTo: TargetState;
    invalidCredentials = false;

    constructor(public $fb: UntypedFormBuilder, private $state: StateService, private $spinner: SpinnerService,
        private $auth: AuthService, private toastr: ToastrService, private translate: TranslateService,
        private globals: UIRouterGlobals) { }

    ngOnInit() {
        this.$spinner.setLoadingState(false);
        this.returnTo = this.globals.params.returnTo;

        this.form = this.$fb.group({
            username: ["", Validators.required],
            password: ["", Validators.required]
        });

        window.document.getElementById("username").focus();
    }

    onSubmit({ value, valid }) {
        this.$spinner.setLoadingState(true);
        this.$auth.login(value.username, value.password)
            .then(() => {
                this.$spinner.setLoadingState(false);
                if (this.returnTo.name) {
                    this.$state.go(this.returnTo.name(), this.returnTo.params());
                } else {
                    this.$state.go("dashboard");
                }
            }).catch((err) => {
                this.$spinner.setLoadingState(false);
                this.invalidCredentials = true;
                this.toastr.error(this.translate.instant(err.error.message || err.statusText || err.message),
                    this.translate.instant("alert.type.error"));
            });
    }
}

const LoginStates = {
    name: "login",
    component: LoginComponent,
    data: {
        breadcrumb: "nav.login",
        requiresAuth: false
    },
    params: {
        returnTo: {}
    }
};

export { LoginComponent, LoginStates };
