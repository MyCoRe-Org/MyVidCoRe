const pkg = require("./package.json");
const path = require("path");
const webpack = require("webpack");
const GoogleFontsPlugin = require("google-fonts-webpack-plugin");

module.exports = {
	plugins : [ new webpack.ProvidePlugin({
		$ : "jquery",
		jQuery : "jquery",
		videojs : "video.js",
		"window.videojs" : "video.js",
		Gauge : "svg-gauge",
		"window.Gauge" : "svg-gauge"
	}), new webpack.optimize.CommonsChunkPlugin({
		name : "vendor",
		filename : "js/vendor.bundle.js",
	}), new webpack.optimize.OccurrenceOrderPlugin(), new webpack.optimize.UglifyJsPlugin({
		mangle : false,
		sourceMap : true,
	}), new GoogleFontsPlugin({
		filename : "css/fonts.css",
		path : "fonts/",
		fonts : [ {
			family : "Titillium Web",
			variants : [ "200", "400", "600", "700", "200italic", "400italic", "600italic", "700italic" ]
		}, {
			family : "Source Code Pro",
			variants : [ "400", "300", "500" ]
		} ]
	}) ],
	entry : {
		app : pkg.main,
		vendor : Object.keys(pkg.dependencies).filter(function(name) {
			return [ "font-awesome" ].join("|").indexOf(name) === -1;
		})
	},
	output : {
		path : pkg.config.assetDir,
		publicPath : "/assets/",
		filename : "js/app.bundle.js"
	},
	resolve : {
		modules : [ pkg.config.lessDir, path.resolve(__dirname, "node_modules") ]
	},
	module : {
		rules : [ {
			test : /\.less$/,
			use : [ {
				loader : "style-loader"
			}, {
				loader : "css-loader"
			}, {
				loader : "less-loader",
				options : {
					strictMath : false,
					noIeCompat : false,
					modifyVars : {
						// font-awesome
						"fa-font-path" : "\"../fonts\"",
						// bootstrap
						"icon-font-path" : "\"../fonts/\"",
						"font-family-sans-serif" : "\"Titillium Web\", sans-serif",
						"brand-primary" : "orange",
						// "brand-success" : "#5cb85c",
						// "brand-warning" : "#f0ad4e",
						// "brand-danger" : "#d9534f",
						// "brand-info" : "#5bc0de",
						"panel-default-heading-bg" : "#fafafa",
						"input-border-focus" : "@brand-primary",
						"text-color" : "#5a5a5a"
					}
				}
			} ]
		}, {
			test : /\.(png|jpg|jpeg|gif|svg)$/,
			loader : "url-loader?limit=10000&name=images/[name].[ext]",
		}, {
			test : /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
			loader : "url-loader?limit=10000&name=fonts/[name].[ext]&mimetype=application/font-woff"
		}, {
			test : /\.(eot|ttf)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
			loader : "file-loader?name=fonts/[name].[ext]",
		}, {
			test : /\.css$/,
			loader : "style-loader/useable?name=css/[name].[ext]!css-loader!postcss-loader"
		} ]
	},
	node : {
		fs : "empty",
		tls : "empty"
	}
};