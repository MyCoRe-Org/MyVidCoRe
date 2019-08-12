import { Injectable } from "@angular/core";
import { HttpClient, HttpRequest, HttpEventType } from "@angular/common/http";

import { Subject } from "rxjs";
import { map, distinctUntilChanged, last } from "rxjs/operators";

import { ApiService } from "../_services/api.service";
import { Job, Codecs, Formats, HWAccels } from "./definitions";

@Injectable()
export class ConverterApiService extends ApiService {

    constructor(public $http: HttpClient) {
        super($http);
    }

    getRunning(start: number = 1, limit: number = 10) {
        return this.$http.get(`${this.base}/widget/converter/${start}/${limit}/isdone/status`, this.forceNoCache(true));
    }

    getDone(start: number = 1, limit: number = 50) {
        return this.$http.get(`${this.base}/widget/converter/${start}/${limit}/!isdone/status`, this.forceNoCache(true));
    }

    getJobStatus(id: string) {
        return this.$http.get(`${this.base}/widget/converter/${id}/status`, this.forceNoCache(true)).
            pipe(map((res: Job) => res));
    }


    addJob(file: File, progress?: Subject<number>) {
        const formData = new FormData();
        formData.append("file", file);

        const req = new HttpRequest(
            "POST",
            `${this.base}/converter/addjob`,
            formData,
            {
                reportProgress: progress !== undefined
            }
        );

        return this.$http.request(req).pipe(
            distinctUntilChanged(),
            map(event => {
                if (event.type === HttpEventType.UploadProgress && progress && event.total) {
                    const percentDone = Math.round(100 * event.loaded / event.total);
                    progress.next(percentDone);
                } else if (event.type === HttpEventType.Response) {
                    if (progress) {
                        progress.next(100);
                        progress.complete();
                    }

                    return event;
                }
            }),
            last()
        );
    }

    getCodecs(filter?: string, value?: string, force: boolean = false) {
        const p = filter ? `/${filter}` + (value ? `/${value}` : "") : "";

        return this.$http.get(`${this.base}/converter/codecs${p}`, this.forceNoCache(force)).
            pipe(
                map((codecs: Codecs) => codecs.codecs || null)
            );
    }

    getFormats(filter?: string, value?: string, force: boolean = false) {
        const p = filter ? `/${filter}` + (value ? `/${value}` : "") : "";

        return this.$http.get(`${this.base}/converter/formats${p}`, this.forceNoCache(force)).
            pipe(
                map((formats: Formats) => formats.formats || null)
            );
    }

    getEncoder(name: string, force: boolean = false) {
        return this.$http.get(`${this.base}/converter/encoder/${name}`, this.forceNoCache(force));
    }

    getDecoder(name: string, force: boolean = false) {
        return this.$http.get(`${this.base}/converter/decoder/${name}`, this.forceNoCache(force));
    }

    getHWAccels(force: boolean = false) {
        return this.$http.get(`${this.base}/converter/hwaccels`, this.forceNoCache(force)).
            pipe(
                map((hwaccels: HWAccels) => hwaccels.hwaccels || null)
            );
    }

}
