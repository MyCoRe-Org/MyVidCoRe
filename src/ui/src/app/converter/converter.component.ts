import { Component, OnInit, OnDestroy } from "@angular/core";

import { timer, Subscription, Observable } from "rxjs";

import videojs from "video.js";
import "../../../../../node_modules/@guaclive/videojs-resolution-switcher/lib/videojs-resolution-switcher.js";

import { environment } from "../../environments/environment";


import { ConverterApiService } from "./api.service";
import { ConverterJobService, AppEvent } from "./converterJob.service";
import { ErrorService } from "../_services/error.service";

import { Job } from "./definitions";
import { hashCode } from "../definitions/index.js";

interface Source {
    src: string;
    type: string;
    label?: string;
}

interface PlayerResSwitch extends videojs.Player {
    videoJsResolutionSwitcher?: (options?: any) => void;
    updateSrc?: (src: Array<Source>) => any;
}

@Component({
    selector: "ui-converter",
    templateUrl: "./converter.component.html"
})
export class ConverterComponent implements OnInit, OnDestroy {

    static REMOVE_TIMEOUT = 30000;

    static REFRESH_INTERVAL = 30000;

    baseUrl = environment.apiBaseUrl;

    timer: Subscription;

    socket: Subscription;

    doneJobs: any;

    runningJobs: any;

    done: Array<Job> = Array();

    running: Array<Job> = Array();

    jobStatus: Map<string, Observable<Job>> = new Map();

    videoPlayer: Map<string, PlayerResSwitch> = new Map();

    constructor(private $api: ConverterApiService, private $error: ErrorService, private $jobsvc: ConverterJobService) {
    }

    ngOnInit() {
        this.socket = this.$jobsvc.events.subscribe(evt => {
            this.handleEvent(evt);
        });

        this.timer = timer(100, ConverterComponent.REFRESH_INTERVAL).subscribe(() => {
            this.refreshDone();
            this.refreshRunning();
        });
    }

    ngOnDestroy() {
        this.timer.unsubscribe();
        this.socket.unsubscribe();

        this.videoPlayer.forEach(p => p.dispose());
    }

    onReload() {
        this.refreshRunning();
    }

    initPreview(id: string) {
        let player = this.videoPlayer.get(id);

        if (!player) {
            const playerId = "video-" + id;
            if (document.getElementById(playerId)) {
                player = videojs(playerId, {}, () => {
                    player.videoJsResolutionSwitcher({
                        "default": "high",
                        dynamicLabel: true
                    });
                });
            }

            this.videoPlayer.set(id, player);
        }
    }

    canStream(job: Job) {
        return job.files.findIndex(f => "mp4|webm".indexOf(f.format) !== -1) !== -1;
    }

    getJobStatus(id: string) {
        if (!this.jobStatus.get(id)) {
            this.jobStatus.set(id, this.$api.getJobStatus(id));
        }

        return this.jobStatus.get(id);
    }

    trackByJobId(_index: number, item: Job) {
        return item.hashCode || item.id;
    }

    formatScale(scale: string) {
        const sp = scale.split(":", 2);

        if (sp.length === 2) {
            if (parseInt(sp[0], 10) > 0) {
                return sp[0] + "x" + sp[1];
            }
            return sp[1] + "p";
        }

        return scale;
    }

    private refreshDone() {
        this.$api.getDone().toPromise().then((res: any) => this.doneJobs = res).
            catch(err => this.$error.handleError(err)).
            then(() => this.buildDone());
    }

    private refreshRunning() {
        this.$api.getRunning().toPromise().then((res: any) => this.runningJobs = res).
            catch(err => this.$error.handleError(err)).
            then(() => this.buildRunning());
    }

    private filterDone(job: Job) {
        const diff = new Date().getTime() - new Date(job.endTime).getTime();
        return job.running || !job.running && !job.done ?
            true : job.done && diff < ConverterComponent.REFRESH_INTERVAL ? true : false;
    }

    private sortByPercent(a: Job, b: Job) {
        return a.progress && a.progress.percent ?
            b.progress && b.progress.percent ?
                a.progress.percent < b.progress.percent ? 1 : -1
                : 0
            : 0;
    }

    private sortByEndTime(a: Job, b: Job) {
        return a.endTime < b.endTime ? 1 : -1;
    }

    private mergeJobs(from: Array<Job>, to: Array<Job>) {
        from.forEach((j: Job) => {
            const fi = to.findIndex(fj => fj.id === j.id);
            if (fi !== -1) {
                to[fi] = j;
            } else {
                to.push(j);
            }
        });
    }

    private injectHashCode(job: Job) {
        if (job && !job.hashCode) {
            job.hashCode = hashCode(job.id);
        }
        return job;
    }

    private buildDone() {
        this.mergeJobs(this.doneJobs && this.doneJobs.converter || [], this.done);
        this.done = this.done.filter(j => !this.filterDone(j)).map(this.injectHashCode).sort(this.sortByEndTime);
    }

    private buildRunning() {
        this.mergeJobs(this.doneJobs && this.doneJobs.converter || [], this.running);
        this.mergeJobs(this.runningJobs && this.runningJobs.converter || [], this.running);
        this.running = this.running.filter(this.filterDone).map(this.injectHashCode).sort(this.sortByPercent);
    }

    private handleEvent(event: AppEvent<Job>) {
        if (!this.running) {
            this.running = Array();
        }

        if (event.object) {
            event.object.hashCode = hashCode(event.object.id);
        }

        this.mergeJobs([event.object], this.running);
        this.running = this.running.filter(this.filterDone).map(this.injectHashCode).sort(this.sortByPercent);
        this.done = this.done.filter(j => !this.filterDone(j)).map(this.injectHashCode);
    }

}

export const ConverterStates = {
    name: "converter",
    url: "/converter",
    component: ConverterComponent,
    data: {
        parentState: "home",
        breadcrumb: "converter.breadcrumb",
        requiresAuth: false
    },
};
