config.devServer = config.devServer || {};
config.devServer.proxy = [
    {
        context: ['/api', '/ical'],
        target: 'https://login.schulmanager-online.de',
        changeOrigin: true,
        secure: true,
        headers: {
            'Referer': 'https://login.schulmanager-online.de/',
            'Origin': 'https://login.schulmanager-online.de'
        }
    }
];