import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "callbackFilter",
    pure: false,
    standalone: false
})
export class CallbackFilterPipe implements PipeTransform {
    transform(items: any[], callback: (item: any) => boolean): any {
        if (!items || !callback) {
            return items;
        }
        return items.filter(item => callback(item));

    }
}
