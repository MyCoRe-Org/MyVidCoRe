require("layout.less");

var appName = "MyVidCoRe";

var app = module.export = angular.module(appName, [ "ngAnimate", "ngSanitize", "pascalprecht.translate" ]);

/**
 * Configure App
 */
app.config(require("./config.js"));

/**
 * Register App Filters
 */
angular.forEach(require("./utils/filters.js"), function(func, name) {
	app.filter(name, func);
});

/**
 * Register App Directives
 */
angular.forEach(require("./utils/directives.js"), function(func, name) {
	app.directive(name, func);
});

/**
 * Register App Services
 */
angular.forEach(require("./utils/services.js"), function(func, name) {
	app.service(name, func);
});

/**
 * Register Directory Watcher Status controller
 */
app.controller("directoryWatcherStatus", require("./controller/directory-watcher-status.js"));

/**
 * Register Converter Status controller
 */
app.controller("converterStatus", require("./controller/converter-status.js"));

/**
 * Register Settings controller
 */
app.controller("settings", require("./controller/settings.js"));

/**
 * Start App
 */
app.run(function($animate) {
	$animate.enabled(true);
});
