import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";

import { ApiService } from "../_services/api.service";
import { Settings } from "./definitions";

@Injectable()
export class SettingsApiService extends ApiService {

    constructor(public $http: HttpClient) {
        super($http);
    }

    getSettings(force: boolean = false) {
        return this.$http.get(`${this.base}/settings`, this.forceNoCache(force));
    }

    saveSettings(settings: Settings) {
        return this.$http.post(`${this.base}/settings`, settings);
    }

}
