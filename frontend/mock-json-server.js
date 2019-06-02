const fs = require('fs')
const os = require('os')
const moment = require('moment')

module.exports = () => {
	
	// Default date format
	const DATE_FORMAT = 'ddd, D MMM YYYY H:mm:ss [GMT]'
	
	// Create some dates
	const lastYear = moment().subtract(1, 'year').format(DATE_FORMAT)
	const soon = moment().add(2, 'week').format(DATE_FORMAT)
	const nextYear = moment().add(1, 'year').format(DATE_FORMAT)

	// Create the details
	const createDetails = (spacing) => {
		const emailFileName = os.homedir() + '/subscription.emails'
		let emailAddresses
		if (fs.existsSync(emailFileName)) {
			const emailFileContents = fs.readFileSync(emailFileName, 'utf8')
			emailAddresses = JSON.parse(emailFileContents)
		} else {
			console.log(`${emailFileName} not found. Using default emails`)
			emailAddresses = [ 'test@example.com', 'another@test.com' ]
		}
		const details = []
		for (let emailAddress of emailAddresses) {
			details.push({
				name: emailAddress.split('@')[0],
				email: emailAddress
			})
		}
		for (let i = 0; i < spacing; i++) {
			details.push({})
		}
		return details
	}

	// Create the payments
	const createPayments = (domainName, yearsOffset, numberOfYears, isGap, payDetails) => {
		const payments = []
		const startingYear = yearsOffset + (isGap ? 5 : 0)
		const paymentDate = moment().subtract(startingYear, 'year')
		let extendsToDate = moment().subtract(startingYear -1, 'year')
		if (!payDetails) {
			payDetails = [{}]
		}
		let orderId = 1
		for (let i = 0; i < numberOfYears; i++) {
			paymentDate.add(365 - (Math.floor(Math.random() * 50)), 'day')
			extendsToDate.add(1, 'year')
			let isRestartSubscription = false
			if (isGap && (i == 2)) {
				isRestartSubscription = true
				paymentDate.add(4, 'year')
				extendsToDate = moment(paymentDate).add(1, 'year')
			}
			const details = payDetails[i % payDetails.length]
			payments.push({
				paymentDate: paymentDate.format(DATE_FORMAT),
				extendsToDate: extendsToDate.format(DATE_FORMAT),
				isRestartSubscription: isRestartSubscription,
				paidByName: details.name,
				paidByEmail: details.email,
				paymentOrderId: details.email ? 'ORDER-' + (orderId++) : undefined,
			})
		}
		return {
			domainName: domainName,
			expiresDate: extendsToDate.format(DATE_FORMAT),
			payments: payments,
		}
	}
	
	
	// Create data template
	const data = {
		initialise: {
			// TODO load from user home directory
			googleClientId: '443132781504-19vekci7r4t2qvqpbg9q1s32kjnp1c7t.apps.googleusercontent.com',
			paypalClientId: 'AZVitvHU3nWyNt8rTdndNq8MP_CDd-xShU6iO1kMPYrN8ZfGj0d9hAk29MrXZD0WpaAFPO0B1DP4rvLL'
		},
		configuration: {},
		defaultConfiguration: {
			paypalEnvironment: 'sandbox',
			paypalClientId: 'DEFAULT_CLIENT_ID',
			paypalClientSecret: 'DEFAULT_CLIENT_SECRET'
		},
		domains: [{
			domainName: 'sagenschneider.net',
			expiresDate: lastYear
		}, {
			domainName: 'activicy.com',
			expiresDate: lastYear
		}, {
			domainName: 'officefloor.net',
			expiresDate: soon
		}, {
			domainName: 'officefloor.org',
			expiresDate: nextYear
		}],
		domainPayments: [
			createPayments('activicy.com', 10, 5, false, createDetails(2)),
			createPayments('officefloor.org', 8, 10, true, createDetails(2)),
			createPayments('officefloor.net', 15, 15, false, createDetails(2)),
			{ domainName: 'sagenschneider.net' }
		]
	}
	data.domainPayments.forEach((domain) => {
		domain.id = domain.domainName
		if ('officefloor.net' === domain.domainName) {
			domain.expiresDate = soon
		}
	})
	
	// Return the base data
	return data
}