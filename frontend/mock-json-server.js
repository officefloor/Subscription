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
			id: domainName,
			domainName: domainName,
			expiresDate: extendsToDate.format(DATE_FORMAT),
			payments: payments,
		}
	}
	
	
	// Create data template
	const data = {
		configuration: {},
		domains: [{
			domainName: 'officefloor.org',
			expiresDate: lastYear
		}, {
			domainName: 'sagenschneider.net',
			expiresDate: soon
		}, {
			domainName: 'officefloor.net',
			expiresDate: soon
		}, {
			domainName: 'activicy.com',
			expiresDate: nextYear
		}],
		payments: [
			createPayments('officefloor.org', 8, 10, true, createDetails(2)),
			createPayments('officefloor.net', 15, 15, false, createDetails(2)),
			createPayments('activicy.com', 10, 5, false, createDetails(2)),
			{ id: 'sagenschneider.net' }
		]
	}
	
	// Return the base data
	return data
}