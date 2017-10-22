module.exports = function($compileProvider, $translateProvider, $routeProvider, $provide) {
	$compileProvider.debugInfoEnabled(false);
	
	$translateProvider.useStaticFilesLoader({
		prefix : "/assets/i18n/i18n-",
		suffix : ".json"
	});

	$translateProvider.preferredLanguage("de_DE");
	$translateProvider.determinePreferredLanguage();
	$translateProvider.useSanitizeValueStrategy("sanitizeParameters");

	$("html").attr("lang", $translateProvider.resolveClientLocale());

	$provide.factory("$routeProvider", function() {
		return $routeProvider;
	});
};