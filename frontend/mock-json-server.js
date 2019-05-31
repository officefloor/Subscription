const moment = require('moment')

module.exports = () => {
	
	// Default date format
	const DATE_FORMAT = 'ddd, D MMM YYYY H:mm:ss [GMT]'
	
	// Create some dates
	const lastYear = moment().subtract(1, 'year').format(DATE_FORMAT)
	const soon = moment().add(2, 'week').format(DATE_FORMAT)
	const nextYear = moment().add(1, 'year').format(DATE_FORMAT)
		
	// Create data template
	const data = {
		configuration: {},
		domains: [{
			domainName: 'officefloor.org',
			expiresDate: lastYear
		}, {
			domainName: 'officefloor.net',
			expiresDate: soon
		}, {
			domainName: 'activicy.com',
			expiresDate: nextYear
		}]
	}
	
// payments: [{
// paymentDate: last2Years,
// extendsSubscriptionToDate: lastYear,
// isSubscriptionReset: false,
// user: {
// name: 'Daniel'
// },
// invoice: {
// paymentOrderId: 'ORDER-1'
// }
// }]

	
	// Return the base data
	return data
}