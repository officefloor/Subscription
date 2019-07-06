var baseConfig = require('./karma.conf.js')

module.exports = function(config) {
	// Load base config
	baseConfig(config);

	// Override base config
	config.set({
		singleRun : true,
		autoWatch : false,
		browsers : [ 'PhantomJS' ],
		plugins : [ require('karma-jasmine'),
				require('karma-phantomjs-launcher'),
				require('karma-coverage-istanbul-reporter'),
				require('@angular-devkit/build-angular/plugins/karma') ],
		reporters : [ 'progress' ],
	})
}