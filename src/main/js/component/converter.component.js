module.exports = [ "converterStatus", {
	templateUrl : "/assets/templates/converter.component.html",

	controller : function($scope, $http, $interval, $websocket, asyncQueue, buildWSURL) {
		var refresh;
		var removeTimeout = 30000;
		
		$scope.converters = {
			"active" : {},
			"done" : {}
		};
		$scope.details = [];
		
		$scope.ws = $websocket(buildWSURL("/converter"));
		$scope.ws.reconnectIfNotNormalClose = true;
		
		$scope.ws.onMessage(function(message) {
	        if (message && message.data) {
	        	var obj = angular.fromJson(message.data);
	        	if (obj.event && obj.event.type.indexOf("converter") === 0) {
	        		var converters = $scope.converters.active.converter;
	        		var ec = obj.event.object;
	        		var found = false;
	        		for ( var i in converters) {
	        			if (converters[i].id === ec.id) {
	        				converters[i] = ec;
	        				found = true;
	        				break;
	        			}
	        		}
	        		if (!found) {
	        			if (converters) {
	        				converters.push(ec);
	        			} else {
	        				$scope.converters.active = {
	        					"converter" : [ ec ]
	        				};
	        			}
	        		}
	        	}
	        }
		});
	
		$scope.loadData = function() {
			var urls = [];
			for ( var type in $scope.converters) {
				if ({}.hasOwnProperty.call($scope.converters, type)) {
					var c = $scope.converters[type];
					var start = c.start || 0;
					var limit = c.limit || (type === "active" ? 50 : 10);
					urls.push("/widget/converter/" + (start / limit + 1) + "/" + limit + (type === "done" ? "/!isdone" : "") + "/status");
				}
			}
	
			asyncQueue.load(urls).then(function(results) {
				results.forEach(function(result) {
					if (result.status === 200) {
						var type = result.config.url.indexOf("!isdone") !== -1 ? "done" : "active";
						if ($scope.converters[type].length !== 0) {
							$scope.converters[type] = result.data;
						} else {
							angular.merge($scope.converters[type], result.data);
						}
	
					}
				});
			}, function(error) {
				console.error(error);
				$scope.converters = {
					"active" : {},
					"done" : {}
				};
			});
		};
	
		$scope.loadDetailData = function(id) {
			var converter = $scope.details[id] || {};
			if (converter.outputStream === undefined && converter.errorStream === undefined) {
				$http({
					method : "GET",
					url : "/widget/converter/" + id + "/status"
				}).then(function(response) {
					if (response.status === 200) {
						$scope.details[id] = response.data;
					}
				});
			}
		};
	
		$scope.orderByPercent = function(converter) {
			return converter.progress ? converter.progress.percent : -1;
		};
	
		$scope.filterDone = function(converter) {
			var diff = new Date() - new Date(converter.endTime);
			return converter.running || !converter.running && !converter.done ? true : converter.done && diff < removeTimeout ? true : false;
		};
	
		$scope.pagination = function(pagination) {
			var maxPages = 5;
			var total = Math.floor(pagination.total / pagination.limit) + (pagination.total % pagination.limit > 0 ? 1 : 0);
			var page = pagination.start / pagination.limit + 1;
			if (total > 1) {
				var pS = (page - (maxPages - 1) / 2) - 1;
				if (pS < 0) {
					pS = 0;
				}
				var pE = pS + maxPages;
				if (pE >= total) {
					pE = total;
					pS = pE - (maxPages > total ? total : maxPages);
				}
				pagination = [];
				for (var p = pS; p < pE; p++) {
					pagination.push(p + 1);
				}
				return pagination;
			}
	
			return [];
		};
	
		$scope.paginationPage = function(pagination, page) {
			if ($scope.paginationDisabled(pagination, page)) {
				return;
			}
	
			if (typeof page === "string") {
				page = page[0] === "-" ? (pagination.start / pagination.limit) + 1 - parseInt(page.substring(1))
						: page[0] === "+" ? page = (pagination.start / pagination.limit) + 1 + parseInt(page.substring(1)) : 0;
			}
			var start = (page - 1) * pagination.limit;
			pagination.start = start;
			$scope.loadData();
		};
	
		$scope.paginationActive = function(pagination, page) {
			return pagination.start === (page - 1) * pagination.limit;
		};
	
		$scope.paginationDisabled = function(pagination, page) {
			if (typeof page === "string") {
				page = page[0] === "-" ? (pagination.start / pagination.limit) + 1 - parseInt(page.substring(1))
						: page[0] === "+" ? page = (pagination.start / pagination.limit) + 1 + parseInt(page.substring(1)) : 0;
			}
			var total = Math.floor(pagination.total / pagination.limit) + (pagination.total % pagination.limit > 0 ? 1 : 0);
			return page <= 0 || page > total;
		};
	
		$scope.formatStream = function(stream) {
			if (stream !== undefined) {
				return stream.replace(/\r/g, "\n");
			}
		};
		
		$scope.initPreview = function(id, files) {
			videojs("video-" + id).videoJsResolutionSwitcher({
				default: "high",
				dynamicLabel: true
			});
		};
	
		$scope.$on("$destroy", function() {
			if (refresh) {
				$interval.cancel(refresh);
			}
			if ($scope.ws) {
				$scope.ws.close();
			}
		});
	
		$scope.loadData();
	
		refresh = $interval(function() {
			$scope.loadData();
		}, 10000);
	}
} ];