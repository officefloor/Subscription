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
	default:
		next();
	}
}