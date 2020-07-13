"""
This inline script can be used to dump flows as HAR files.

example cmdline invocation:
mitmdump -s ./har_dump.py --set hardump=./dump.har

filename endwith '.zhar' will be compressed:
mitmdump -s ./har_dump.py --set hardump=./dump.zhar
"""

import json
import base64
import typing
import tempfile

from datetime import datetime
from datetime import timezone

import falcon

from mitmproxy import ctx

from mitmproxy import connections
from mitmproxy import version
from mitmproxy.utils import strutils
from mitmproxy.net.http import cookies


class ProxyManagerResource:

    def addon_path(self):
        return "proxy_manager"

    def __init__(self, harDumpAddOn):
        ctx.options.connection_idle_seconds = -1
        ctx.options.dns_resolving_delay_ms = -1

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_health_check(self, req, resp):
        resp.body = 'OK'
        resp.status = falcon.HTTP_200

    def on_trust_all(self, req, resp):
        trust_all = req.get_param('trustAll')
        if trust_all is not None and str(trust_all) == str(True):
            ctx.options.ssl_insecure = True

    def on_set_connection_timeout_idle(self, req, resp):
        idle_seconds = req.get_param('idleSeconds')
        if idle_seconds is not None:
            ctx.options.connection_idle_seconds = int(idle_seconds)

    def on_set_dns_resolving_delay_ms(self, req, resp):
        delay_ms = req.get_param('delayMs')
        if delay_ms is not None:
            ctx.options.dns_resolving_delay_ms = int(delay_ms)

    def on_set_upstream_proxy_authorization(self, req, resp):
        credentials = req.get_param('credentials')
        if credentials is not None:
            ctx.options.upstream_proxy_credentials = credentials

    def on_set_chained_proxy_non_proxy_hosts(self, req, resp):
        non_proxy_hosts = req.get_param('nonProxyHosts')

        if non_proxy_hosts is not None:
            non_proxy_hosts_parsed = non_proxy_hosts.strip("[]").split(",")
            ctx.options.upstream_proxy_exception_hosts = non_proxy_hosts_parsed
        else:
            ctx.options.upstream_proxy_exception_hosts = []


class ProxyManagerAddOn:

    def get_resource(self):
        return ProxyManagerResource(self)

    def http_connect(self, f):
        if ctx.options.upstream_proxy_credentials and f.mode == "upstream":
            f.request.headers["Proxy-Authorization"] = "Basic " + ctx.options.upstream_proxy_credentials

    def requestheaders(self, f):
        if self.are_upstream_proxy_credentials_available():
            if f.mode == "upstream" and not f.server_conn.via:
                f.request.headers["Proxy-Authorization"] = "Basic " + ctx.options.upstream_proxy_credentials
            elif ctx.options.mode.startswith("reverse"):
                f.request.headers["Proxy-Authorization"] = "Basic " + ctx.options.upstream_proxy_credentials

    def response_from_upstream_proxy(self, f):
        if self.are_upstream_proxy_credentials_available() and f.response is not None and f.response.status_code == 407:
            f.response.status_code = 502

    def are_upstream_proxy_credentials_available(self):
        return ctx.options.upstream_proxy_credentials is not None and ctx.options.upstream_proxy_credentials != ""

addons = [
    ProxyManagerAddOn()
]
