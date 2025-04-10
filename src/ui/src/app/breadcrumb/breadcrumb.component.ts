import { Component } from "@angular/core";
import { StateService } from "@uirouter/core";

import { BreadcrumbService } from "./breadcrumb.service";

@Component({
    selector: "ui-breadcrumb",
    templateUrl: "./breadcrumb.component.html",
    standalone: false
})
export class BreadcrumbComponent {

    constructor(public $breadcrumb: BreadcrumbService, public $state: StateService) { }

    textEllipsis(text: string, maxChars: number = 50, ellipsisChars: string = "...") {
        return text.length > maxChars
            ? [text.substr(0, maxChars - ellipsisChars.length), ellipsisChars].join("") : text;
    }
}
