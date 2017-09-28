module.exports = function($translateProvider) {
	$translateProvider.useStaticFilesLoader({
		prefix : "/web/assets/i18n/i18n-",
		suffix : ".json"
	});

	$translateProvider.preferredLanguage("de_DE");
	$translateProvider.determinePreferredLanguage();
	$("html").attr("lang", $translateProvider.resolveClientLocale());
};