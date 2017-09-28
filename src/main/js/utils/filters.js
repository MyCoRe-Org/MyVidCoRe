module.exports = {
	"endsWith" : function() {
		return function(input, search, prop) {
			if (input instanceof Array || typeof input === "object") {
				for ( var i in input) {
					if (prop && input[i][prop].endsWith(search) || input[i].endsWith(search)) {
						return true;
					}
				}
			} else if (typeof someVar === "string") {
				input = input || "";
				return input.endsWith(search);
			}

			return false;
		};
	},
	"translateWithFallBack" : function($translate) {
		return function(key, prefix, fallBack, replaceRegExp) {
			fallBack = replaceRegExp !== undefined ? fallBack.replace(new RegExp(replaceRegExp, "g"), "").trim() : fallBack.trim();
			var text = $translate.instant(prefix + key);
			return text === prefix + key ? fallBack || key : text;
		};
	},
	"fileName" : function() {
		return function(input) {
			input = input || "";
			return input.split(/\/|\\/).pop();
		};
	},
	"formatScale" : function() {
		return function(input) {
			var scale = (input || "").split(":");
			return parseInt(scale[0]) < 0 ? scale[1] + "p" : scale[0] + "x" + scale[1];
		};
	},
	"parseNumber" : function() {
		return function(input, type) {
			input = input || "";
			return type === "int" ? parseInt(input) : type === "float" ? parseFloat(input) : input;
		};
	},
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
	}
};