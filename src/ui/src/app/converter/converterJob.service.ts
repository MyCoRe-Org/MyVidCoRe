import { Injectable } from "@angular/core";

import { Subject } from "rxjs";
import { map, retry } from "rxjs/operators";

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

    constructor(wsService: WebsocketService) {
        this.events = <Subject<AppEvent<Job>>>wsService.connect(WebsocketService.buildWSURL(WS_CONTEXT)).pipe(
            map((response: MessageEvent): AppEvent<Job> => {
                const data = JSON.parse(response.data);
                return data.event;
            },
            ),
            retry(3)
        );
    }
}
