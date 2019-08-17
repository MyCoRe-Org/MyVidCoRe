import { Injectable } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";

import { environment } from "../../environments/environment";

export enum DetailLevel {
    SUMMARY = "summary",
    NORMAL = "normal",
    DETAILED = "detailed"
}

@Injectable()
export class ApiService {

    public base = environment.apiBaseUrl;

    public httpOptions = environment.apiHttpOptions;

    static getFileNameFromResponseContentDisposition(res: HttpResponse<any>) {
        const contentDisposition = res.headers.get("content-disposition") || "";
        const re = new RegExp("filename\\s*=\\s*\"?([^;\"]+)", "i");
        if (re.test(contentDisposition)) {
            const matches = re.exec(contentDisposition);
            const fileName = (matches[1] || "untitled").trim();
            return fileName;
        }

        return null;
    }

    constructor(public $http: HttpClient) { }

    acceptDetailsHeader(level: DetailLevel = DetailLevel.NORMAL, mimeType: string = "application/json") {
        const options = Object.assign({}, this.httpOptions);

        if (level !== DetailLevel.NORMAL) {
            if (!options["headers"]) {
                options["headers"] = {};
            }
            options["headers"]["accept"] = `${mimeType}; detail=${level}`;
        }

        return options;
    }

    forceNoCache(force: boolean, options?: Object) {
        const opts = Object.assign({}, options || this.httpOptions);

        if (force) {
            if (!opts["headers"]) {
                opts["headers"] = {};
            }
            opts["headers"]["Cache-Control"] = [
                "no-cache",
                "max-age=0",
                "must-revalidate"
            ];
        }

        return opts;
    }

    /**
     * Access
     */
    login(username: string, password: string) {
        return this.$http.post(`${this.base}/access/authorize`, { username: username, password: password }, this.httpOptions);
    }

    logout() {
        return this.$http.get(`${this.base}/access/dismiss`, this.httpOptions);
    }

    getAuthorized() {
        return this.$http.get(`${this.base}/access/authorized`, this.httpOptions);
    }

    isAllowed(roles: Array<String>) {
        return this.$http.post(`${this.base}/access/isAllowed`, roles, this.httpOptions);
    }

    /**
     * External Assets
     */
    gitInfo() {
        return this.$http.get(`${this.base}/assets/git.json`, this.httpOptions);
    }

}
