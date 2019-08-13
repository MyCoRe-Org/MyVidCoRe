import { Component, OnInit, Input, OnChanges, SimpleChanges } from "@angular/core";
import { FormBuilder, FormGroup, Validators, ControlContainer, FormGroupDirective } from "@angular/forms";

import { Settings, Audio, AllowedFormat } from "./definitions";
import {
    DEFAULT_BITRATES
} from "./defaults";

@Component({
    selector: "ui-settings-audio",
    templateUrl: "./settings.audio.component.html",
    viewProviders: [
        { provide: ControlContainer, useExisting: FormGroupDirective }
    ]
})
export class SettingsAudioComponent implements OnInit, OnChanges {

    @Input()
    index: number;

    @Input()
    type: string;

    @Input()
    settings: Settings;

    @Input()
    selectedFormat: AllowedFormat;

    selectedEncoder: any;

    audioBitrates = DEFAULT_BITRATES;

    output: FormGroup;

    static createAudio($fb: FormBuilder, audio: Audio = null): FormGroup {
        const fg = $fb.group({
            codec: [null, [Validators.required]],
            bitrate: [],
            samplerate: [],
            parameter: $fb.group({})
        });

        if (audio) {
            fg.patchValue(audio);
        }

        return fg;
    }

    constructor(private parent: FormGroupDirective, public $fb: FormBuilder) {
    }

    ngOnInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.selectedFormat.previousValue !== changes.selectedFormat.currentValue) {
            this.selectedFormat = changes.selectedFormat.currentValue;

            const form = <FormGroup>this.parent.form.get("output." + this.index);

            if (form.contains(this.type)) {
                const output = this.settings && this.settings.output[this.index] && this.settings.output[this.index][this.type];

                form.addControl(
                    this.type,
                    SettingsAudioComponent.createAudio(this.$fb, output)
                );
            }

            this.output = <FormGroup>this.parent.form.get("output." + this.index + "." + this.type);
            this.onCodecChange();
        }
    }

    filterParameter(params: Array<any>, name: string) {
        return params.find(p => p.name === name);
    }

    onCodecChange() {
        if (!this.selectedEncoder) {
            this.selectedEncoder = <(key: string) => any>{};
        }
        const sel = this.output.get("codec").value;
        const sc = this.selectedFormat.audio.
            find(c => c.name === sel || c.encoders && c.encoders.encoder.find(e => e.name === sel));

        this.selectedEncoder = sc ?
            sc.encoders.encoder.length === 1 ? sc.encoders.encoder[0] : sc.encoders.encoder.find(e => e.name === sel)
            : this.selectedFormat.audio[0].encoders.encoder[0];
    }

}
