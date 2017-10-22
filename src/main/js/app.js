require("layout.less");
var routing = require("./utils/routing.js");

var appName = "MyVidCoRe";

var app = module.export = angular.module(appName, [ "ngAnimate", "ngSanitize", "ngRoute", "ngWebSocket", "pascalprecht.translate" ]);

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
 * Register routing dependencies
 */
app.service("access", routing.access);
app.directive("navigation", routing.navigation);
app.factory("routeNavigation", routing.routeNavigation);

/**
 * Register Controllers
 */
app.controller("alertCtrl", require("./controller/alert.js"));
app.controller("loadingCtrl", require("./controller/loading.js"));
app.controller("directoryWatcherStatusCtrl", require("./controller/directory-watcher-status.js"));
app.controller("converterStatusCtrl", require("./controller/converter-status.js"));
app.controller("settingsCtrl", require("./controller/settings.js"));
app.component("nvMonitorPlugin", require("./controller/nvmonitor.plugin.js"));

/**
 * Start App
 */
app.run(function($animate, $route, $routeProvider, $http, $q, access, routeNavigation) {
	$animate.enabled(true);
	
	routing.initRoutes($route, $routeProvider, $http, $q, access, routeNavigation);
});

jQuery(document).ready(function() {
	jQuery(this).click(function(ev) {
		var $this = jQuery(ev.target);
		if ($this.data("collapse-hide") !== undefined) {
			jQuery($this.data("collapse-hide")).collapse("hide");
		}
	});
});
