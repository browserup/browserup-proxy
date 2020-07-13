import json
import base64
import typing
import tempfile

import re

from datetime import datetime
from datetime import timezone

import asyncio

import falcon

from mitmproxy import ctx

from mitmproxy import connections
from mitmproxy import version
from mitmproxy.utils import strutils
from mitmproxy.net.http import cookies
from mitmproxy import http

class BlackListResource:

    def addon_path(self):
        return "blacklist"

    def __init__(self, black_list_addon):
        self.black_list_addon = black_list_addon

    def on_get(self, req, resp, method_name):
        try:
            asyncio.get_event_loop()
        except:
            asyncio.set_event_loop(asyncio.new_event_loop())
        getattr(self, "on_" + method_name)(req, resp)

    def on_put(self, req, resp, method_name):
        try:
            asyncio.get_event_loop()
        except:
            asyncio.set_event_loop(asyncio.new_event_loop())
        getattr(self, "on_" + method_name)(req, resp)

    def on_blacklist_requests(self, req, resp):
        url_pattern = req.get_param('urlPattern')
        status_code = req.get_param('statusCode')
        http_method_pattern = req.get_param('httpMethodPattern')

        ctx.log.info(
            'Blacklisting url pattern: {}, status code: {}, method pattern: {}'.
                format(url_pattern, status_code, http_method_pattern))

        try:
            url_pattern_compiled = self.parse_regexp(url_pattern)

            http_method_pattern_compiled = None
            if http_method_pattern is not None:
                http_method_pattern_compiled = self.parse_regexp(http_method_pattern)
        except re.error:
            raise falcon.HTTPBadRequest("Invalid regexp patterns")

        self.black_list_addon.black_list.append({
            "status_code": status_code,
            "url_pattern": url_pattern_compiled,
            "http_method_pattern": http_method_pattern_compiled
        })

    def on_set_black_list(self, req, resp):
        self.black_list_addon.black_list = []

        blacklist = json.loads(req.bounded_stream.read())

        for bl_item in blacklist:
            try:
                url_pattern_compiled = self.parse_regexp(bl_item['urlPattern'])

                http_method_pattern_compiled = None
                if bl_item['httpMethodPattern'] is not None:
                    http_method_pattern_compiled = self.parse_regexp(bl_item['httpMethodPattern'])

                ctx.log.info(
                    'Blacklisting url pattern: {}, status code: {}, method pattern: {}'.
                        format(bl_item['urlPattern'], bl_item['statusCode'], bl_item['httpMethodPattern']))

                self.black_list_addon.black_list.append({
                    "status_code": bl_item['statusCode'],
                    "url_pattern": url_pattern_compiled,
                    "http_method_pattern": http_method_pattern_compiled
                })
            except re.error:
                raise falcon.HTTPBadRequest("Invalid regexp patterns")

    def parse_regexp(self, raw_regexp):
        if not raw_regexp.startswith('^'):
            raw_regexp = '^' + raw_regexp
        if not raw_regexp.endswith('$'):
            raw_regexp = raw_regexp + '$'
        return re.compile(raw_regexp)

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

        for bl_item in self.black_list:
            request_url = flow.request.url

            if bl_item['http_method_pattern'] is None:
                break

            if not request_url.startswith("http") and not request_url.startswith("https"):
                request_url = 'https://' + request_url

            if bl_item['url_pattern'].match(request_url) and \
                    ((bl_item['http_method_pattern'] is None) or
                     (bl_item['http_method_pattern'].match(flow.request.method))):
                status_code = bl_item['status_code']
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

        is_blacklisted = False
        status_code = 400

        for bl_item in self.black_list:
            request_url = flow.request.url

            if flow.request.method == 'CONNECT':
                if bl_item['http_method_pattern'] is None:
                    break

                if not request_url.startswith("http") and not request_url.startswith("https"):
                    request_url = 'https://' + request_url

            if bl_item['url_pattern'].match(request_url) and \
                    ((bl_item['http_method_pattern'] is None) or
                     (bl_item['http_method_pattern'].match(flow.request.method))):
                status_code = bl_item['status_code']
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
