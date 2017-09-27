module.exports = {
	"output" : [ {
		"format" : "mp4",
		"video" : {
			"codec" : "libx264",
			"framerateType" : "VFR",
			"profile" : "main",
			"level" : "4.0",
			"pixelFormat" : "yuv420p",
			"quality" : {
				"type" : "CRF",
				"rateFactor" : 23,
				"bitrate" : 2500
			}
		},
		"audio" : {
			"codec" : "libfdk_aac",
			"samplerate" : 44100,
			"bitrate" : 128
		}
	} ]
};