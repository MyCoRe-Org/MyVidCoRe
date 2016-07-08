/*global angular */
"use strict";

var appName = "MyVidCoRe";
var app = angular.module(appName, [ "ngSanitize", "pascalprecht.translate" ]);

app.run(function($animate) {
	$animate.enabled(true);
});

app.config(function($translateProvider) {
	$translateProvider.useStaticFilesLoader({
		prefix : "/web/assets/i18n/i18n-",
		suffix : ".json"
	});

	$translateProvider.preferredLanguage("de_DE");
});

app.directive("convertToNumber", function() {
	return {
		require : 'ngModel',
		link : function(scope, element, attrs, ngModel) {
			ngModel.$parsers.push(function(val) {
				console.log(val);
				return parseInt(val, 10);
			});
			ngModel.$formatters.push(function(val) {
				return val === undefined || val.length == 0 ? null : '' + val;
			});
		}
	};
});

app.directive("slider", function($parse) {
	return {
		require : 'ngModel',
		link : function(scope, element, attrs, ngModel) {
			var slider;
			if (attrs.slider.length == 0) {
				var options = {};
				var data = $(element).data();

				angular.forEach(data, function(value, key) {
					if (key.indexOf("slider") == 0) {
						var k = key.replace("slider", "");
						k = k.substring(0, 1).toLowerCase() + k.substring(1);
						options[k] = value;
					}
				});

				slider = $(element).slider(options);
			} else {
				scope.$watch(function(scope) {
					var options = scope.$eval(attrs.slider);
					if (options != false && options.length > 2) {
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
	}
});

app.controller("directoryWatcherStatus", function($scope, $http, $interval) {
	var refresh;
	$scope.status = {};

	$scope.loadData = function() {
		$http({
			method : "GET",
			url : "/widget/directoryWatcher/status"
		}).then(function(response) {
			if (response.status = 200)
				angular.merge($scope.status, response.data);
		});
	}

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

app.controller("converterStatus", function($scope, $http, $interval, $timeout) {
	var refresh;
	var removeTimeout = 30000;
	$scope.converters = [];

	$scope.getConverterById = function(id) {
		for (var i = 0; i < $scope.converters.length; i++) {
			if ($scope.converters[i].id == id) {
				return $scope.converters[i];
			}
		}

		return undefined;
	}

	$scope.loadData = function() {
		$http({
			method : "GET",
			url : "/widget/converter/status"
		}).then(function(response) {
			if (response.status = 200) {
				var converters = [];
				for (var i = 0; i < response.data.converter.length; i++) {
					var converter = response.data.converter[i];
					if (!converter.done) {
						converters.push(converter);
						continue;
					}
					var diff = new Date() - new Date(converter.endTime);
					if (diff < removeTimeout || $scope.getConverterById(converter.id) !== undefined) {
						converters.push(converter);
					}
				}

				angular.merge($scope.converters, converters);

				angular.forEach($scope.converters, function(converter, index) {
					if (converter.done) {
						var $elm = $("#converter-" + converter.id);
						$timeout(function() {
							$elm.animate({
								opacity : 0,
								height : 0
							}, "slow", function() {
								var id = $elm.attr("id").replace("converter-", "");
								for (var i = 0; i < $scope.converters.length; i++) {
									if ($scope.converters[i].id == id) {
										$scope.converters.splice(i, 1);
										break;
									}
								}
								$elm.remove();
							});
						}, removeTimeout);
					}
				});
			}
		});
	}

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
			urls.push("/settings/formats/name/" + format);
			angular.forEach(options, function(codecs, type) {
				angular.forEach(codecs, function(codec) {
					var url = "/settings/codecs/name/" + codec;
					if ($.inArray(url, urls) == -1)
						urls.push(url);
				});
			});
		});
		return urls;
	}

	function buildEncoderURLs(codecs) {
		var urls = [];
		angular.forEach(codecs, function(cs, type) {
			angular.forEach(cs, function(c) {
				if (c.encoders !== undefined && c.encoders.length != 0) {
					angular.forEach(c.encoders.encoder, function(e) {
						var url = "/settings/encoder/" + e;
						if ($.inArray(url, urls) == -1)
							urls.push(url);
					});
				} else {
					var url = "/settings/encoder/" + c.name;
					if ($.inArray(url, urls) == -1)
						urls.push(url);
				}
			});
		});
		return urls;
	}

	function getByProperty(array, prop, value) {
		if (array !== undefined) {
			for (var i = 0; i < array.length; i++) {
				if (array[i][prop] !== undefined && array[i][prop] == value) {
					return array[i];
				}
			}
		}
		return null;
	}

	this.getByProperty = function(array, prop, value) {
		return getByProperty(array, prop, value);
	}

	this.getSupportedEncoders = function() {
		return this.supportedEncoders;
	}

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
				if (json.formats !== undefined && json.formats.length != 0) {
					fs.push(json.formats[0]);
				} else if (json.codecs !== undefined && json.codecs.length != 0) {
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

				angular.forEach(cc, function(codecs, type) {
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
					if (f != null) {
						formats[format]["description"] = f.description;
						angular.forEach(options, function(codecs, type) {
							if (Array.isArray(formats[format][type])) {
								var re = [];
								angular.forEach(codecs, function(codec, index) {
									var c = angular.copy(getByProperty(cc[type], "name", codec));
									if (c != null) {
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
		});

		deferred.resolve(formats);

		return deferred.promise;
	}
});

app.controller("settings", function($scope, $http, $translate, $log, $timeout, formatService) {
	var removeTimeout = 30000;
	$scope.status = {};

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

	$scope.defaultFrameRates = [ "5", "10", "12", "15", "23.976", "24", "25", "29.97", "30", "50", "59.94", "60" ];

	$scope.defaultProfiles = [ {
		"name" : "baseline"
	}, {
		"name" : "main"
	}, {
		"name" : "high"
	} ];

	$scope.defaultLevels = [ {
		"name" : "3.0"
	}, {
		"name" : "3.1"
	}, {
		"name" : "4.0"
	}, {
		"name" : "4.1"
	}, {
		"name" : "4.2"
	} ];

	// default settings
	$scope.settings = {
		"format" : "mp4",
		"video" : {
			"codec" : "libx264",
			"framerateType" : "VFR",
			"profile" : "main",
			"level" : "4.0",
			"pixelFormat" : "yuv420p",
			"profile" : "default",
			"quality" : {
				"type" : "CRF",
				"rateFactor" : 23,
				"bitrate" : 2500
			}
		},
		"audio" : {
			"codec" : "libfdk_aac",
			"samplerate" : "auto",
			"bitrate" : "auto"
		}
	};

	$scope.formats = {};
	$scope.selectedCodec = {};

	$scope.supportedFormats = function() {
		var formats = {};
		angular.copy($scope.converterFormats, formats);

		formatService.getSupportedFormats(formats).then(function(formats) {
			$scope.formats = formats;
		}, function(error) {
			$log.error("failure loading formats", error);
		});
	}

	$scope.load = function() {
		$http.get("/settings").then(function(response) {
			if (response.status = 200) {
				$scope.settings = response.data;
			}
		}, function(error) {
			$log.error("failure loading settings", error);
		});
	}

	$scope.save = function() {
		$http.post("/settings", $scope.settings).then(function(response) {
			$scope.showStatusMessage("success", $translate.instant("settings.saved.success"));
		}, function(error) {
			$scope.showStatusMessage("error", $translate.instant("settings.saved.error"));
		});
	}

	$scope.filterCodecs = function(format, type) {
		return $scope.formats[format][type];
	}

	$scope.filterEncoder = function(name) {
		var encoders = formatService.getSupportedEncoders();
		return encoders !== undefined && encoders.length != 0 && formatService.getByProperty(encoders, "name", name);
	}

	$scope.encoderParameter = function(obj, param) {
		var encoder = typeof obj == "String" ? $scope.filterEncoder(obj) : obj;
		if (encoder === undefined || encoder == null || encoder.length == 0)
			return false;
		if (encoder.parameters === undefined || encoder.parameters.length == 0)
			return false;

		return formatService.getByProperty(encoder.parameters, "name", param);
	}

	$scope.qualityData = function(obj, param) {
		var encoder = typeof obj == "String" ? $scope.filterEncoder(obj) : obj;
		if (encoder === undefined || encoder == null || encoder.length == 0)
			return false;

		var data = {};
		if (param == "qscale") {
			data = {
				id : "qualitySlider",
				min : 0,
				max : 31,
				step : 1,
				value : 14,
				reversed : true
			};
		} else if (encoder.name == "libx264") {
			data = {
				id : "qualitySlider",
				min : 0,
				max : 51,
				step : 0.25,
				precision : 2,
				value : 23,
				reversed : true
			};
		} else {
			if (encoder.parameters === undefined || encoder.parameters.length == 0)
				return false;
			var p = formatService.getByProperty(encoder.parameters, "name", param);
			if (p !== undefined && p != null) {
				data = {
					id : "qualitySlider",
					min : p.type == "float" ? parseFloat(p.fromValue) : parseInt(p.fromValue),
					max : p.type == "float" ? parseFloat(p.toValue) : parseInt(p.toValue),
					step : p.type == "float" ? 0.25 : 1,
					precision : p.type == "float" ? 2 : 0,
					value : Math.floor((p.toValue - p.fromValue) / 2),
					reversed : true
				};
			}
		}

		return JSON.stringify(data);
	}

	$scope.getLength = function(obj) {
		return Object.keys(obj).length;
	}

	$scope.changeCodec = function(type) {
		var codec = $scope.settings[type].codec;

		$scope.selectedCodec[type] = $scope.filterEncoder(codec);
		$scope.settings[type] = {
			"codec" : codec
		};
	}

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
	}

	// init
	$scope.load();
	$scope.supportedFormats();
});