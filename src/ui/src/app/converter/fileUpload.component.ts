import {
    Component, Directive, OnInit, Output, HostListener,
    EventEmitter,
    Input
} from "@angular/core";

import { Subject } from "rxjs";
import { distinctUntilChanged } from "rxjs/operators";

import { ErrorService } from "../_services/error.service";
import { ConverterApiService } from "./api.service";

interface FileItem {
    file: File | any;
    progressEvent?: Subject<number>;
    progress?: number;
    complete: boolean;
    processing: boolean;
    error: boolean;
    created?: number;
    started?: number;
    completed?: number;
}

@Component({
    selector: "ui-fileupload",
    templateUrl: "./fileUpload.component.html",
    styleUrls: ["fileUpload.component.scss"]
})
export class FileUploadComponent implements OnInit {

    private static MAX_CONCURRENT_UPLOADS = 1;

    private static QUEUE_REMOVE_TIMEOUT = 30000;

    public queue: Array<FileItem> = new Array();

    @Input()
    public accept = "*/*";

    @Output()
    public reload = new EventEmitter();

    constructor(private $api: ConverterApiService, private $error: ErrorService) {
    }

    ngOnInit() {
    }

    onFiles(files: any) {
        if (files instanceof Array) {
            files.forEach(f => {
                const qitem: FileItem = {
                    file: f,
                    progressEvent: new Subject(),
                    complete: false,
                    processing: false,
                    error: false,
                    created: Date.now()
                };
                qitem.progressEvent.pipe(distinctUntilChanged()).subscribe(p => qitem.progress = p);
                this.queue.push(qitem);
            });

            this.invokeUpload();
        }
    }

    retry(item: FileItem) {
        item.processing = false;
        item.error = false;
        item.complete = false;

        this.invokeUpload();
    }

    trackByItem(_index: number, item: FileItem) {
        return item.file.name + item.created;
    }

    private queueItemProcess(item: FileItem) {
        if (item.complete === false && item.processing === false && item.error === false) {
            item.started = Date.now();
            item.processing = true;
            this.$api.
                addJob(item.file, item.progressEvent).
                subscribe((res) => this.queueItemDone(item, res), (err) => this.queueItemError(item, err));
        }
    }

    private queueItemDone(item: FileItem, res: any) {
        item.processing = false;
        item.complete = true;
        item.completed = Date.now();

        if (res.status === 200 || res.status === 201 || res.status === 204) {
            item.error = false;
        } else {
            item.error = true;
        }

        setTimeout(() => {
            const ii = this.queue.findIndex(i => i === item);
            if (ii !== -1) {
                this.queue.splice(ii, 1);
            }
        }, FileUploadComponent.QUEUE_REMOVE_TIMEOUT);

        this.invokeUpload();
    }

    private queueItemError(item: FileItem, err: any) {
        if (err.status === 200) {
            this.queueItemDone(item, err);
            return;
        }

        item.processing = false;
        item.complete = true;
        item.error = true;

        this.$error.handleError(err);

        this.invokeUpload();
    }

    private invokeUpload() {
        const waiting = this.queue.
            filter(i => i.complete === false && i.processing === false && i.error === false).
            slice(0, FileUploadComponent.MAX_CONCURRENT_UPLOADS);

        if (waiting.length === 0) {
            this.reload.next(true);
        } else {
            waiting.forEach((item) => {
                this.queueItemProcess(item);
            });
        }
    }

}

@Directive({ selector: "[fileDropZone]" })
export class FileDropZoneDirective {

    @Output()
    public files = new EventEmitter();

    @HostListener("dragenter", ["$event"])
    @HostListener("dragover", ["$event"])
    onDragOver(event: Event) {
        event.preventDefault();

        // TODO check if accept
    }

    @HostListener("drop", ["$event"])
    onDrop(event: any) {
        event.preventDefault();

        if (event.dataTransfer.items) {
            this.getFilesWebkitDataTransferItems(event.dataTransfer.items).then((files) => this.upload(files));
        } else {
            this.upload(event.dataTransfer.files);
        }
    }

    @HostListener("change", ["$event"])
    onChange(event: any) {
        event.preventDefault();

        if (event.target.webkitEntries) {
            this.getFilesWebkitDataTransferItems(event.target.webkitEntries).then((files) => this.upload(files));
        } else {
            this.upload(event.target.files);
        }
    }

    private getFilesWebkitDataTransferItems(dataTransferItems: any) {
        const files = [];

        function traverseFileTreePromise(item, path = "") {
            return new Promise(resolve => {
                if (item.isFile) {
                    item.file(file => {
                        file.filepath = path + file.name;
                        files.push(file);
                        resolve(file);
                    });
                } else if (item.isDirectory) {
                    const dirReader = item.createReader();
                    dirReader.readEntries(entries => {
                        const entriesPromises = [];
                        for (const entr of entries) {
                            entriesPromises.push(traverseFileTreePromise(entr, path + item.name + "/"));
                        }
                        resolve(Promise.all(entriesPromises));
                    });
                }
            });
        }

        return new Promise((resolve) => {
            const entriesPromises = [];
            for (const it of dataTransferItems) {
                entriesPromises.push(traverseFileTreePromise(it.webkitGetAsEntry ? it.webkitGetAsEntry() : it));
            }
            Promise.all(entriesPromises).then(() => resolve(files));
        });
    }

    private upload(files: any) {
        this.files.next(files);
    }
}
