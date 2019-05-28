let configurationCount = 0

module.exports = (req, res, next) => {
	
	switch(req.path) {
	case '/authenticate':
		res.send({
			accessToken: 'MOCK_ACCESS_TOKEN',
			refreshToken: 'MOCK_REFRESH_TOKEN'
		})
		break;
	case '/refreshAccessToken':
		res.send({
			accessToken: 'MOCK_ACCESS_TOKEN'
		})
		break
	case '/configuration':
		configurationCount++;		
		if (configurationCount % 3 === 0) {
			res.sendStatus(403)
			return
		}
		next()
		break
	default:
		next()
	}
}