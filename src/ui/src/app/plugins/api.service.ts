import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";

import { map } from "rxjs/operators";

import { ApiService } from "../_services/api.service";
import { Plugins } from "./definitions";

@Injectable()
export class PluginApiService extends ApiService {

    constructor(public $http: HttpClient) {
        super($http);
    }

    getPlugins(force: boolean = false) {
        return this.$http.get(`${this.base}/plugins`, this.forceNoCache(force));
    }

    isPluginEnabled(name: string) {
        return this.getPlugins(false).pipe(map((plugins: Plugins) => {
            const ps = plugins.plugins.find(p => name === p.name);
            return ps && ps.enabled === true;
        }));
    }

}
