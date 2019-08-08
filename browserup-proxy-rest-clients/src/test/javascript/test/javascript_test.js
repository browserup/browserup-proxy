var BrowserUpProxyApi = require('../client/node_modules/browser_up_proxy_api');


var api = new BrowserUpProxyApi.DefaultApi()
api.basePath = 'http://' + process.env.PROXY_REST_HOST + ':' + process.env.PROXY_REST_PORT
var port = process.env.PROXY_PORT;
var urlPattern = ".*";
var callback = function(error, data, response) {
    if (error) {
        console.error(error);
    } else {
        console.log('API called successfully. Returned data: ' + data);
    }
};
api.entries(port, urlPattern, callback);