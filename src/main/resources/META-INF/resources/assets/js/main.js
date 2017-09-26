/*global angular */
/*global videojs */
"use strict";

var appName = "MyVidCoRe";
var app = angular.module(appName, [ "ngAnimate", "ngSanitize", "pascalprecht.translate" ]);

app.run(function($animate) {
	$animate.enabled(true);
});

app.config(function($translateProvider) {
	$translateProvider.useStaticFilesLoader({
		prefix : "/web/assets/i18n/i18n-",
		suffix : ".json"
	});

	$translateProvider.preferredLanguage("de_DE");
	$translateProvider.determinePreferredLanguage();
	$("html").attr("lang", $translateProvider.resolveClientLocale())
});

app.filter("endsWith", function() {
	return function(input, search, prop) {
		if (typeof input === "array" || typeof input === "object") {
			for (var i in input) {
				if (prop && input[i][prop].endsWith(search)|| input[i].endsWith(search)) {
					return true;
				}
			}
		} else if (typeof someVar === "string") {
			input = input || "";
			return input.endsWith(search);
		}
		
		return false;
	};
});

app.filter("translateWithFallBack", function($translate) {
	return function(key, prefix, fallBack, replaceRegExp) {
		fallBack = replaceRegExp !== undefined ? fallBack.replace(new RegExp(replaceRegExp, "g"), "").trim() : fallBack.trim();
		var text = $translate.instant(prefix + key);
		return text === prefix + key ? fallBack || key : text;
	};
});

app.filter("fileName", function() {
	return function(input) {
		input = input || "";
		return input.split(/\/|\\/).pop();
	};
});

app.filter("formatScale", function() {
	return function(input) {
		var scale = (input || "").split(":");
		return parseInt(scale[0]) < 0 ? scale[1] + "p" : scale[0] + "x" + scale[1];
	};
});

app.filter("parseNumber", function() {
	return function(input, type) {
		input = input || "";
		return type === "int" ? parseInt(input) : type === "float" ? parseFloat(input) : input;
	};
});

app.directive("convertToNumber", function() {
	return {
		require : "ngModel",
		link : function(scope, element, attrs, ngModel) {
			ngModel.$parsers.push(function(val) {
				return parseInt(val, 10);
			});
			ngModel.$formatters.push(function(val) {
				return val === undefined || val.length === 0 ? null : val;
			});
		}
	};
});

app.directive("slider", function() {
	return {
		require : "ngModel",
		link : function(scope, element, attrs, ngModel) {
			var slider;
			if (attrs.slider.length === 0) {
				var options = {};
				var data = $(element).data();

				angular.forEach(data, function(value, key) {
					if (key.indexOf("slider") === 0) {
						var k = key.replace("slider", "");
						k = k.substring(0, 1).toLowerCase() + k.substring(1);
						options[k] = scope.$eval(value);
					}
				});

				slider = $(element).slider(options);
			} else {
				scope.$watch(function(scope) {
					var options = scope.$eval(attrs.slider);
					if (options !== false && options.length > 2) {
						slider = $(element).slider(JSON.parse(options));
					}
				});
			}

			if (slider !== undefined) {
				slider.on("slide", function() {
					ngModel = $(this).val();
				});
			}
		}
	};
});

app.service("asyncQueue", function($http, $q) {
	this.load = function(urls) {
		var deferred = $q.defer();

		var queue = [];
		angular.forEach(urls, function(url) {
			queue.push($http.get(url));
		});

		$q.all(queue).then(function(results) {
			deferred.resolve(results);
		}, function(errors) {
			deferred.reject(errors);
		}, function(updates) {
			deferred.update(updates);
		});
		return deferred.promise;
	};
});

app.controller("directoryWatcherStatus", function($scope, $http, $interval) {
	var refresh;
	$scope.status = {};

	$scope.loadData = function() {
		$http({
			method : "GET",
			url : "/widget/directoryWatcher/status"
		}).then(function(response) {
			if (response.status === 200) {
				angular.merge($scope.status, response.data);
			}
		}, function() {
			$scope.status = {};
		});
	};

	$scope.$on("$destroy", function() {
		if (refresh) {
			$interval.cancel(refresh);
		}
	});

	$scope.loadData();

	refresh = $interval(function() {
		$scope.loadData();
	}, 60000);
});

app.controller("converterStatus", function($scope, $http, $interval, asyncQueue) {
	var refresh;
	var removeTimeout = 30000;
	$scope.converters = {
		"active" : {},
		"done" : {}
	};
	$scope.details = [];

	$scope.loadData = function() {
		var urls = [];
		for ( var type in $scope.converters) {
			if ({}.hasOwnProperty.call($scope.converters, type)) {
				var c = $scope.converters[type];
				var start = c.start || 0;
				var limit = c.limit || (type === "active" ? 50 : 10);
				urls.push("/widget/converter/" + (start / limit + 1) + "/" + limit + (type === "done" ? "/!isdone" : "") + "/status");
			}
		}

		asyncQueue.load(urls).then(function(results) {
			results.forEach(function(result) {
				if (result.status === 200) {
					var type = result.config.url.indexOf("!isdone") !== -1 ? "done" : "active";
					if ($scope.converters[type].length !== 0) {
						$scope.converters[type] = result.data;
					} else {
						angular.merge($scope.converters[type], result.data);
					}

				}
			});
		}, function(error) {
			console.error(error);
			$scope.converters = {
				"active" : {},
				"done" : {}
			};
		});
	};

	$scope.loadDetailData = function(id) {
		var converter = $scope.details[id] || {};
		if (converter.outputStream === undefined && converter.errorStream === undefined) {
			$http({
				method : "GET",
				url : "/widget/converter/" + id + "/status"
			}).then(function(response) {
				if (response.status === 200) {
					$scope.details[id] = response.data;
				}
			});
		}
	};

	$scope.orderByPercent = function(converter) {
		return converter.progress ? converter.progress.percent : -1;
	};

	$scope.filterDone = function(converter) {
		var diff = new Date() - new Date(converter.endTime);
		return converter.running || !converter.running && !converter.done ? true : converter.done && diff < removeTimeout ? true : false;
	};

	$scope.pagination = function(pagination) {
		var maxPages = 5;
		var total = Math.floor(pagination.total / pagination.limit) + (pagination.total % pagination.limit > 0 ? 1 : 0);
		var page = pagination.start / pagination.limit + 1;
		if (total > 1) {
			var pS = (page - (maxPages - 1) / 2) - 1;
			if (pS < 0) {
				pS = 0;
			}
			var pE = pS + maxPages;
			if (pE >= total) {
				pE = total;
				pS = pE - (maxPages > total ? total : maxPages);
			}
			pagination = [];
			for (var p = pS; p < pE; p++) {
				pagination.push(p + 1);
			}
			return pagination;
		}

		return [];
	};

	$scope.paginationPage = function(pagination, page) {
		if ($scope.paginationDisabled(pagination, page)) {
			return;
		}

		if (typeof page === "string") {
			page = page[0] === "-" ? (pagination.start / pagination.limit) + 1 - parseInt(page.substring(1))
					: page[0] === "+" ? page = (pagination.start / pagination.limit) + 1 + parseInt(page.substring(1)) : 0;
		}
		var start = (page - 1) * pagination.limit;
		pagination.start = start;
		$scope.loadData();
	};

	$scope.paginationActive = function(pagination, page) {
		return pagination.start === (page - 1) * pagination.limit;
	};

	$scope.paginationDisabled = function(pagination, page) {
		if (typeof page === "string") {
			page = page[0] === "-" ? (pagination.start / pagination.limit) + 1 - parseInt(page.substring(1))
					: page[0] === "+" ? page = (pagination.start / pagination.limit) + 1 + parseInt(page.substring(1)) : 0;
		}
		var total = Math.floor(pagination.total / pagination.limit) + (pagination.total % pagination.limit > 0 ? 1 : 0);
		return page <= 0 || page > total;
	};

	$scope.formatStream = function(stream) {
		if (stream !== undefined) {
			return stream.replace(/\r/g, "\n");
		}
	};
	
	$scope.initPreview = function(id, files) {
		videojs("video-" + id).videoJsResolutionSwitcher({
			default: "high",
			dynamicLabel: true
		});
	};

	$scope.$on("$destroy", function() {
		if (refresh) {
			$interval.cancel(refresh);
		}
	});

	$scope.loadData();

	refresh = $interval(function() {
		$scope.loadData();
	}, 3000);
});

// provides some helper for converter format settings
app.service("formatService", function($http, $q, asyncQueue) {
	this.supportedEncoders = {};

	function buildFormatURLs(formats) {
		var urls = [];
		angular.forEach(formats, function(options, format) {
			urls.push("/converter/formats/name/" + format);
			angular.forEach(options, function(codecs) {
				angular.forEach(codecs, function(codec) {
					var url = "/converter/codecs/name/" + codec;
					if ($.inArray(url, urls) === -1) {
						urls.push(url);
					}
				});
			});
		});
		return urls;
	}

	function buildEncoderURLs(codecs) {
		var urls = [];
		angular.forEach(codecs, function(cs) {
			angular.forEach(cs, function(c) {
				if (c.encoders !== undefined && c.encoders.length !== 0) {
					angular.forEach(c.encoders.encoder, function(e) {
						var url = "/converter/encoder/" + e;
						if ($.inArray(url, urls) === -1) {
							urls.push(url);
						}
					});
				} else {
					var url = "/converter/encoder/" + c.name;
					if ($.inArray(url, urls) === -1) {
						urls.push(url);
					}
				}
			});
		});
		return urls;
	}

	function getByProperty(array, prop, value) {
		if (array !== undefined) {
			for (var i = 0; i < array.length; i++) {
				if (array[i] !== undefined && array[i][prop] !== undefined && array[i][prop] === value) {
					return array[i];
				}
			}
		}
		return null;
	}

	this.getByProperty = function(array, prop, value) {
		return getByProperty(array, prop, value);
	};

	this.getSupportedEncoders = function() {
		return this.supportedEncoders;
	};

	this.getSupportedFormats = function(formats) {
		var that = this;
		var deferred = $q.defer();

		asyncQueue.load(buildFormatURLs(formats)).then(function(results) {
			var fs = [];
			var cc = {
				audio : [],
				video : []
			};
			var ec = [];

			// load formats and codecs
			angular.forEach(results, function(result) {
				var json = result.data;
				if (json.formats !== undefined && json.formats.length !== 0) {
					fs.push(json.formats[0]);
				} else if (json.codecs !== undefined && json.codecs.length !== 0) {
					var codec = json.codecs[0];
					cc[codec.type.toLowerCase()].push(codec);
				}
			});

			// load encoder infos for codecs
			asyncQueue.load(buildEncoderURLs(cc)).then(function(results) {
				angular.forEach(results, function(result) {
					var json = result.data;
					ec = ec.concat(json.encoders);
				});

				// cache it
				that.supportedEncoders = ec;

				angular.forEach(cc, function(codecs) {
					angular.forEach(codecs, function(codec) {
						if (codec.encoders !== undefined) {
							var encoders = [];
							angular.forEach(codec.encoders.encoder, function(e) {
								encoders.push(getByProperty(ec, "name", e));
							});
							codec.encoders = encoders;
						} else {
							codec.encoders = [];
							codec.encoders.push(getByProperty(ec, "name", codec.name));
						}
					});
				});

				angular.forEach(formats, function(options, format) {
					var f = getByProperty(fs, "name", format);
					if (f !== null) {
						formats[format].description = f.description;
						angular.forEach(options, function(codecs, type) {
							if (Array.isArray(formats[format][type])) {
								var re = [];
								angular.forEach(codecs, function(codec, index) {
									var c = angular.copy(getByProperty(cc[type], "name", codec));
									if (c !== null) {
										formats[format][type][index] = c;
									} else {
										re.push(index);
									}
								});

								for (var i = 0; i < re.length; i++) {
									formats[format][type].splice(re[i], 1);
								}
							}
						});
					} else {
						delete formats[format];
					}
				});
			});

			deferred.resolve(formats);
		});

		return deferred.promise;
	};
});

app.controller("settings", function($scope, $http, $translate, $log, $timeout, formatService) {
	var removeTimeout = 30000;
	$scope.status = {};
	$scope.isLoading = true;

	// configured formats
	$scope.converterFormats = {
		"avi" : {
			"audio" : [ "aac", "mp3" ],
			"video" : [ "h264", "mpeg4", "msmpeg4v2", "mpeg1video", "mpeg2video", "vp8" ]
		},
		"mp4" : {
			"audio" : [ "aac", "mp3" ],
			"video" : [ "h264", "h265", "mpeg4", "msmpeg4v2", "mpeg1video", "mpeg2video" ]
		},
		"matroska" : {
			"audio" : [ "aac", "mp3", "vorbis", "opus", "flac" ],
			"video" : [ "h264", "h265", "mpeg4", "msmpeg4v2", "mpeg2video", "vp8", "vp9", "theora" ]
		},
		"webm" : {
			"audio" : [ "vorbis", "opus" ],
			"video" : [ "vp8", "vp9" ]
		}
	};

	$scope.defaultPresets = {
		"libx264" : [ {
			"name" : "ultrafast"
		}, {
			"name" : "superfast"
		}, {
			"name" : "veryfast"
		}, {
			"name" : "faster"
		}, {
			"name" : "fast"
		}, {
			"name" : "medium"
		}, {
			"name" : "slow"
		}, {
			"name" : "slower"
		}, {
			"name" : "veryslow"
		}, {
			"name" : "placebo"
		} ]
	};

	$scope.defaultTunes = {
		"libx264" : [ {
			"name" : "film"
		}, {
			"name" : "animation"
		}, {
			"name" : "grain"
		}, {
			"name" : "stillimage"
		}, {
			"name" : "psnr"
		}, {
			"name" : "ssim"
		}, {
			"name" : "zerolatency"
		} ]
	};

	$scope.defaultScales = [ {
		"name" : "2160p",
		"value" : "-2:2160"
	}, {
		"name" : "1440p",
		"value" : "-2:1440"
	}, {
		"name" : "1080p",
		"value" : "-2:1080"
	}, {
		"name" : "720p",
		"value" : "-2:720"
	}, {
		"name" : "540p",
		"value" : "-2:540"
	}, {
		"name" : "480p",
		"value" : "-2:480"
	}, {
		"name" : "360p",
		"value" : "-2:360"
	} ];

	$scope.defaultFrameRates = [ "5", "10", "12", "15", "23.976", "24", "25", "29.97", "30", "50", "59.94", "60" ];

	$scope.defaultProfiles = [ {
		"name" : "baseline"
	}, {
		"name" : "main"
	}, {
		"name" : "high"
	}, {
		"name" : "high10"
	}, {
		"name" : "high442"
	}, {
		"name" : "high444"
	} ];

	$scope.defaultLevels = [ {
		"name" : "1.0"
	}, {
		"name" : "1b"
	}, {
		"name" : "1.0b"
	}, {
		"name" : "1.1"
	}, {
		"name" : "1.2"
	}, {
		"name" : "1.3"
	}, {
		"name" : "2"
	}, {
		"name" : "2.0"
	}, {
		"name" : "2.1"
	}, {
		"name" : "2.2"
	}, {
		"name" : "3"
	}, {
		"name" : "3.0"
	}, {
		"name" : "3.1"
	}, {
		"name" : "3.2"
	}, {
		"name" : "4"
	}, {
		"name" : "4.0"
	}, {
		"name" : "4.1"
	}, {
		"name" : "4.2"
	}, {
		"name" : "5"
	}, {
		"name" : "5.0"
	}, {
		"name" : "5.1"
	} ];
	
	$scope.defaultBitrates = [ 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512 ];

	// default settings
	$scope.settings = {
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

	$scope.formats = {};
	$scope.selectedCodec = {};
	$scope.hwaccels = [];

	$scope.supportedFormats = function() {
		$scope.isLoading = true;
		var formats = {};
		angular.copy($scope.converterFormats, formats);

		formatService.getSupportedFormats(formats).then(function(formats) {
			$scope.formats = formats;
			$scope.isLoading = false;
		}, function(error) {
			$log.error("failure loading formats", error);
		});
	};
	
	$scope.initHWAccelsSettings = function() {
		var hwaccels = [];
		for (var i in $scope.hwaccels) {
			var hwaccel = $scope.hwaccels[i];
			for (var j in $scope.settings.hwaccels) {
				var shw = $scope.settings.hwaccels[j];
				if (shw !== undefined && shw !== null && shw.type === hwaccel.type && shw.index === hwaccel.index) {
					break;
				}	
				hwaccel = null;
			}
			hwaccels[i] = hwaccel;
		}
		$scope.settings.hwaccels = hwaccels;
	};
	
	$scope.checkedHWAccel = function(i, hwaccel) {
		var shw = $scope.settings.hwaccels[i];
		return shw !== undefined && shw !== null && shw.type === hwaccel.type && shw.index === hwaccel.index;
	};
	
	$scope.detectedHWAccels = function() {
		$scope.hwaccels = [];

		$http.get("/converter/hwaccels").then(function(result) {
			$scope.hwaccels = result.data.hwaccels || [];
		}, function(error) {
			$log.error("failure loading hwaccels", error);
		});
	};

	$scope.load = function() {
		$http.get("/settings").then(function(response) {
			if (response.status === 200) {
				if (response.data.output.length > 0) {
					$scope.settings = response.data;
				}
			}
		}, function(error) {
			$log.error("failure loading settings", error);
		});
	};

	$scope.save = function(settings) {
		$http.post("/settings", settings).then(function() {
			$scope.showStatusMessage("success", $translate.instant("settings.saved.success"));
		}, function() {
			$scope.showStatusMessage("error", $translate.instant("settings.saved.error"));
		});
	};

	$scope.filterCodecs = function(format, type) {
		return format !== undefined ? $scope.formats[format][type] : null;
	};

	$scope.filterEncoder = function(name) {
		var encoders = formatService.getSupportedEncoders();
		return encoders !== undefined && encoders.length !== 0 && formatService.getByProperty(encoders, "name", name);
	};

	$scope.encoderParameter = function(obj, param) {
		var encoder = typeof obj === "string" ? $scope.filterEncoder(obj) : obj;
		if (encoder === undefined || encoder === null || encoder.length === 0) {
			return false;
		}
		if (encoder.parameters === undefined || encoder.parameters.length === 0) {
			return false;
		}

		return formatService.getByProperty(encoder.parameters, "name", param);
	};

	$scope.qualityData = function(obj, param, value) {
		var encoder = typeof obj === "string" ? $scope.filterEncoder(obj) : obj;
		if (encoder === undefined || encoder === null || encoder.length === 0) {
			return false;
		}

		var data = {};
		if (param === "qscale") {
			data = {
				id : "qualitySlider",
				min : 0,
				max : 31,
				step : 1,
				value : value !== undefined && value !== null ? parseInt(value) : 14,
				reversed : true
			};
		} else if (encoder.name === "libx264") {
			data = {
				id : "qualitySlider",
				min : 0,
				max : 51,
				step : 0.25,
				precision : 2,
				value : value !== undefined && value !== null ? parseInt(value) : 23,
				reversed : true
			};
		} else {
			if (encoder.parameters === undefined || encoder.parameters.length === 0) {
				return false;
			}
			var p = formatService.getByProperty(encoder.parameters, "name", param);
			if (p !== undefined && p !== null) {
				data = {
					id : "qualitySlider",
					min : p.type === "float" ? parseFloat(p.fromValue) : parseInt(p.fromValue),
					max : p.type === "float" ? parseFloat(p.toValue) : parseInt(p.toValue),
					step : p.type === "float" ? 0.25 : 1,
					precision : p.type === "float" ? 2 : 0,
					value : value !== undefined && value !== null ? p.type === "float" ? parseFloat(value) : parseInt(value) : Math
							.floor((p.toValue - p.fromValue) / 2),
					reversed : true
				};
			}
		}

		return JSON.stringify(data);
	};

	$scope.getLength = function(obj) {
		return Object.keys(obj).length;
	};

	$scope.changeCodec = function(index, type) {
		if ($scope.settings.output[index] === undefined || $scope.settings.output[index][type] === undefined) {
			return;
		}

		var codec = $scope.settings.output[index][type].codec;

		$scope.selectedCodec[index][type] = $scope.filterEncoder(codec);
		$scope.settings.output[index][type] = {
			"codec" : codec
		};
	};

	$scope.addOutput = function() {
		$scope.settings.output.push({});
	};

	$scope.removeOutput = function(index) {
		$scope.settings.output.splice(index, 1);
	};

	$scope.showStatusMessage = function(type, msg) {
		var style = "primary";

		switch (type) {
		case "info":
			style = "info";
			break;
		case "success":
			style = "success";
			break;
		case "error":
			style = "danger";
			break;
		case "warning":
			style = "warning";
			break;
		}

		$scope.status = {
			style : style,
			msg : msg
		};

		$timeout(function() {
			var $elm = $("#status-message");
			$elm.animate({
				opacity : 0,
				height : 0
			}, "slow", function() {
				$scope.status = {};
				$elm.remove();
			});
		}, removeTimeout);
	};

	// init
	$scope.load();
	$scope.detectedHWAccels();
	$scope.supportedFormats();
});