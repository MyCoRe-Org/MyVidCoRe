import { Injectable, EventEmitter } from "@angular/core";

@Injectable()
export class SpinnerService {

    public loadingEvent$: EventEmitter<boolean>;

    constructor() {
        this.loadingEvent$ = new EventEmitter(true);
    }

    setLoadingState(loading: boolean) {
        this.loadingEvent$.emit(loading);
    }

}
