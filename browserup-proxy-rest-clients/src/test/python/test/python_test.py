from __future__ import print_function
import time
import openapi_client
import os

from openapi_client.rest import ApiException
from pprint import pprint

# Create an instance of the API class
api_client = openapi_client.ApiClient()
api_client.configuration.host = 'http://' + os.environ['PROXY_REST_HOST'] + ':' + os.environ['PROXY_REST_PORT']

api_instance = openapi_client.DefaultApi(api_client)
port = os.environ['PROXY_PORT']
url_pattern = '.*'

try:
    api_response = api_instance.entries(port, url_pattern)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DefaultApi->entries: %s\n" % e)
    raise