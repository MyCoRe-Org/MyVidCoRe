import { Component, OnInit, Input, OnChanges, SimpleChanges } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup, Validators, ControlContainer, FormGroupDirective } from "@angular/forms";

import { Settings, Video, AllowedFormat } from "./definitions";
import {
    DEFAULT_SCALES, DEFAULT_LEVELS,
    DEFAULT_PROFILES, DEFAULT_PRESETS, DEFAULT_TUNES, DEFAULT_FRAMERATES
} from "./defaults";
import { SettingsExtendedComponent } from "./settings.extended.component";

@Component({
    selector: "ui-settings-video",
    templateUrl: "./settings.video.component.html",
    viewProviders: [
        { provide: ControlContainer, useExisting: FormGroupDirective }
    ]
})
export class SettingsVideoComponent implements OnInit, OnChanges {

    @Input()
    index: number;

    @Input()
    type: string;

    @Input()
    settings: Settings;

    @Input()
    selectedFormat: AllowedFormat;

    selectedEncoder: any;

    framerates = DEFAULT_FRAMERATES;

    scales = DEFAULT_SCALES;

    profiles = DEFAULT_PROFILES;

    levels = DEFAULT_LEVELS;

    presets = DEFAULT_PRESETS;

    tunes = DEFAULT_TUNES;

    output: UntypedFormGroup;

    static createVideo($fb: UntypedFormBuilder, video: Video = null): UntypedFormGroup {
        const fg = $fb.group({
            codec: [null, [Validators.required]],
            forceKeyFrames: [],
            framerate: [],
            framerateType: ["VFR"],
            pixelFormat: [],
            scale: [],
            upscale: [false],
            parameter: $fb.group({}),
            quality: $fb.group({
                bitrate: [],
                bufsize: [],
                maxrate: [],
                minrate: [],
                rateFactor: [],
                scale: [],
                type: []
            })
        });

        if (video) {
            fg.patchValue(video);
        }

        return fg;
    }

    constructor(private parent: FormGroupDirective, public $fb: UntypedFormBuilder) {
    }

    ngOnInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.selectedFormat.previousValue !== changes.selectedFormat.currentValue) {
            this.selectedFormat = changes.selectedFormat.currentValue;

            const form = <UntypedFormGroup>this.parent.form.get("output." + this.index);

            if (form.contains(this.type)) {
                const output = this.settings && this.settings.output[this.index] && this.settings.output[this.index][this.type];

                form.addControl(
                    this.type,
                    SettingsVideoComponent.createVideo(this.$fb, output)
                );
            }

            this.output = <UntypedFormGroup>this.parent.form.get("output." + this.index + "." + this.type);
            this.onCodecChange();
            this.onQualityTypeChange();
        }
    }

    filterParameter(params: Array<any>, name: string) {
        return params.find(p => p.name === name);
    }

    createParameters() {
        const params = <UntypedFormGroup>this.output.get("parameter");
        if (params && Object.keys(params.controls).length !== 0) {
            const ctrKeys = Object.keys(params.controls);
            ctrKeys.forEach(n => {
                if (this.selectedEncoder.parameters.find((p: any) => p.name === n) === -1) {
                    params.removeControl(n);
                }
            });

            this.selectedEncoder.parameters.forEach((p: any) => {
                if (ctrKeys.indexOf(p.name) === -1) {
                    params.addControl(p.name, SettingsExtendedComponent.createParameter(this.$fb, p));
                }
            });
        } else {
            this.selectedEncoder.parameters.forEach((p: any) =>
                params.addControl(p.name, SettingsExtendedComponent.createParameter(this.$fb, p))
            );

            SettingsExtendedComponent.patchValues(
                <UntypedFormGroup>this.output.get("parameter"),
                this.selectedEncoder.parameters,
                this.settings && this.settings.output[this.index] && this.settings.output[this.index][this.type].parameter
            );
        }
    }

    onCodecChange() {
        if (!this.selectedEncoder) {
            this.selectedEncoder = <(key: string) => any>{};
        }

        const sel = this.output.get("codec").value;

        const sc = this.selectedFormat.video.
            find(c => c.name === sel || c.encoders && c.encoders.encoder.find(e => e.name === sel));

        this.selectedEncoder = sc ?
            sc.encoders.encoder.length === 1 ? sc.encoders.encoder[0] : sc.encoders.encoder.find(e => e.name === sel)
            : this.selectedFormat.video[0].encoders.encoder[0];

        this.createParameters();
    }

    onQualityTypeChange() {
        const type = this.output.get("quality.type").value;

        if (type === "ABR") {
            this.output.get("quality.bitrate").enable();
            this.output.get("quality.rateFactor").disable();
            this.output.get("quality.scale").disable();
        } else if (type === "CQ") {
            this.output.get("quality.bitrate").disable();
            this.output.get("quality.scale").enable();
        } else if (type === "CRF") {
            this.output.get("quality.bitrate").disable();
            this.output.get("quality.rateFactor").enable();
        }

        if (!type) {
            this.output.get("quality.type").setValue(
                this.filterParameter(this.selectedEncoder.parameters, "crf") ? "CRF" : "CQ"
            );
        }
    }

}
