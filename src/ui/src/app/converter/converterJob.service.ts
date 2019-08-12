import { Injectable } from "@angular/core";

import { Subject } from "rxjs";
import { map, retry } from "rxjs/operators";

import { environment } from "../../environments/environment";

import { WebsocketService } from "../_services/websocket.service";
import { Job } from "./definitions";

const WS_URL = `${environment.wsBaseUrl}/ws/converter`;

export interface AppEvent<T> {
    object: T;
    source: string;
    type: string;
}

@Injectable()
export class ConverterJobService {

    public events: Subject<AppEvent<Job>>;

    constructor(wsService: WebsocketService) {
        this.events = <Subject<AppEvent<Job>>>wsService.connect(WS_URL).pipe(
            map((response: MessageEvent): AppEvent<Job> => {
                const data = JSON.parse(response.data);
                return data.event;
            },
            ),
            retry(3)
        );
    }
}
