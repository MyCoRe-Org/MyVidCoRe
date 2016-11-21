(function($, window, document, undefined) {
	var pluginName = "template";

	function Template(element, options) {
		this.el = element;
		this.$el = $(element);

		this.options = $.extend({}, $.fn[pluginName].defaults, options);

		this.init();
	}

	var EXP_VAR = RegExp("\\{([^\\}]+)", "g");

	var propertyValue = function(vars, property) {
		var p = property.split(".");

		var v = vars[p[0]];
		if (v) {
			return p.length === 1 ? v : propertyValue(v, p.slice(1).join());
		}

		return null;
	};

	var replaceVariables = function(text, vars) {
		var m;
		while ((m = EXP_VAR.exec(text))) {
			var name = m[1];
			text = text.replace(RegExp("\\{" + name + "\\}", "g"), propertyValue(vars, name));
			m = EXP_VAR.exec(text);
		}

		return text;
	}

	var extractVariableSelectors = function(elm, selector, vars) {
		if (elm instanceof jQuery) {
			elm = elm.get(0);
		}

		vars = vars || [];

		if (elm) {
			if (selector === undefined) {
				selector = "";
			}

			if (elm.parentNode.nodeType === 1) {
				if (selector !== undefined && selector.length > 0)
					selector += " > ";

				selector += elm.localName;

				if (elm.id && !EXP_VAR.exec(elm.id)) {
					selector += "#" + elm.id;
				} else if (elm.className) {
					var clss = elm.className.split(/\s/g);
					for (var i = 0; i < clss.length; i++) {
						var cls = clss[i];
						if (EXP_VAR.exec(cls) || cls.length === 0)
							continue;
						selector += "." + cls;
					}
				}
			}

			if (elm.attributes && elm.attributes.length) {
				for (var i = 0; i < elm.attributes.length; i++) {
					var attr = elm.attributes[i];
					while (m = EXP_VAR.exec(attr.value)) {
						var vn = m[1];
						var vs = vars[vn] || [];
						var vt = {
							selector : selector + (selector.length > 0 ? " > " : "") + "@" + attr.name,
							template : attr.value
						};
						vs.push(vt);
						vars[vn] = vs;
					}
				}
			}

			if (elm.childNodes && elm.childNodes.length) {
				for (var i = 0; i < elm.childNodes.length; i++) {
					var cn = elm.childNodes[i];
					if (cn.nodeType === 1) {
						vars = extractVariableSelectors(cn, selector, vars);
					} else if (cn.nodeType === 3) {
						var m;
						while (m = EXP_VAR.exec(cn.nodeValue)) {
							var vn = m[1];
							var vs = vars[vn] || [];
							var vt = {
								selector : selector + (selector.length > 0 ? " > " : "") + "text()",
								template : cn.nodeValue
							};
							vs.push(vt);
							vars[vn] = vs;
						}
					}
				}
			}

		}

		return vars;
	}

	var updateSelectors = function($template, varSels, vars) {
		for ( var name in varSels) {
			if ({}.hasOwnProperty.call(varSels, name)) {
				for (var i = 0; i < varSels[name].length; i++) {
					var vt = varSels[name][i];
					var path = vt.selector;
					var offset = path.lastIndexOf(">");
					var selector = offset !== -1 ? path.substring(0, offset).trim() : null;
					var elm = offset !== -1 ? path.substring(offset + 1).trim() : path.trim();
					var $elm = selector === null ? $template : $template.find(selector);

					if (elm === "text()") {
						$elm.text(replaceVariables(vt.template, vars));
					} else if (elm.startsWith("@")) {
						var attr = elm.substring(1);
						$elm.attr(attr, replaceVariables(vt.template, vars));
					}
				}
			}
		}
	}

	Template.prototype = {
		init : function() {
			this.$template = $(this.options.template);
			this.selectors = extractVariableSelectors(this.$template);
		},

		destroy : function() {
			this.$el.removeData();
		},

		compile : function(vars) {
			var rootId;
			if ((rootId = this.$template.attr("id")) === undefined) {
				console.error("Missing \"id\" attribute on root element.");
				return;
			}

			rootId = replaceVariables(rootId, vars);

			var $elm = $("#" + rootId, this.$el);

			if ($elm.length === 0) {
				$elm = this.$template.clone();
				updateSelectors($elm, this.selectors, vars);
				this.$el.append($elm);
			} else {
				updateSelectors($elm, this.selectors, vars);
			}
		}
	}

	$.fn[pluginName] = function(options) {
		var args = arguments;

		if (options === undefined || typeof options === "object") {
			return this.each(function() {
				if (!$.data(this, "plugin_" + pluginName)) {
					$.data(this, "plugin_" + pluginName, new Template(this, options));
				}
			});
		} else if (typeof options === "string" && options[0] !== "_" && options !== "init") {
			if (Array.prototype.slice.call(args, 1).length === 0 && $.inArray(options, $.fn[pluginName].getters) !== -1) {
				var instance = $.data(this[0], "plugin_" + pluginName);
				return instance[options].apply(instance, Array.prototype.slice.call(args, 1));
			} else {
				return this.each(function() {
					var instance = $.data(this, "plugin_" + pluginName);
					if (instance instanceof Template && typeof instance[options] === "function") {
						instance[options].apply(instance, Array.prototype.slice.call(args, 1));
					}
				});
			}
		}
	};

	$.fn[pluginName].getters = [ "compile" ];

	/**
	 * Default options
	 */
	$.fn[pluginName].defaults = {};

}(jQuery, window, document));