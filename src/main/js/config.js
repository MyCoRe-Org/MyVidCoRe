module.exports = function($translateProvider, $routeProvider, $provide) {
	$translateProvider.useStaticFilesLoader({
		prefix : "/assets/i18n/i18n-",
		suffix : ".json"
	});

	$translateProvider.preferredLanguage("de_DE");
	$translateProvider.determinePreferredLanguage();
	$("html").attr("lang", $translateProvider.resolveClientLocale());

	$provide.factory("$routeProvider", function() {
		return $routeProvider;
	});
};