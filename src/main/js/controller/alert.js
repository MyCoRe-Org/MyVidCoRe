module.exports = function($rootScope, $scope, $translate) {
	$scope.alertObj = {};

	$rootScope.$on("alertEvent", function(event, type, obj) {
		if (obj === null) {
			return;
		}

		$scope.alertObj.type = type;
		$scope.alertObj.show = true;
		$scope.alertObj.closeable = true;
		if (typeof obj === "string") {
			$scope.alertObj.headline = $translate.instant("alert.type." + type);
			$scope.alertObj.message = obj;
		} else {
			obj = obj.data || obj;
			$scope.alertObj.headline = obj.localizedHeadline ? $translate.instant(obj.localizedHeadline) : undefined ||
					$translate.instant("alert.type." + type);
			$scope.alertObj.message = obj.localizedMessage ? $translate.instant(obj.localizedMessage) : undefined || obj.message;

			if (obj.stackTrace !== undefined) {
				$scope.alertObj.stackTrace = obj.stackTrace;
			} else if (obj.localizedMessage === undefined) {
				if (obj.status === -1) {
					$scope.alertObj.message = $translate.instant("alert.network.unreachable", obj);
				} else {
					$scope.alertObj.message = $translate.instant("alert.network.error." + obj.status, obj);
				}
			}
			$scope.alertObj.closeable = obj.closeable || $scope.alertObj.closeable;
		}
	});

	$rootScope.$on("clearAlertEvent", function(event) {
		$scope.alertObj = {};
	});

	$scope.clear = function() {
		$scope.alertObj.show = false;
	};
};