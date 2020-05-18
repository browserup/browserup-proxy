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

# A list of server seen till now is maintained so we can avoid
# using 'connect' time for entries that use an existing connection.
SERVERS_SEEN: typing.Set[connections.ServerConnection] = set()

DEFAULT_PAGE_REF = "Default"
DEFAULT_PAGE_TITLE = "Default"


class ProxyManagerResource:

    def addon_path(self):
        return "proxy_manager"

    def __init__(self, harDumpAddOn):
        self.num = 0
        ctx.options.connection_idle_seconds = -1
        ctx.options.dns_resolving_delay_ms = -1

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

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


class ProxyManagerAddOn:

    def get_resource(self):
        return ProxyManagerResource(self)


addons = [
    ProxyManagerAddOn()
]
