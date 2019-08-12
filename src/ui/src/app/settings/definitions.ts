import { Codec } from "../converter/definitions";

export interface Audio {
    codec: string;
    bitrate?: number;
    samplerate?: number;
    parameter?: (key: string) => string;
}

export interface VideoQuality {
    bitrate?: number;
    bufsize?: number;
    minrate?: number;
    maxrate?: number;
    rateFactor?: number;
    scale?: number;
    type?: string;
}

export interface Video {
    codec: string;
    forceKeyFrames?: number;
    framerate?: number;
    framerateType?: string;
    parameter?: (key: string) => string;
    pixelFormat?: string;
    quality?: VideoQuality;
    scale?: string;
    upscale?: boolean;
}

export interface Output {
    format: string;
    filenameAppendix?: string;
    audio: Audio;
    video: Video;
    "video-fallback"?: Video;
}

export interface HWAccel {
    index?: number;
    name: string;
    type?: string;
}

export interface Settings {
    hwaccels?: Array<HWAccel>;
    output: Array<Output>;
    plugins?: Array<any>;
}

export interface AllowedFormat {
    name: string;
    description: string;
    audio?: Array<Codec>;
    video?: Array<Codec>;
}
