module.exports = [
		"settings",
		{
			templateUrl : "/assets/templates/settings.component.html",

			controller : function($rootScope, $scope, $http, $translate, $log, $timeout, formatService, asyncQueue) {
				var removeTimeout = 30000;
				$scope.status = {};

				// configured defaults
				$scope.converterFormats = require("../defaults/converter-formats.js");
				$scope.defaultPresets = require("../defaults/presets.js");
				$scope.defaultTunes = require("../defaults/tunes.js");
				$scope.defaultScales = require("../defaults/scales.js");
				$scope.defaultFrameRates = require("../defaults/framerates.js");
				$scope.defaultProfiles = require("../defaults/profiles.js");
				$scope.defaultLevels = require("../defaults/levels.js");
				$scope.defaultBitrates = require("../defaults/bitrates.js");

				// default settings
				$scope.settings = require("../defaults/settings.js");

				$scope.formats = {};
				$scope.selectedCodec = {};
				$scope.hwaccels = [];

				$scope.supportedFormats = function() {
					var formats = {};
					angular.copy($scope.converterFormats, formats);

					formatService.getSupportedFormats(formats).then(function(formats) {
						$scope.formats = formats;
						$rootScope.$emit("loadingDone");
					}, function(error) {
						$log.error("failure loading formats", error);
					});
				};

				$scope.load = function() {
					$rootScope.$emit("loading");

					asyncQueue.load([ "/settings", "/converter/hwaccels" ]).then(function(results) {
						results.forEach(function(result) {
							if (result.config.url == "/settings") {
								if (result.status === 200) {
									if (result.data.output.length > 0) {
										$scope.settings = result.data;
									}
								}
							} else if (result.config.url == "/converter/hwaccels") {
								$scope.hwaccels = result.data.hwaccels || [];
							}
						});

						$scope.supportedFormats();
					}, function(error) {
						$rootScope.$emit("alertEvent", "error", error);
						$log.error("failure loading settings", error);
						$rootScope.$emit("loadingDone");
					});
				};

				$scope.save = function(settings) {
					$http.post("/settings", settings).then(function() {
						$rootScope.$emit("alertEvent", "success", {
							localizedMessage : "settings.saved.success",
							closeable : true
						});
					}, function() {
						$rootScope.$emit("alertEvent", "success", {
							localizedMessage : "settings.saved.error",
							closeable : false
						});
					});
				};

				$scope.initHWAccelsSettings = function() {
					var hwaccels = [];
					for ( var i in $scope.hwaccels) {
						var hwaccel = $scope.hwaccels[i];
						for ( var j in $scope.settings.hwaccels) {
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

				$scope.filterCodecs = function(format, type) {
					return format !== undefined ? $scope.formats[format][type] : null;
				};

				$scope.isFilteredEncoder = function(encoder, filter) {
					if (encoder !== undefined && filter !== undefined) {
						for ( var i in filter) {
							if (encoder.name.indexOf(filter[i]) !== -1) {
								return true;
							}
						}
					}
					
					return false;
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

				// init
				$scope.load();
			}
		} ];