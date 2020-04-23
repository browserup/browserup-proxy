import json
import base64
import typing
import tempfile
import orjson

import re

from datetime import datetime
from datetime import timezone

import falcon

from mitmproxy import ctx

from mitmproxy import connections
from mitmproxy import version
from mitmproxy.utils import strutils
from mitmproxy.net.http import cookies
from mitmproxy import http

# A list of server seen till now is maintained so we can avoid
# using 'connect' time for entries that use an existing connection.
SERVERS_SEEN: typing.Set[connections.ServerConnection] = set()

DEFAULT_PAGE_REF = "Default"
DEFAULT_PAGE_TITLE = "Default"


class BlackListResource:

    def addon_path(self):
        return "blacklist"

    def __init__(self, black_list_addon):
        self.num = 0
        self.black_list_addon = black_list_addon

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_put(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_blacklist_requests(self, req, resp):
        url_pattern = req.get_param('urlPattern')
        status_code = req.get_param('statusCode')
        http_method_pattern = req.get_param('httpMethodPattern')

        try:
            if not url_pattern.startswith('^'):
                url_pattern = '^' + url_pattern
            if not url_pattern.endswith('$'):
                url_pattern = url_pattern + '$'
            if http_method_pattern is not None and not http_method_pattern.endswith('$'):
                http_method_pattern = http_method_pattern + '$'

            url_pattern_compiled = re.compile(url_pattern)

            http_method_pattern_compiled = None

            if http_method_pattern is not None:
                http_method_pattern_compiled = re.compile(http_method_pattern)
        except re.error:
            resp.status = falcon.HTTP_400
            resp.body = "Invalid regular expressions"
            return

        self.black_list_addon.black_list.append({
            "status_code": status_code,
            "url_pattern": url_pattern_compiled,
            "http_method_pattern": http_method_pattern_compiled
        })

    def on_set_black_list(self, req, resp):
        self.black_list_addon.black_list = []

        blacklist = orjson.loads(req.bounded_stream.read())

        for bl in blacklist:
            try:
                if not bl['urlPattern'].startswith('^'):
                    bl['urlPattern'] = '^' + bl['urlPattern']
                if not bl['urlPattern'].endswith('$'):
                    bl['urlPattern'] = bl['urlPattern'] + '$'
                if bl['httpMethodPattern'] is not None and not bl['httpMethodPattern'].endswith('$'):
                    bl['httpMethodPattern'] = bl['httpMethodPattern'] + '$'

                url_pattern_compiled = re.compile(bl['urlPattern'])
                http_method_pattern = bl['httpMethodPattern']
                http_method_pattern_compiled = None

                if http_method_pattern is not None:
                    http_method_pattern_compiled = re.compile(
                        http_method_pattern)

                self.black_list_addon.black_list.append({
                    "status_code": bl['statusCode'],
                    "url_pattern": url_pattern_compiled,
                    "http_method_pattern": http_method_pattern_compiled
                })
            except re.error:
                resp.status = falcon.HTTP_400
                resp.body = "Invalid regular expressions"
                return


class BlackListAddOn:

    def __init__(self):
        self.num = 0
        self.black_list = []

    def get_resource(self):
        return BlackListResource(self)

    def is_blacklist_enabled(self):
        return len(self.black_list) > 0

    def http_connect(self, flow):
        if not self.is_blacklist_enabled():
            return

        is_blacklisted = False
        status_code = 400

        for bl in self.black_list:
            request_url = flow.request.url

            if bl['http_method_pattern'] is None:
                break

            if not request_url.startswith("http") and not request_url.startswith("https"):
                request_url = 'https://' + request_url

            if bl['url_pattern'].match(request_url) and \
                    ((bl['http_method_pattern'] is None) or
                     (bl['http_method_pattern'].match(flow.request.method))):
                status_code = bl['status_code']
                is_blacklisted = True
                break

        if is_blacklisted:
            flow.response = http.HTTPResponse.make(
                int(status_code),
                b"",
                {"Content-Type": "text/html"}
            )
            flow.metadata['BlackListFiltered'] = True

    def request(self, flow):
        if not self.is_blacklist_enabled():
            return

        if 'BlackListFiltered' in flow.metadata:
            return

        is_blacklisted = False
        status_code = 400

        for bl in self.black_list:
            request_url = flow.request.url

            if flow.request.method == 'CONNECT':
                if bl['http_method_pattern'] is None:
                    break

                if not request_url.startswith("http") and not request_url.startswith("https"):
                    request_url = 'https://' + request_url

            if bl['url_pattern'].match(request_url) and \
                    ((bl['http_method_pattern'] is None) or
                     (bl['http_method_pattern'].match(flow.request.method))):
                status_code = bl['status_code']
                is_blacklisted = True
                break

        if is_blacklisted:
            flow.response = http.HTTPResponse.make(
                int(status_code),
                b"",
                {"Content-Type": "text/html"}
            )
            flow.metadata['BlackListFiltered'] = True

addons = [
    BlackListAddOn()
]
