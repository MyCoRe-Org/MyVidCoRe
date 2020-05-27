import { Component, OnInit, OnDestroy } from "@angular/core";

import { timer, Subscription, Observable } from "rxjs";

import videojs from "video.js";
import videojsPluginQualitySelector from "@silvermine/videojs-quality-selector";

import { environment } from "../../environments/environment";

import { ConverterApiService } from "./api.service";
import { ConverterJobService, AppEventData } from "./converterJob.service";
import { ErrorService } from "../_services/error.service";

import { Job } from "./definitions";
import { hashCode } from "../definitions/index.js";
import { StateService } from "@uirouter/core";

@Component({
    selector: "ui-converter",
    templateUrl: "./converter.component.html"
})
export class ConverterComponent implements OnInit, OnDestroy {

    static REMOVE_TIMEOUT = 30000;

    static REFRESH_INTERVAL = 30000;

    baseUrl = environment.apiBaseUrl;

    page = 1;

    start = 0;

    limit = 50;

    end = this.limit;

    timer: Subscription;

    socket: Subscription;

    doneJobs: any;

    runningJobs: any;

    done: Array<Job> = Array();

    running: Array<Job> = Array();

    jobStatus: Map<string, Observable<Job>> = new Map();

    videoPlayer: Map<string, videojs.Player> = new Map();

    constructor(private $api: ConverterApiService, private $error: ErrorService, private $state: StateService,
        private $jobsvc: ConverterJobService) {
        this.page = this.$state.params.page || 1;
        this.limit = this.$state.params.limit || 50;
    }

    ngOnInit() {
        this.socket = this.$jobsvc.events.subscribe(evt => {
            this.handleEvent(evt.event);
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
                player = videojs(playerId, {
                    controlBar: {
                        children: [
                            "playToggle",
                            "progressControl",
                            "volumePanel",
                            "qualitySelector",
                            "fullscreenToggle",
                        ],
                    },
                }, () => {
                    videojsPluginQualitySelector(videojs);
                    player.controlBar.addChild("QualitySelector");
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

    trackByJobId(index: number, item: Job) {
        return item && (item.hashCode || item.id) || index;
    }

    formatScale(scale: string) {
        if (scale) {
            const sp = scale.split(":", 2);

            if (sp.length === 2) {
                if (parseInt(sp[0], 10) > 0) {
                    return sp[0] + "x" + sp[1];
                }
                return sp[1] + "p";
            }
        }
        return scale;
    }

    pageChange(page: number) {
        this.page = page;
        this.start = (this.page - 1) * this.limit;
        this.end = Math.min(this.start + this.limit, this.doneJobs.total);
        this.transitionTo();
    }

    private refreshDone() {
        this.$api.getDone(this.page || 1, this.limit).toPromise().then((res: any) => this.doneJobs = res).
            catch(err => this.$error.handleError(err)).
            then(() => this.buildDone());
    }

    private refreshRunning() {
        this.$api.getRunning().toPromise().then((res: any) => this.runningJobs = res).
            catch(err => this.$error.handleError(err)).
            then(() => this.buildRunning());
    }

    private filterDone(job: Job) {
        if (!job) {
            return false;
        }

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
            const fi = to.findIndex(fj => fj && j && fj.id === j.id);
            if (fi !== -1) {
                to[fi] = j;
            } else {
                to.push(j);
            }
        });

        return to.filter(j => {
            return from.findIndex(fj => fj && j && fj.id === j.id) !== -1;
        });
    }

    private injectHashCode(job: Job) {
        if (job && !job.hashCode) {
            job.hashCode = hashCode(job.id);
        }
        return job;
    }

    private buildDone() {
        this.done = this.mergeJobs(this.doneJobs && this.doneJobs.converter || [], this.done);
        this.done = this.done.filter(j => !this.filterDone(j)).map(this.injectHashCode).sort(this.sortByEndTime);

        this.start = this.doneJobs && this.doneJobs.start || 0;
        this.end = Math.min(this.start + this.limit, this.doneJobs && this.doneJobs.total || 0);
    }

    private buildRunning() {
        this.mergeJobs(this.doneJobs && this.doneJobs.converter || [], this.running);
        this.mergeJobs(this.runningJobs && this.runningJobs.converter || [], this.running);
        this.done = this.mergeJobs(this.running.filter(j => !this.filterDone(j)), this.done);
        this.running = this.running.filter(this.filterDone).map(this.injectHashCode).sort(this.sortByPercent);
    }

    private handleEvent(event: AppEventData<Job>) {
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

    private transitionTo() {
        this.$state.transitionTo(this.$state.$current.name, {
            page: this.page,
            limit: this.limit
        }, { reload: true });
    }

}

export const ConverterStates = {
    name: "converter",
    url: "/converter",
    component: ConverterComponent,
    data: {
        parentState: "dashboard",
        breadcrumb: "converter.breadcrumb",
        requiresAuth: false
    },
    params: {
        page: {
            type: "int",
            value: 1,
            squash: true
        },
        limit: {
            type: "int",
            value: 50,
            squash: true
        }
    },
};
