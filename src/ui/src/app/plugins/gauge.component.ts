import { Component, Input } from "@angular/core";

import { Attrib } from "./definitions";

@Component({
    selector: "ui-gauge",
    templateUrl: "./gauge.component.html"
})
export class GaugeComponent {

    @Input()
    i18nPrefix: string;

    @Input()
    index: number;

    @Input()
    name: string;

    @Input()
    attrib: Attrib;

    @Input()
    max = 100;

    @Input()
    startAngle = 145;

    @Input()
    endAngle = 45;

    @Input()
    class: string;

    constructor() { }

    gaugeLabel(name: string, attrib: Attrib) {
        if (attrib) {
            if (["C", "F"].indexOf(attrib.unit) !== -1) {
                return (value: number): string => {
                    return `${Math.round(value)} °` + attrib.unit;
                };
            } else if (["B"].indexOf(attrib.unit) !== -1 || "%" !== attrib.unit && ["memory", "swap"].indexOf(name) !== -1) {
                return (value: number): string => {
                    if (value >= (1024 * 1024 * 1024)) {
                        return `${Math.round(value / (1024 * 1024 * 1024))} G`;
                    } else if (value >= (1024 * 1024)) {
                        return `${Math.round(value / (1024 * 1024))} M`;
                    }

                    return `${Math.round(value / 1024)} K`;
                };
            } else {
                return (value: number): string => {
                    return `${Math.round(value)} ` + attrib.unit;
                };
            }
        }

        return null;
    }

}
