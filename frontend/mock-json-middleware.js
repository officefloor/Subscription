let configurationCount = 0

module.exports = (req, res, next) => {
	
	switch(req.path) {
	case '/authenticate':
		res.send({
			accessToken: 'MOCK_ACCESS_TOKEN',
			refreshToken: 'MOCK_REFRESH_TOKEN'
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