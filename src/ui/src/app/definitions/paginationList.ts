export interface PaginationList<T> {
    term?: string;
    start: number;
    limit: number;
    total?: number;
    items?: Array<T>;
}
