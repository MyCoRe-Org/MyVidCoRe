import { Component, OnInit } from "@angular/core";

import { SpinnerService } from "./spinner.service";

@Component({
    selector: "ui-spinner",
    templateUrl: "./spinner.component.html",
    styleUrls: ["./spinner.component.scss"]
})
export class SpinnerComponent implements OnInit {

    public loading = false;

    constructor(private $spinner: SpinnerService) {
        this.$spinner.loadingEvent$.subscribe((state) => {
            this.loading = state;
        });
    }

    ngOnInit() {

    }

}
