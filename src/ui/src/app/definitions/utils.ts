export function hashCode(input: string): string {
    input = input || "";
    let hash = 0;
    for (let i = 0; i < input.length; i++) {
        const char = input.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash;
    }

    if (hash < 0) {
        hash = hash * -1;
    }

    return hash.toString(16);
}
