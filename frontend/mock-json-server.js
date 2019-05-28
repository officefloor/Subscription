module.exports = () => {
	
	// Create data template
	const data = {
		configuration: {},
		defaultConfiguration: {
			paypalEnvironment: 'sandbox',
			paypalClientId: 'DEFAULT_CLIENT_ID',
			paypalClientSecret: 'DEFAULT_CLIENT_SECRET'
		},
	}
	
	// Return the base data
	return data
}