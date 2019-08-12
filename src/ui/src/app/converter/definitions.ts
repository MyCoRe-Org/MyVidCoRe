export interface Progress {
    percent?: number;
    elapsed?: string;
    estimate?: string;
}

export interface File {
    name: string;
    format: string;
    scale?: string;
}

export interface HWAccelNvidiaSpec {
    name: string;
    family?: string;
    chip?: string;
    numChips?: number;
    numEncoder?: number;
    numDecoder?: number;
}

export interface HWAccel {
    type: string;
    index: number;
    name: string;
    deviceSpec?: HWAccelNvidiaSpec;
}

export interface HWAccels {
    hwaccels: Array<HWAccel>;
}

export interface Job {
    id: string;
    file: string;
    inputPath?: string;
    outputStream?: string;
    errorStream?: string;
    command?: string;
    priority?: number;
    exitValue?: number;
    done: boolean;
    running: boolean;
    addTime?: Date;
    endTime?: Date;
    startTime?: Date;
    progress: Progress;
    files: Array<File>;
    hwAccel?: HWAccel;

    hashCode?: string;
}

export interface CodecEncoders {
    encoder: Array<string | any>;
}

export interface Codec {
    type: string;
    name: string;
    description?: string;
    encoders?: CodecEncoders;
    lossy?: boolean;
    lossless?: boolean;
}

export interface Codecs {
    codecs: Array<Codec>;
}

export interface Format {
    name: string;
    description?: string;
    demuxer?: boolean;
    muxer?: boolean;
}

export interface Formats {
    formats: Array<Format>;
}
