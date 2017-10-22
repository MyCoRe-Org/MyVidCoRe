module.exports = [ "directoryWatcher", {
	templateUrl : "/assets/templates/dirwatcher.component.html",

	controller : function($scope, $http, $interval) {
		var refresh;
		$scope.status = {};

		$scope.loadData = function() {
			$http({
				method : "GET",
				url : "/widget/directoryWatcher/status"
			}).then(function(response) {
				if (response.status === 200) {
					angular.merge($scope.status, response.data);
				}
			}, function() {
				$scope.status = {};
			});
		};

		$scope.$on("$destroy", function() {
			if (refresh) {
				$interval.cancel(refresh);
			}
		});

		$scope.loadData();

		refresh = $interval(function() {
			$scope.loadData();
		}, 60000);
	}
} ];