import { Pipe, PipeTransform } from "@angular/core";

import { hashCode } from "../definitions";

@Pipe({
    name: "hashCode",
    pure: false,
    standalone: false
})
export class HashCodePipe implements PipeTransform {

    transform(value: string): string {
        return hashCode(value);
    }

}
