module.exports = [ "addJob", {
	templateUrl : "/assets/templates/addjob.component.html",

	controller : function($scope, $element, $timeout, Upload) {
		$scope.files = [];

		$scope.cleanUp = function(file) {
			$timeout(function() {
				$scope.files = $scope.files.filter(function(elm) {
					return elm.$$hashKey !== file.$$hashKey;
				});
			}, 30000);
		};

		$scope.upload = function(file) {
			$scope.files.push(file);

			file.uploaded = false;
			file.error = false;

			Upload.upload({
				url : "/converter/addjob",
				data : {
					file : file,
					filename : file.name
				},
			}).then(function(response) {
				if (response.status === 200) {
					file.uploaded = true;
				}
				$scope.cleanUp(file);
			}, function(error) {
				file.error = true;
				$scope.cleanUp(file);
			}, function(evt) {
				file.progress = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
			});
		};
	}
} ];