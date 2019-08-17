import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
    name: "limit",
    pure: false
})
export class LimitPipe implements PipeTransform {

    transform(items: any[], limit: number): any {
        if (!items || !limit) {
            return [];
        }

        return items.slice(0, limit);
    }

}
