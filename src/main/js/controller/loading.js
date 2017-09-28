module.exports = function($rootScope, $scope, $translate) {
	$scope.loading = false;

	$rootScope.$on("loading", function(event) {
		$scope.loading = true;
	});

	$rootScope.$on("loadingDone", function(event) {
		$scope.loading = false;
	});
};
