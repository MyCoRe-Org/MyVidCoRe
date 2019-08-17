export function hashCode(input: string): string {
    input = input || "";
    let hash = 0;
    for (let i = 0; i < input.length; i++) {
        const char = input.charCodeAt(i);
        // tslint:disable-next-line: no-bitwise
        hash = ((hash << 5) - hash) + char;
        // tslint:disable-next-line: no-bitwise
        hash = hash & hash;
    }

    if (hash < 0) {
        hash = hash * -1;
    }

    return hash.toString(16);
}
