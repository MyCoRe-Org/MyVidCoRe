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
				return parseInt(val, 10);
			});
			ngModel.$formatters.push(function(val) {
				return '' + val;
			});
		}
	};
});

app.directive("slider", function() {
	return {
		require : 'ngModel',
		link : function(scope, element, attrs, ngModel) {
			var data = $(element).data();
			var options = {};

			angular.forEach(data, function(value, key) {
				if (key.indexOf("slider") == 0) {
					var k = key.replace("slider", "");
					k = k.substring(0, 1).toLowerCase() + k.substring(1);
					options[k] = value;
				}
			});

			$(element).slider(options).on("slide", function() {
				ngModel = $(this).val();
			});
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

	this.getSupportedFormats = function(formats) {
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

				angular.forEach(cc, function(codecs, type) {
					angular.forEach(codecs, function(codec) {
						if (codec.encoders !== undefined) {
							var encoders = {};
							angular.forEach(codec.encoders.encoder, function(e) {
								encoders[e] = getByProperty(ec, "name", e);
							});
							codec.encoders = encoders;
						} else {
							codec.encoders = {};
							codec.encoders[codec.name] = getByProperty(ec, "name", codec.name);
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
		console.log(formats);
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
		}
	};

	// default settings
	$scope.settings = {
		"format" : "mp4",
		"video" : {
			"codec" : "libx264",
			"framerate" : "auto",
			"framerateType" : "VFR",
			"profile" : "main",
			"level" : "4.0",
			"quality" : {
				"type" : "CRF",
				"rateFactor" : 23,
				"bitrate" : 2500
			}
		},
		"audio" : {
			"codec" : "libfaac",
			"mixdown" : "stereo",
			"samplerate" : "auto",
			"bitrate" : 160
		}
	};

	$scope.formats = {};

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
			if (response.status = 200)
				angular.merge($scope.settings, response.data);
		}, function(error) {
			$log.error("failure loading settings", error);
		});
	}

	$scope.save = function(settings) {
		$http.post("/settings", settings).then(function(response) {
			$scope.showStatusMessage("success", $translate.instant("settings.saved.success"));
		}, function(error) {
			$scope.showStatusMessage("error", $translate.instant("settings.saved.error"));
		});

	}

	$scope.filterCodecs = function(format, type) {
		return $scope.formats[format][type];
	}

	$scope.getLength = function(obj) {
		return Object.keys(obj).length;
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
	$scope.supportedFormats();
	$scope.load();
});