import json
import base64
import typing
import tempfile

from urllib.parse import urlparse

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

DEFAULT_PAGE_REF = "Default"
DEFAULT_PAGE_TITLE = "Default"


class RewriteUrlResource:

    def addon_path(self):
        return "rewrite_url"

    def __init__(self, rewrite_url_addon):
        self.rewrite_url_addon = rewrite_url_addon
        for a in ctx.master.addons.get("scriptloader").addons:
            if 'har_dump.py' in a.fullpath:
                self.rewrite_url_addon.har_dump_addon = a.addons[0].addons[0]


    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_rewrite_url(self, req, resp):
        for k, v in req.params.items():
            compiled_pattern = self.parse_regexp(k)
            self.rewrite_url_addon.rules[k] = {
                "replacement": v,
                "url_pattern": compiled_pattern
            }

    def on_rewrite_urls(self, req, resp):
        for k, v in req.params.items():
            compiled_pattern = self.parse_regexp(k)
            self.rewrite_url_addon.rules[k] = {
                "replacement": v,
                "url_pattern": compiled_pattern
            }

    def on_clear_rewrite_rules(self, req, resp):
        self.rewrite_url_addon.rules = {}

    def on_remove_rewrite_rule(self, req, resp):
        self.rewrite_url_addon.rules.pop(req.get_param('pattern'))

    def parse_regexp(self, raw_regexp):
        if not raw_regexp.startswith('^'):
            raw_regexp = '^' + raw_regexp
        if not raw_regexp.endswith('$'):
            raw_regexp = raw_regexp + '$'
        return re.compile(raw_regexp)


class RewriteUrlAddOn:

    def __init__(self):
        self.har_dump_addon = None
        self.rules = {}

    def get_resource(self):
        return RewriteUrlResource(self)

    def request(self, flow):
        self.har_dump_addon.get_or_create_har(DEFAULT_PAGE_REF, DEFAULT_PAGE_TITLE, True)
        rewrote = False
        rewritten_url = flow.request.url
        for url, rule in self.rules.items():
            if rule['url_pattern'].match(rewritten_url):
                rewrote = True
                rewritten_url = re.sub(rule['url_pattern'], rule['replacement'], rewritten_url)

        if rewrote:
            original_host_port = flow.request.host + ':' + str(flow.request.port)

            parsed_rewritten_url = urlparse(rewritten_url)
            rewritten_host_port = parsed_rewritten_url.hostname + ':' + str(parsed_rewritten_url.port)

            flow.request.url = rewritten_url

            if original_host_port is not rewritten_host_port:
                if 'Host' in flow.request.headers:
                    flow.request.headers['Host'] = rewritten_host_port



    def is_http_or_https(self, req):
        return req.url.startswith('https') or req.url.startswith('http')

addons = [
    RewriteUrlAddOn()
]
