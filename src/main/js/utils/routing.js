module.exports = {
	navigation : function(routeNavigation) {
		return {
			restrict : "E",
			replace : true,
			templateUrl : "/assets/templates/navigation.html",
			controller : function($scope) {
				$scope.routes = routeNavigation.routes;
				$scope.activeRoute = routeNavigation.activeRoute;
			}
		};
	},

	access : function($rootScope, $http, $q) {
		$rootScope.accessCache = {};

		var isRoleAllowed = function(role) {
			return $q(function(resolve) {
				$http.post("/user/isAllowed", [ role ]).then(function(result) {
					var res = {};
					res[role] = result.data || false;
					resolve(res);
				}, function(error) {
					var res = {};
					res[role] = false;
					resolve(res);
				});
			});
		};

		this.isAllowed = function(roles) {
			var deferred = $q.defer();

			$q(function(resolve) {
				var rolesToCheck = [];
				angular.forEach(roles, function(role) {
					if ($rootScope.accessCache[role] === undefined) {
						rolesToCheck.push(isRoleAllowed(role));
					}
				});

				if (rolesToCheck.length !== 0) {
					var accessCache = {};
					$q.all(rolesToCheck).then(function(result) {
						angular.forEach(result, function(role) {
							angular.merge(accessCache, role);
						});
						resolve(accessCache);
					});
				} else {
					resolve($rootScope.accessCache);
				}
			}).then(function(accessCache) {
				angular.merge($rootScope.accessCache, accessCache);

				var allowed = false;
				for ( var i in roles) {
					var role = roles[i];
					if ($rootScope.accessCache[role] === true) {
						allowed = true;
					}
				}
				deferred.resolve(allowed);
			});

			return deferred.promise;
		};
	},

	routeNavigation : function($route, $location) {
		var routes = [];

		var buildRoutes = function(rs, parentPath) {
			var _routes = [];
			angular.forEach(rs, function(route, path) {
				if (route.name && route.allowed && (!route.parent || parentPath)) {
					var r = {
						path : [ parentPath, path ].join(""),
						name : route.name,
						icon : route.icon,
					};
					if (route.submenu) {
						var sr = buildRoutes(route.submenu, path);
						r.submenu = sr.length !== 0 ? sr : undefined;
					}
					_routes.push(r);
				}
			});
			return _routes;
		};

		var reload = function() {
			angular.merge(routes, buildRoutes($route.routes));
		};

		// init
		reload();

		return {
			routes : routes,
			activeRoute : function(route) {
				return route.path === $location.path();
			},
			reload : reload
		};
	},

	initRoutes : function($route, $routeProvider, $http, $q, access, routeNavigation) {
		var isAllowed = function(path, route) {
			return $q(function(resolve, reject) {
				if (route.allowedRoles === undefined || route.allowedRoles.length === 0) {
					var res = {};
					route.allowed = true;
					res[path] = route;
					resolve(res);
				} else {
					access.isAllowed(route.allowedRoles).then(function(allowed) {
						var res = {};
						route.allowed = allowed;
						res[path] = route;
						resolve(res);
					});
				}
			});
		};

		var checkRoutes = function(routes) {
			var deferred = $q.defer();

			var queue = [];
			angular.forEach(routes, function(route, path) {
				queue.push(isAllowed(path, route));
				if (route.submenu) {
					queue.push(checkRoutes(route.submenu).then(function(subroutes) {
						route.submenu = subroutes;
					}));
				}
			});

			$q.all(queue).then(function(results) {
				var routes = {};
				angular.forEach(results, function(res) {
					angular.forEach(res, function(route, path) {
						routes[path] = route;
					});
				});
				deferred.resolve(routes);
			});

			return deferred.promise;
		};

		var buildRoutes = function(routes, parentPath, parentRoute) {
			angular.forEach(routes, function(route, path) {
				route.resolve = {
					clearAlert : function($rootScope) {
						$rootScope.$emit("clearAlertEvent");
					}
				};

				if (path === "default") {
					if (route.allowed) {
						$routeProvider.otherwise(route);
					}
				} else {
					if (route.allowed) {
						if (parentPath && parentRoute) {
							route.parent = parentRoute;
							if (!route.templateUrl) {
								route.templateUrl = parentRoute.templateUrl;
							}
							$routeProvider.when([ parentPath, path ].join(""), route);

							if (route.submenu) {
								buildRoutes(route.submenu, [ parentPath, path ].join(""), route);
							}
						} else {
							$routeProvider.when(path, route);

							if (route.submenu) {
								buildRoutes(route.submenu, path, route);
							}
						}
					}
				}
			});
		};

		$http.get("/assets/templates/navigation.json").then(function(result) {
			checkRoutes(result.data).then(function(routes) {
				buildRoutes(routes);
				$route.reload();
				routeNavigation.reload();
			});
		});
	}
};