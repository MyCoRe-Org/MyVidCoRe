import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";

import { CallbackFilterPipe } from "./callbackFilter.pipe";
import { HashCodePipe } from "./hashCode.pipe";
import { LimitPipe } from "./limit.pipe";
import { OrderByPipe } from "./orderBy.pipe";

@NgModule({
    imports: [
        CommonModule
    ],
    declarations: [
        CallbackFilterPipe,
        HashCodePipe,
        LimitPipe,
        OrderByPipe
    ],
    exports: [
        CallbackFilterPipe,
        HashCodePipe,
        LimitPipe,
        OrderByPipe
    ]
})
export class PipesModule { }
