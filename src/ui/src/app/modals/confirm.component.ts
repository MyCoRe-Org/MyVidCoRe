import { Component, OnInit } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { StateService } from "@uirouter/core";
import { ToastrService } from "ngx-toastr";
import { TranslateService } from "@ngx-translate/core";

import { Observable } from "rxjs";

import { ApiService } from "../_services/api.service";

interface I18NConfirmProperties {
    headline?: string;
    content: string;
    confirmBtn?: string;
    cancelBtn?: string;
    onSuccess?: string;
    onError?: string;
}

export interface ConfirmOptions {
    apiFunc?: (obj: any) => Observable<any>;
    object?: any;
    returnTo?: any;
    dangerConfirm?: boolean;
    i18n: I18NConfirmProperties;
}

@Component({
    selector: "ui-confirm-modal",
    templateUrl: "./confirm.component.html"
})
export class ConfirmComponent implements OnInit {

    public options: ConfirmOptions;

    constructor(public activeModal: NgbActiveModal, public toastr: ToastrService, public translate: TranslateService,
        private $api: ApiService, private $state: StateService) { }

    ngOnInit() {
    }

    confirm() {
        if (this.options.apiFunc && this.options.object) {
            this.options.apiFunc.call(this.$api, this.options.object).subscribe((res) => {
                this.activeModal.close();

                if (this.options.returnTo) {
                    this.$state.go(this.options.returnTo.name ? this.options.returnTo.name() : this.options.returnTo,
                        this.options.returnTo.params ? this.options.returnTo.params() : {});
                } else {
                    this.$state.reload();
                }

                if (this.options.i18n.onSuccess) {
                    this.toastr.success(this.translate.instant(this.options.i18n.onSuccess, this.options.object),
                        this.translate.instant("alert.type.success"));
                }
            }, (err) => {
                this.toastr.error(this.translate.instant(this.options.i18n.onError || err.error.message || err.statusText || err.message),
                    this.translate.instant("alert.type.error"));
            });
        } else {
            this.activeModal.close();

            if (this.options.returnTo) {
                this.$state.go(this.options.returnTo.name ? this.options.returnTo.name() : this.options.returnTo,
                    this.options.returnTo.params ? this.options.returnTo.params() : {});
            } else {
                this.$state.reload();
            }

            if (this.options.i18n.onSuccess) {
                this.toastr.success(this.translate.instant(this.options.i18n.onSuccess, this.options.object),
                    this.translate.instant("alert.type.success"));
            }
        }
    }

    cancel() {
        this.activeModal.close();
    }
}
