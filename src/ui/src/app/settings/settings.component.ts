import { Component, OnInit, Input } from "@angular/core";
import { FormBuilder, FormArray, FormGroup, Validators } from "@angular/forms";

import { of, forkJoin } from "rxjs";
import { mergeMap, map, mergeAll, take, delay, retryWhen } from "rxjs/operators";

import { Transition } from "@uirouter/core";

import { ConverterApiService } from "../converter/api.service";
import { SettingsApiService } from "./api.service";
import { ErrorService } from "../_services/error.service";
import { SpinnerService } from "../spinner/spinner.service";

import { Codec, Format } from "../converter/definitions";
import { Settings, Output, AllowedFormat, HWAccel } from "./definitions";
import { DEFAULT_BITRATES, DEFAULT_FORMATS } from "./defaults";
import { SettingsVideoComponent } from "./settings.video.component";
import { SettingsAudioComponent } from "./settings.audio.component";
import { SettingsExtendedComponent } from "./settings.extended.component";

@Component({
    selector: "ui-settings",
    templateUrl: "./settings.component.html"
})
export class SettingsComponent implements OnInit {

    @Input()
    hwaccels: Array<HWAccel>;

    @Input()
    codecs: Array<Codec>;

    @Input()
    formats: Array<Format>;

    @Input()
    settings: Settings;

    audioBitrates = DEFAULT_BITRATES;

    allowedFormats: Array<AllowedFormat>;

    selectedFormat: Array<AllowedFormat> = new Array();

    activeTab: string;

    form: FormGroup;

    hwAccelsForm: FormArray;

    output: FormArray;

    constructor(private $api: SettingsApiService, private $capi: ConverterApiService,
        private $error: ErrorService, private $spinner: SpinnerService, public $fb: FormBuilder) {
    }

    ngOnInit() {
        this.$spinner.setLoadingState(true);

        this.output = this.$fb.array([]);
        this.form = this.$fb.group({
            output: this.output,
            plugins: this.$fb.array([])
        });

        if (this.hwaccels && this.hwaccels.length > 0) {
            this.hwAccelsForm = this.$fb.array([]);
            this.hwaccels.forEach(hwa => this.hwAccelsForm.push(this.$fb.group({
                index: [hwa.index],
                name: [hwa.name],
                type: [hwa.type]
            })));
            this.form.addControl("hwaccels", this.hwAccelsForm);
        }

        this.buildAllowedFormats().toPromise()
            .then(res => this.allowedFormats = res)
            .catch((err) => {
                this.$spinner.setLoadingState(false);
                this.$error.handleError(err);
            })
            .then(() => {
                if (this.settings) {
                    this.settings.output.forEach((o, i) => {
                        this.output.push(this.createOutput(o));
                        this.onFormatChange(i);
                    });

                    this.form.patchValue(this.settings);
                } else {
                    this.output.push(this.createOutput());
                    // this.onFormatChange(0);
                }

                this.activeTab = "tab-output-0";
                this.$spinner.setLoadingState(false);
            })
            .catch((err) => {
                this.$spinner.setLoadingState(false);
                this.$error.handleError(err);
            });
    }

    private resolveEncoders(codecs: Array<Codec>) {
        return of(codecs).pipe(
            mergeMap(cl =>
                forkJoin(cl.map(cc => {
                    if (cc.encoders && cc.encoders.encoder) {
                        return of(cc).pipe(
                            mergeMap(cce => of(cce.encoders.encoder).pipe(
                                mergeMap(el => forkJoin(el.map(e =>
                                    this.$capi.getEncoder(e).pipe(
                                        retryWhen(errors => errors.pipe(delay(10000), take(3))),
                                        map((res: any) => res.encoders[0])
                                    )
                                )))
                            ).pipe(map(re => {
                                cce.encoders.encoder = re;
                                return cce;
                            })))
                        );
                    }

                    return of(cc).pipe(
                        mergeMap(cce => this.$capi.getEncoder(cce.name).pipe(
                            retryWhen(errors => errors.pipe(delay(10000), take(3))),
                            map((res: any) => res.encoders[0]))
                        )
                    ).pipe(
                        map(re => {
                            cc.encoders = { encoder: [re] };
                            return cc;
                        })
                    );
                }))
            )
        );
    }

    private buildAllowedFormats() {
        this.allowedFormats = Object.keys(DEFAULT_FORMATS).map(f => {
            return {
                name: f,
                description: this.filterFormat(f).description || "",
                audio: DEFAULT_FORMATS[f].audio.map((n: string) => this.filterCodecs(n)).filter((r: any) => r),
                video: DEFAULT_FORMATS[f].video.map((n: string) => this.filterCodecs(n)).filter((r: any) => r)
            };
        });

        return of(this.allowedFormats).pipe(
            mergeMap(laf =>
                forkJoin(laf.map(
                    af => forkJoin([
                        of(af.audio).pipe(
                            retryWhen(errors => errors.pipe(delay(10000), take(3))),
                            map(a => this.resolveEncoders(a)), mergeAll()
                        ),
                        of(af.video).pipe(
                            retryWhen(errors => errors.pipe(delay(10000), take(3))),
                            map(v => this.resolveEncoders(v)), mergeAll()
                        )
                    ]).pipe(
                        map(renc => {
                            af.audio = <Array<Codec>>renc[0];
                            af.video = <Array<Codec>>renc[1];
                            return af;
                        })
                    )
                ))
            )
        );
    }

    numOutputs() {
        const o = [];
        for (let i = 0; i <= this.output.controls.length; i++) {
            o.push(i);
        }
        return o;
    }

    isHwAccelsChecked(index: number) {
        return this.hwAccelsForm && this.hwAccelsForm.value &&
            this.hwAccelsForm.value.findIndex((v: HWAccel) => v.index === index) !== -1;
    }

    onHWAccelChange(index: number) {
        if (this.isHwAccelsChecked(index)) {
            this.hwAccelsForm.removeAt(this.hwAccelsForm.value.findIndex((v: HWAccel) => v.index === index));
        } else {
            const hwa = this.hwaccels.find((v: HWAccel) => v.index === index);
            this.hwAccelsForm.push(this.$fb.group({
                index: [hwa.index],
                name: [hwa.name],
                type: [hwa.type]
            }));
        }
    }

    filterFormat(name: string): Format {
        return this.formats.find(f => f.name === name);
    }

    filterCodecs(name: string): Codec {
        return this.codecs.find(f => f.name === name);
    }

    filterParameter(params: Array<any>, name: string) {
        return params.find(p => p.name === name);
    }

    createOutput(output: Output = null): FormGroup {
        const fg = this.$fb.group({
            format: ["", [Validators.required]],
            filenameAppendix: [""],
            audio: SettingsAudioComponent.createAudio(this.$fb, output && output.audio),
            video: SettingsVideoComponent.createVideo(this.$fb, output && output.video)
        });

        if (this.hwaccels && this.hwaccels.length > 0) {
            fg.addControl("video-fallback", SettingsVideoComponent.createVideo(this.$fb, output && output["video-fallback"]));
        }

        if (output) {
            fg.patchValue(output);
        }

        return fg;
    }

    addOutput(event: MouseEvent) {
        event.preventDefault();
        this.output.push(this.createOutput());
        this.onFormatChange(this.output.length - 1);
    }

    removeOutput(event: MouseEvent, index: number) {
        event.preventDefault();
        event.stopImmediatePropagation();
        if (this.output.controls.length > 1) {
            this.output.removeAt(index);
            this.activeTab = "tab-output-" + (this.output.length - 1);
        }
    }

    onFormatChange(index: number) {
        const sel = this.output.controls[index].get("format").value;
        this.selectedFormat[index] = sel && (this.allowedFormats.find(f => f.name === sel) || this.allowedFormats[0]) || null;
    }

    private buildParamName(name: string) {
        const re = /^\d.*/;
        return name && re.test(name) ? "_" + name : name;
    }

    onSubmit({ value, valid }) {
        if (valid) {
            value.output.forEach((o: Output) => {
                ["video", "video-fallback", "audio"].forEach(type => {
                    if (o[type]) {
                        const sel = o[type].codec;
                        const cc = this.allowedFormats.find(f => f.name === o.format);
                        const sc = cc[type === "audio" ? type : "video"].
                            find(c => c.name === sel || c.encoders && c.encoders.encoder.find(e => e.name === sel));
                        const se = sc ?
                            sc.encoders.encoder.length === 1 ? sc.encoders.encoder[0] : sc.encoders.encoder.find(e => e.name === sel)
                            : cc.video[0].encoders.encoder[0];

                        o[type].parameter = SettingsExtendedComponent.cleanSettings(se.parameters, o[type].parameter);

                        if (o[type].parameter) {
                            const params = <(key: string) => string>{};
                            Object.keys(o[type].parameter).forEach(n =>
                                params[this.buildParamName(n)] = o[type].parameter[n]
                            );
                            o[type].parameter = params;
                        }
                    }
                });
            });

            this.$api.saveSettings(value).toPromise().then((res) => {
                this.$spinner.setLoadingState(false);
                this.$error.handleMessage("success", res, "settings.action.saved");
            }).catch((err) => {
                this.$spinner.setLoadingState(false);
                this.$error.handleError(err);
            });
        }
    }

}

export function resolveFnCodecs($api: ConverterApiService, $error: ErrorService, $spinner: SpinnerService, trans: Transition) {
    $spinner.setLoadingState(trans.options().source !== "url" && trans.from().name !== trans.to().name);

    const reload = typeof trans.options().reload === "boolean" ? <boolean>trans.options().reload : false;

    return $api.getCodecs(null, null, reload).pipe(
        retryWhen(errors => errors.pipe(delay(10000), take(3)))
    ).toPromise().then((res: any) => {
        $spinner.setLoadingState(false);
        return res;
    }).catch((err) => {
        $spinner.setLoadingState(false);
        $error.handleError(err);
    });
}

export function resolveFnFormats($api: ConverterApiService, $error: ErrorService, $spinner: SpinnerService, trans: Transition) {
    $spinner.setLoadingState(trans.options().source !== "url" && trans.from().name !== trans.to().name);

    const reload = typeof trans.options().reload === "boolean" ? <boolean>trans.options().reload : false;

    return $api.getFormats(null, null, reload).pipe(
        retryWhen(errors => errors.pipe(delay(10000), take(3)))
    ).toPromise().then((res: any) => {
        $spinner.setLoadingState(false);
        return res;
    }).catch((err) => {
        $spinner.setLoadingState(false);
        $error.handleError(err);
    });
}

export function resolveFnHWaccels($api: ConverterApiService, $error: ErrorService, $spinner: SpinnerService, trans: Transition) {
    $spinner.setLoadingState(trans.options().source !== "url" && trans.from().name !== trans.to().name);

    const reload = typeof trans.options().reload === "boolean" ? <boolean>trans.options().reload : false;

    return $api.getHWAccels(reload).toPromise().then((res: any) => {
        $spinner.setLoadingState(false);
        return res;
    }).catch((err) => {
        $spinner.setLoadingState(false);
        $error.handleError(err);
    });
}

export function resolveFnSettings($api: SettingsApiService, $error: ErrorService, $spinner: SpinnerService, trans: Transition,
    _codecs: Array<Codec>, _formats: Array<Format>, _hwAccesls: Array<HWAccel>) {
    $spinner.setLoadingState(trans.options().source !== "url" && trans.from().name !== trans.to().name);

    const reload = typeof trans.options().reload === "boolean" ? <boolean>trans.options().reload : false;

    return $api.getSettings(reload).toPromise().then((res: any) => {
        $spinner.setLoadingState(false);
        return res;
    }).catch((err) => {
        $spinner.setLoadingState(false);
        $error.handleError(err);
    });
}

export const SettingsStates = {
    name: "settings",
    url: "/settings",
    component: SettingsComponent,
    data: {
        parentState: "dashboard",
        breadcrumb: "settings.breadcrumb",
        requiresAuth: false
    },
    resolve: [
        {
            token: "codecs",
            deps: [ConverterApiService, ErrorService, SpinnerService, Transition],
            resolveFn: resolveFnCodecs
        },
        {
            token: "formats",
            deps: [ConverterApiService, ErrorService, SpinnerService, Transition],
            resolveFn: resolveFnFormats
        },
        {
            token: "hwaccels",
            deps: [ConverterApiService, ErrorService, SpinnerService, Transition],
            resolveFn: resolveFnHWaccels
        },
        {
            token: "settings",
            deps: [SettingsApiService, ErrorService, SpinnerService, Transition, "codecs", "formats", "hwaccels"],
            resolveFn: resolveFnSettings
        }
    ]
};
