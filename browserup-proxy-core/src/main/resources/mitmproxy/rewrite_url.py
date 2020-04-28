import json
import base64
import typing
import tempfile

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


class RewriteUrlResource:

    def addon_path(self):
        return "rewrite_url"

    def __init__(self, rewrite_url_addon):
        self.num = 0
        self.rewrite_url_addon = rewrite_url_addon

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_rewrite_url(self, req, resp):
        for k, v in req.params.items():
            self.rewrite_url_addon.rules[k] = v

    def on_rewrite_urls(self, req, resp):
        for k, v in req.params.items():
            self.rewrite_url_addon.rules[k] = v

    def on_clear_rewrite_rules(self, req, resp):
        self.rewrite_url_addon.rules = {}

    def on_remove_rewrite_rule(self, req, resp):
        self.rewrite_url_addon.rules.pop(req.get_param('pattern'))


class RewriteUrlAddOn:

    def __init__(self):
        self.num = 0
        self.rules = {}

    def get_resource(self):
        return RewriteUrlResource(self)

    def request(self, flow):
        for k, v in self.rules.items():
            flow.request.headers[k] = v


addons = [
    RewriteUrlAddOn()
]
