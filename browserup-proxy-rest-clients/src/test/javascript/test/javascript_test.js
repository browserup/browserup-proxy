var BrowserUpProxyApi = require('/client/node_modules/browser_up_proxy_api');


var api = new BrowserUpProxyApi.DefaultApi()
api.apiClient.basePath = 'http://' + process.env.PROXY_REST_HOST + ':' + process.env.PROXY_REST_PORT
var port = process.env.PROXY_PORT;
var urlPattern = ".*";
var callback = function(error, data, response) {
    if (error) {
        console.error('Error while calling API: ' + JSON.stringify(error));
        throw new Error(error);
    } else {
        console.log('API called successfully. Returned data: ' + JSON.stringify(data));
    }
};
api.entries(port, urlPattern, callback);
