export const DEFAULT_FORMATS = {
    "avi": {
        "audio": ["aac", "mp3"],
        "video": ["h264", "mpeg4", "msmpeg4v2", "mpeg1video", "mpeg2video", "vp8"]
    },
    "mp4": {
        "audio": ["aac", "mp3"],
        "video": ["h264", "h265", "mpeg4", "msmpeg4v2", "mpeg1video", "mpeg2video"]
    },
    "matroska": {
        "audio": ["aac", "mp3", "vorbis", "opus", "flac"],
        "video": ["h264", "h265", "mpeg4", "msmpeg4v2", "mpeg2video", "vp8", "vp9", "theora"]
    },
    "webm": {
        "audio": ["vorbis", "opus"],
        "video": ["vp8", "vp9"]
    }
};

export const DEFAULT_BITRATES = [64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512];

export const DEFAULT_FRAMERATES = ["5", "10", "12", "15", "23.976", "24", "25", "29.97", "30", "50", "59.94", "60"];

export const DEFAULT_SCALES = [{
    "name": "2160p",
    "value": "-2:2160"
}, {
    "name": "1440p",
    "value": "-2:1440"
}, {
    "name": "1080p",
    "value": "-2:1080"
}, {
    "name": "720p",
    "value": "-2:720"
}, {
    "name": "540p",
    "value": "-2:540"
}, {
    "name": "480p",
    "value": "-2:480"
}, {
    "name": "360p",
    "value": "-2:360"
}];

export const DEFAULT_PROFILES = [{
    "name": "baseline"
}, {
    "name": "main"
}, {
    "name": "high"
}, {
    "name": "high10"
}, {
    "name": "high442"
}, {
    "name": "high444"
}];

export const DEFAULT_LEVELS = [{
    "name": "1.0"
}, {
    "name": "1b"
}, {
    "name": "1.0b"
}, {
    "name": "1.1"
}, {
    "name": "1.2"
}, {
    "name": "1.3"
}, {
    "name": "2"
}, {
    "name": "2.0"
}, {
    "name": "2.1"
}, {
    "name": "2.2"
}, {
    "name": "3"
}, {
    "name": "3.0"
}, {
    "name": "3.1"
}, {
    "name": "3.2"
}, {
    "name": "4"
}, {
    "name": "4.0"
}, {
    "name": "4.1"
}, {
    "name": "4.2"
}, {
    "name": "5"
}, {
    "name": "5.0"
}, {
    "name": "5.1"
}];

export const DEFAULT_PRESETS = {
    "libx264": [{
        "name": "ultrafast"
    }, {
        "name": "superfast"
    }, {
        "name": "veryfast"
    }, {
        "name": "faster"
    }, {
        "name": "fast"
    }, {
        "name": "medium"
    }, {
        "name": "slow"
    }, {
        "name": "slower"
    }, {
        "name": "veryslow"
    }, {
        "name": "placebo"
    }],
    "libx264rgb": [{
        "name": "ultrafast"
    }, {
        "name": "superfast"
    }, {
        "name": "veryfast"
    }, {
        "name": "faster"
    }, {
        "name": "fast"
    }, {
        "name": "medium"
    }, {
        "name": "slow"
    }, {
        "name": "slower"
    }, {
        "name": "veryslow"
    }, {
        "name": "placebo"
    }]
};

export const DEFAULT_TUNES = {
    "libx264": [{
        "name": "film"
    }, {
        "name": "animation"
    }, {
        "name": "grain"
    }, {
        "name": "stillimage"
    }, {
        "name": "psnr"
    }, {
        "name": "ssim"
    }, {
        "name": "zerolatency"
    }],
    "libx264rgb": [{
        "name": "film"
    }, {
        "name": "animation"
    }, {
        "name": "grain"
    }, {
        "name": "stillimage"
    }, {
        "name": "psnr"
    }, {
        "name": "ssim"
    }, {
        "name": "zerolatency"
    }]
};
