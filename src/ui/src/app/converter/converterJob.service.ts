import { Injectable } from "@angular/core";

import { Subject } from "rxjs";
import { retry } from "rxjs/operators";

import { WebsocketService } from "../_services/websocket.service";
import { Job } from "./definitions";

const WS_CONTEXT = "/converter";

export interface AppEvent<T> {
    object: T;
    source: string;
    type: string;
}

@Injectable()
export class ConverterJobService {

    public events: Subject<AppEvent<Job>>;

    constructor(private wsService: WebsocketService<AppEvent<Job>>) {
        this.events = this.wsService.connect(WebsocketService.buildWSURL(WS_CONTEXT));
    }

}
