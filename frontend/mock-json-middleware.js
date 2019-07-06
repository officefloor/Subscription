let configurationCount = 0
const moment = require('moment')

const DATE_FORMAT = 'ddd, D MMM YYYY H:mm:ss [GMT]'

module.exports = (req, res, next) => {
	
	switch(req.path) {
	case '/authenticate':
		res.send({
			accessToken: 'MOCK_ACCESS_TOKEN',
			accessExpireTime: moment().add(20, 'minute').format(DATE_FORMAT),
			refreshToken: 'MOCK_REFRESH_TOKEN',
			refreshExpireTime: moment().add(8, 'hour').format(DATE_FORMAT)
		})
		return
		
	case '/refreshAccessToken':
		res.send({
			accessToken: 'MOCK_ACCESS_TOKEN'
		})
		return
		
	case '/configuration':
		configurationCount++;		
		if (configurationCount % 5 === 0) {
			res.sendStatus(403)
			return
		}
		next()
		return
		
	case '/payments/domain/no.access':
		res.sendStatus(403)
		return
		
	default:
		next()
	}
}