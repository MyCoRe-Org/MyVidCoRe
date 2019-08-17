import { Pipe, PipeTransform } from "@angular/core";
@Pipe({
    name: "orderBy"
})

export class OrderByPipe implements PipeTransform {
    transform(array: Array<any>, orderField: string, orderType: boolean): Array<string> {
        if (array) {
            array.sort((a: any, b: any) => {
                const ae = a[orderField];
                const be = b[orderField];
                if (ae === undefined && be === undefined) { return 0; }
                if (ae === undefined && be !== undefined) { return orderType ? 1 : -1; }
                if (ae !== undefined && be === undefined) { return orderType ? -1 : 1; }
                if (ae === be) { return 0; }

                if (typeof ae === "number" && typeof ae === typeof be) {
                    return orderType ? ae > be ? -1 : 1 : be > ae ? -1 : 1;
                } else {
                    return orderType ?
                        (ae.toString().toLowerCase() > be.toString().toLowerCase() ? -1 : 1) :
                        (be.toString().toLowerCase() > ae.toString().toLowerCase() ? -1 : 1);
                }
            });
        }
        return array;
    }
}
