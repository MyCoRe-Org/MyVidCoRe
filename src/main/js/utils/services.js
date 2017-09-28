module.exports = {
	"asyncQueue" : function($http, $q) {
		this.load = function(urls) {
			var deferred = $q.defer();

			var queue = [];
			angular.forEach(urls, function(url) {
				queue.push($http.get(url));
			});

			$q.all(queue).then(function(results) {
				deferred.resolve(results);
			}, function(errors) {
				deferred.reject(errors);
			}, function(updates) {
				deferred.update(updates);
			});
			return deferred.promise;
		};
	},
	"formatService" : function($http, $q, asyncQueue) {
		this.supportedEncoders = {};

		function buildFormatURLs(formats) {
			var urls = [];
			angular.forEach(formats, function(options, format) {
				urls.push("/converter/formats/name/" + format);
				angular.forEach(options, function(codecs) {
					angular.forEach(codecs, function(codec) {
						var url = "/converter/codecs/name/" + codec;
						if ($.inArray(url, urls) === -1) {
							urls.push(url);
						}
					});
				});
			});
			return urls;
		}

		function buildEncoderURLs(codecs) {
			var urls = [];
			angular.forEach(codecs, function(cs) {
				angular.forEach(cs, function(c) {
					if (c.encoders !== undefined && c.encoders.length !== 0) {
						angular.forEach(c.encoders.encoder, function(e) {
							var url = "/converter/encoder/" + e;
							if ($.inArray(url, urls) === -1) {
								urls.push(url);
							}
						});
					} else {
						var url = "/converter/encoder/" + c.name;
						if ($.inArray(url, urls) === -1) {
							urls.push(url);
						}
					}
				});
			});
			return urls;
		}

		function getByProperty(array, prop, value) {
			if (array !== undefined) {
				for (var i = 0; i < array.length; i++) {
					if (array[i] !== undefined && array[i][prop] !== undefined && array[i][prop] === value) {
						return array[i];
					}
				}
			}
			return null;
		}

		this.getByProperty = function(array, prop, value) {
			return getByProperty(array, prop, value);
		};

		this.getSupportedEncoders = function() {
			return this.supportedEncoders;
		};

		this.getSupportedFormats = function(formats) {
			var that = this;
			var deferred = $q.defer();

			asyncQueue.load(buildFormatURLs(formats)).then(function(results) {
				var fs = [];
				var cc = {
					audio : [],
					video : []
				};
				var ec = [];

				// load formats and codecs
				angular.forEach(results, function(result) {
					var json = result.data;
					if (json.formats !== undefined && json.formats.length !== 0) {
						fs.push(json.formats[0]);
					} else if (json.codecs !== undefined && json.codecs.length !== 0) {
						var codec = json.codecs[0];
						cc[codec.type.toLowerCase()].push(codec);
					}
				});

				// load encoder infos for codecs
				asyncQueue.load(buildEncoderURLs(cc)).then(function(results) {
					angular.forEach(results, function(result) {
						var json = result.data;
						ec = ec.concat(json.encoders);
					});

					// cache it
					that.supportedEncoders = ec;

					angular.forEach(cc, function(codecs) {
						angular.forEach(codecs, function(codec) {
							if (codec.encoders !== undefined) {
								var encoders = [];
								angular.forEach(codec.encoders.encoder, function(e) {
									encoders.push(getByProperty(ec, "name", e));
								});
								codec.encoders = encoders;
							} else {
								codec.encoders = [];
								codec.encoders.push(getByProperty(ec, "name", codec.name));
							}
						});
					});

					angular.forEach(formats, function(options, format) {
						var f = getByProperty(fs, "name", format);
						if (f !== null) {
							formats[format].description = f.description;
							angular.forEach(options, function(codecs, type) {
								if (Array.isArray(formats[format][type])) {
									var re = [];
									angular.forEach(codecs, function(codec, index) {
										var c = angular.copy(getByProperty(cc[type], "name", codec));
										if (c !== null) {
											formats[format][type][index] = c;
										} else {
											re.push(index);
										}
									});

									for (var i = 0; i < re.length; i++) {
										formats[format][type].splice(re[i], 1);
									}
								}
							});
						} else {
							delete formats[format];
						}
					});
				});

				deferred.resolve(formats);
			});

			return deferred.promise;
		};
	}
};