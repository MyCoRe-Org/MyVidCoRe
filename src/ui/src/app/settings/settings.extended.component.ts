import { Component, OnInit, Input, OnChanges, SimpleChanges } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup, ControlContainer, FormGroupDirective, Validators } from "@angular/forms";

import { Settings } from "./definitions";

@Component({
    selector: "ui-settings-extended",
    templateUrl: "./settings.extended.component.html",
    viewProviders: [
        { provide: ControlContainer, useExisting: FormGroupDirective }
    ]
})
export class SettingsExtendedComponent implements OnInit, OnChanges {

    @Input()
    index: number;

    @Input()
    type: string;

    @Input()
    settings: Settings;

    @Input()
    selectedEncoder: any;

    parameter: UntypedFormGroup;

    static createParameter($fb: UntypedFormBuilder, param: any) {
        if (param.type === "int") {
            const vals = [];

            if (!param.values) {
                if (param.fromValue) {
                    vals.push(Validators.min(parseInt(param.fromValue, 10)));
                }
                if (param.toValue) {
                    vals.push(Validators.max(parseInt(param.toValue, 10)));
                }
            }

            return $fb.control(param.defaultValue, vals);
        } else if (param.type === "float") {
            const vals = [];

            if (!param.values) {
                if (param.fromValue) {
                    vals.push(Validators.min(parseFloat(param.fromValue)));
                }
                if (param.toValue) {
                    vals.push(Validators.max(parseFloat(param.toValue)));
                }
            }

            return $fb.control(param.defaultValue, vals);
        } else if (param.type === "string") {
            return $fb.control(param.defaultValue);
        } else if (param.type === "boolean") {
            return $fb.control(param.defaultValue === "auto" ? param.defaultValue : param.defaultValue === "true");
        }

        return $fb.control(null);
    }

    static isInValues(param: any, value: any) {
        if (param.values) {
            return param.values.findIndex(p => p.name === value) !== -1;
        }

        return false;
    }

    static parseValue(param: any, value: any) {
        if (param.type === "int") {
            return "default" === value && "default" === param.defaultValue || param.defaultValue === value
                || SettingsExtendedComponent.isInValues(param, value) ? value : parseInt(value, 10) || value;
        } else if (param.type === "float") {
            return "default" === value && "default" === param.defaultValue || param.defaultValue === value
                || SettingsExtendedComponent.isInValues(param, value) ? value : parseFloat(value) || value;
        } else if (param.type === "boolean") {
            return "auto" === value ? value : value === true || value === "true";
        }

        return value;
    }

    static patchValues(fg: UntypedFormGroup, encParams: Array<(key: string) => any>, params: (key: string) => string) {
        encParams.forEach((p: any) => {
            if (params && params[p.name] && fg.contains(p.name)) {
                fg.get(p.name).patchValue(
                    SettingsExtendedComponent.parseValue(p, params[p.name] || p.defaultValue)
                );
            }
        });
    }

    static cleanSettings(encParams: Array<(key: string) => any>, params: (key: string) => string) {
        const res = <(key: string) => string>{};

        if (params) {
            Object.keys(params).forEach(name => {
                if (params[name] !== undefined && params[name] !== null) {
                    const ep: any = encParams.find((p: any) => p.name === name);
                    if (ep) {
                        const value = SettingsExtendedComponent.parseValue(ep, params[name]);
                        if (ep.defaultValue) {
                            const dv = SettingsExtendedComponent.parseValue(ep, ep.defaultValue);
                            if (dv !== value) {
                                res[name] = value;
                            }
                        } else {
                            res[name] = value;
                        }
                    }
                }
            });
        }

        return res;
    }

    constructor(private parent: FormGroupDirective, public $fb: UntypedFormBuilder) {
    }

    ngOnInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.selectedEncoder.previousValue !== changes.selectedEncoder.currentValue) {
            this.selectedEncoder = changes.selectedEncoder.currentValue;
            this.createParameters();
        }
    }

    createParameters() {
        const params = <UntypedFormGroup>this.parent.form.get("parameter");
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
                params,
                this.selectedEncoder.parameters,
                this.settings && this.settings.output[this.index] && this.settings.output[this.index][this.type].parameter
            );
        }

        this.parameter = <UntypedFormGroup>this.parent.form.get("parameter");
    }
}
