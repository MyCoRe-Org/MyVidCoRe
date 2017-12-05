module.exports = [ "systemMonitorPlugin", {
	templateUrl : "/assets/templates/sysmonitor.component.html",

	controller : function($scope, $websocket, $document, buildWSURL, plugins) {
		var name = "System Monitor Plugin";
		$scope.gauges = [];

		function getAttrib(attribs, name) {
			var ret = {};
			attribs.forEach(function(attr) {
				if (attr.name === name) {
					ret = attr;
					return;
				}
			});
			return ret;
		}

		function formatValue(name, attribs) {
			var value = getAttrib(attribs, name).value || 0;
			if (name === "systemCpuLoad") {
				value = Math.round(value * 100);
			} else if (name === "freePhysicalMemorySize" || name === "freeSwapSpaceSize") {
				value = formatMaxValue(name, attribs, 0) - value;
			}

			return value;
		}

		function formatMaxValue(name, attribs, maxValue) {
			var mv;
			if (name === "freePhysicalMemorySize") {
				mv = getAttrib(attribs, "totalPhysicalMemorySize").value;
			} else if (name === "freeSwapSpaceSize") {
				mv = getAttrib(attribs, "totalSwapSpaceSize").value;
			}

			return mv || maxValue || 100;
		}

		function formatLabel(name, value) {
			if (name === "freePhysicalMemorySize" || name === "freeSwapSpaceSize") {
				if (value >= (1024 * 1024 * 1024)) {
					return Math.round(value / (1024 * 1024 * 1024)) + "G";
				} else if (value >= (1024 * 1024)) {
					return Math.round(value / (1024 * 1024)) + "M";
				}

				return Math.round(value / 1024) + "K";
			}

			return Math.round(value) + (name === "systemCpuLoad" ? " %" : "");
		}

		plugins.isEnabled(name).then(function(enabled) {
			var gnames = [ "systemCpuLoad", "freePhysicalMemorySize", "freeSwapSpaceSize" ];
			if (enabled) {
				$scope.ws = $websocket(buildWSURL("/sysmonitor"));

				$scope.ws.onOpen(function() {
					$scope.ws.reconnectIfNotNormalClose = true;
				});

				$scope.ws.onMessage(function(message) {
					if (message && message.data) {
						var mon = angular.fromJson(message.data);
						if ($scope.monitor === undefined) {
							$scope.monitor = mon;
						}

						gnames.forEach(function(gt) {
							var elm = $document[0].getElementById(gt);
							if (elm) {
								var gauge = $scope.gauges[gt];
								if (gauge === undefined) {
									$scope.gauges[gt] = Gauge($document[0].getElementById(gt), {
										dialStartAngle : gt == "freePhysicalMemorySize" || gt == "freeSwapSpaceSize" ? 180 : 135,
										dialEndAngle : gt == "freePhysicalMemorySize" || gt == "freeSwapSpaceSize" ? 0 : 45,
										max : formatMaxValue(gt, mon.attribs),
										value : formatValue(gt, mon.attribs),
										label : function(value) {
											return formatLabel(gt, value);
										}
									});
								} else {
									gauge.setValueAnimated(formatValue(gt, mon.attribs), 0.75);
								}
							}
						});
					}
				});
			}
		});

	}
} ];