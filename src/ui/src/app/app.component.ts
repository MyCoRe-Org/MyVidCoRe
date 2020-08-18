import { Component, ViewChild, OnInit } from "@angular/core";
import { StateService, UIRouterGlobals } from "@uirouter/core";
import { TranslateService } from "@ngx-translate/core";

import { AuthService } from "./_services/auth.service";
import { ApiService } from "./_services/api.service";

import { of } from "rxjs";

import { ROUTES, RouteFlag, RouteItem, RouteMenu } from "./routes";

@Component({
    selector: "ui-root",
    templateUrl: "./app.component.html"
})
export class AppComponent implements OnInit {

    private langs = ["de", "en"];

    @ViewChild("sidebar")
    public sidebar: any;

    @ViewChild("content")
    public content: any;

    public gitInfo: any;

    public Flags = RouteFlag;

    public routes: Array<RouteMenu> = ROUTES;

    constructor(public $auth: AuthService, public translate: TranslateService,
        private $api: ApiService, private $state: StateService, private globals: UIRouterGlobals) {
        translate.addLangs(this.langs);
        translate.setDefaultLang(this.langs[0]);

        const lang = window.localStorage.getItem("lang") || translate.getBrowserLang();
        document.querySelector("html").setAttribute("lang", lang);
        translate.use(lang);
    }

    ngOnInit() {
        this.$api.gitInfo().subscribe(gitInfo => this.gitInfo = gitInfo);
    }

    public toggleSidebar() {
        this.sidebar.nativeElement.classList.toggle("active");
        this.content.nativeElement.classList.toggle("active");
    }

    public editUser() {
        this.$state.go("user", {
            action: "updateProfile",
            name: this.$auth.user.name,
            returnTo: this.$state.target(this.globals.current, this.globals.params)
        });
    }

    public anyAllowed(items: Array<RouteItem>) {
        const roles = [];
        items.forEach(i => {
            if (i.roles) {
                i.roles.forEach((r) => roles.indexOf(r) === -1 && roles.push(r));
            }
        });
        return roles.length !== 0 && this.$auth.isAllowed(roles) || of(true);
    }

    public refId(item: RouteItem | RouteMenu) {
        return item.id || "ref-" + item.ref.split(".").join("-");
    }

}
