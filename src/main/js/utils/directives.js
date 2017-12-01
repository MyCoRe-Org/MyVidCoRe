module.exports = {
	"convertToNumber" : function() {
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
	},
	"slider" : function() {
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
	},
	"settingsAudio" : function() {
		return {
			restrict : "E",
			require : "^^settings",
			templateUrl : "/assets/templates/settings.audio.directive.html",
			scope : {
				type : "@",
				index : "=index",
				parent : "=parent",
				output : "=output",
				audio : "=audio",
			}
		};
	},
	"settingsVideo" : function() {
		return {
			restrict : "E",
			require : "^^settings",
			templateUrl : "/assets/templates/settings.video.directive.html",
			scope : {
				type : "@",
				index : "=index",
				parent : "=parent",
				output : "=output",
				video : "=video",
				encoderFilter : "=encoderFilter"
			}
		};
	},
	"settingsAdvancedOptions" : function() {
		return {
			restrict : "E",
			require : "^^settings",
			templateUrl : "/assets/templates/settings.advanced-options.directive.html",
			scope : {
				type : "@",
				index : "=index",
				output : "=output",
				codec : "=codec"
			}
		};
	}
};