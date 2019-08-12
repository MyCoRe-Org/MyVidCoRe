import { Component, OnInit } from "@angular/core";

import { AuthService } from "../_services/auth.service";

@Component({
    selector: "ui-home",
    templateUrl: "./home.component.html"
})
export class HomeComponent implements OnInit {

    constructor(public $auth: AuthService) { }

    ngOnInit() {
    }

}

export const HomeStates = {
    name: "home",
    url: "/",
    redirectTo: "converter",
    component: HomeComponent,
    data: {
        breadcrumb: "nav.home",
        requiresAuth: false
    }
};
