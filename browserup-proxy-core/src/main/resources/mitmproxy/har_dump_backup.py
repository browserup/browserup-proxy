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

class HarDumpAddonResource:

    def addon_path(self):
        return "har"

    def __init__(self, harDumpAddOn):
        self.num = 0
        self.name = "hardump"
        self.harDumpAddOn = harDumpAddOn

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_get_har(self, req, resp):
        har_file = self.harDumpAddOn.save_current_har()
        if req.get_param('cleanHar') == 'true':
            self.harDumpAddOn.reset_har()

        resp.status = falcon.HTTP_200
        resp.body = json.dumps({
            "path": har_file.name
        }, ensure_ascii=False)

    def on_new_page(self, req, resp):
        pageRef = req.get_param('pageRef')
        pageTitle = req.get_param('pageTitle')



class HarDumpAddOn:

    def __init__(self):
        self.num = 0
        self.har = None
        self.har_page_count = 0
        self.current_har_page = None

    def configure(self, updated):
        ctx.log.info('Configuring har dump add-on...')
        self.reset_har()

    def get_default_har_page(self):
        for hp in self.har.pages:
            if hp.title == DEFAULT_PAGE_TITLE:
                return hp
        return {}

    def generate_new_har_log(self):
        return {
            "version": "1.2",
            "creator": {
                "name": "BrowserUp Har Dump",
                "version": "0.1",
                "comment": ""
            },
            "entries": [],
            "pages": []
        }

    def generate_new_har(self):
        return {
            "log": self.generate_new_har_log()
        }

    def reset_har(self):
        self.har = {}
        self.har.update(self.generate_new_har())
        self.add_default_page()

    def get_or_create_har(self, page_ref, page_title):
        if self.har is None:
            self.har = self.new_har(page_ref, page_title)
        return self.har

    def new_page(self, page_ref, page_title):
        har = self.get_or_create_har(page_ref, page_title)

        end_of_page_har = None

        if self.current_har_page is not None:
            current_page_ref = self.current_har_page.id

            self.end_page()

            self.copy_har_through_page_ref(har, self.current_har_page)

    def copy_har_through_page_ref(self, har, page_ref):
        if har is None:
            return None

        if har.log is None:
            return self.generate_new_har()

        page_refs_to_copy = []

        for page in har.log.pages:
            page_refs_to_copy.append(page.id)
            if page_ref == page.id:
                break

        log_copy = self.generate_new_har_log()

        for entry in har.log.entries:
            if entry.page_ref in page_refs_to_copy:
                log_copy.entries.append(entry)

        for page in har.log.pages:
            if page.id in page_refs_to_copy:
                log_copy.pages.append(page)

        har_copy = self.generate_new_har()
        har_copy.log = log_copy

        return har_copy

    def new_har(self, initial_page_ref, initial_page_title):
        old_har = self.end_har()

        self.har_page_count = 0

        self.har = self.generate_new_har()

        self.new_page(initial_page_ref, initial_page_title)

        return old_har

    def end_har(self):
        old_har = self.har
        if old_har is None: return

        self.end_page()

        self.har = None

        return old_har

    def end_page(self):
        previous_har_page = self.current_har_page
        self.current_har_page = {}

        if previous_har_page == {}:
            return

        if previous_har_page.startedDateTime is not None:
            on_load_delta_ms = (datetime.utcnow() - datetime.fromisoformat(previous_har_page.startedDateTime)).total_seconds() * 1000
            previous_har_page.pageTimings.onLoad = on_load_delta_ms

        default_har_page = self.get_default_har_page()
        if default_har_page is not {}:
            if default_har_page.startedDateTime is not None:
                default_har_page.pageTimings = (datetime.utcnow() - datetime.fromisoformat(default_har_page.startedDateTime)).total_seconds() * 1000


    def add_default_page(self):
        self.add_har_page(DEFAULT_PAGE_REF, DEFAULT_PAGE_TITLE)

    def add_har_page(self, pageRef, pageTitle):
        har_page = {
            "id": pageRef,
            "title:": pageTitle,
            "startedDateTime": datetime.utcnow().isoformat(),
            "pageTimings": {
                "onContentLoad": 0,
                "onLoad": 0,
                "comment": ""
            }
        }
        self.har.pages.append(har_page)
        return har_page

    def reset_har_pages(self):
        self.har.log.pages = []

    def get_resource(self):
        return HarDumpAddonResource(self)

    def response(self, flow):
        """
             Called when a server response has been received.
        """
        # -1 indicates that these values do not apply to current request
        ssl_time = -1
        connect_time = -1

        if flow.server_conn and flow.server_conn not in SERVERS_SEEN:
            connect_time = (flow.server_conn.timestamp_tcp_setup -
                            flow.server_conn.timestamp_start)

            if flow.server_conn.timestamp_tls_setup is not None:
                ssl_time = (flow.server_conn.timestamp_tls_setup -
                            flow.server_conn.timestamp_tcp_setup)

            SERVERS_SEEN.add(flow.server_conn)

        # Calculate raw timings from timestamps. DNS timings can not be calculated
        # for lack of a way to measure it. The same goes for HAR blocked.
        # mitmproxy will open a server connection as soon as it receives the host
        # and port from the client connection. So, the time spent waiting is actually
        # spent waiting between request.timestamp_end and response.timestamp_start
        # thus it correlates to HAR wait instead.
        timings_raw = {
            'send': flow.request.timestamp_end - flow.request.timestamp_start,
            'receive': flow.response.timestamp_end - flow.response.timestamp_start,
            'wait': flow.response.timestamp_start - flow.request.timestamp_end,
            'connect': connect_time,
            'ssl': ssl_time,
        }

        # HAR timings are integers in ms, so we re-encode the raw timings to that format.
        timings = {
            k: int(1000 * v) if v != -1 else -1
            for k, v in timings_raw.items()
        }

        # full_time is the sum of all timings.
        # Timings set to -1 will be ignored as per spec.
        full_time = sum(v for v in timings.values() if v > -1)

        started_date_time = datetime.fromtimestamp(flow.request.timestamp_start,
                                                   timezone.utc).isoformat()

        # Response body size and encoding
        response_body_size = len(
            flow.response.raw_content) if flow.response.raw_content else 0
        response_body_decoded_size = len(
            flow.response.content) if flow.response.content else 0
        response_body_compression = response_body_decoded_size - response_body_size

        entry = {
            "page_ref": None,
            "startedDateTime": started_date_time,
            "time": full_time,
            "request": {
                "method": flow.request.method,
                "url": flow.request.url,
                "httpVersion": flow.request.http_version,
                "cookies": self.format_request_cookies(
                    flow.request.cookies.fields),
                "headers": self.name_value(flow.request.headers),
                "queryString": self.name_value(flow.request.query or {}),
                "headersSize": len(str(flow.request.headers)),
                "bodySize": len(flow.request.content),
            },
            "response": {
                "status": flow.response.status_code,
                "statusText": flow.response.reason,
                "httpVersion": flow.response.http_version,
                "cookies": self.format_response_cookies(
                    flow.response.cookies.fields),
                "headers": self.name_value(flow.response.headers),
                "content": {
                    "size": response_body_size,
                    "compression": response_body_compression,
                    "mimeType": flow.response.headers.get('Content-Type', '')
                },
                "redirectURL": flow.response.headers.get('Location', ''),
                "headersSize": len(str(flow.response.headers)),
                "bodySize": response_body_size,
            },
            "cache": {},
            "timings": timings,
        }

        # Store binary data as base64
        if strutils.is_mostly_bin(flow.response.content):
            entry["response"]["content"]["text"] = base64.b64encode(
                flow.response.content).decode()
            entry["response"]["content"]["encoding"] = "base64"
        else:
            entry["response"]["content"]["text"] = flow.response.get_text(
                strict=False)

        if flow.request.method in ["POST", "PUT", "PATCH"]:
            params = [
                {"name": a, "value": b}
                for a, b in flow.request.urlencoded_form.items(multi=True)
            ]
            entry["request"]["postData"] = {
                "mimeType": flow.request.headers.get("Content-Type", ""),
                "text": flow.request.get_text(strict=False),
                "params": params
            }

        if flow.server_conn.connected():
            entry["serverIPAddress"] = str(flow.server_conn.ip_address[0])

        self.har["log"]["entries"].append(entry)

    def save_current_har(self):
        json_dump: str = json.dumps(self.har, indent=2)

        tmp_file = tempfile.NamedTemporaryFile(mode="wb", prefix="har_dump_",
                                               delete=False)

        raw: bytes = json_dump.encode()

        tmp_file.write(raw)
        tmp_file.flush()
        tmp_file.close()

        return tmp_file

    def format_cookies(self, cookie_list):
        rv = []

        for name, value, attrs in cookie_list:
            cookie_har = {
                "name": name,
                "value": value,
            }

            # HAR only needs some attributes
            for key in ["path", "domain", "comment"]:
                if key in attrs:
                    cookie_har[key] = attrs[key]

            # These keys need to be boolean!
            for key in ["httpOnly", "secure"]:
                cookie_har[key] = bool(key in attrs)

            # Expiration time needs to be formatted
            expire_ts = cookies.get_expiration_ts(attrs)
            if expire_ts is not None:
                cookie_har["expires"] = datetime.fromtimestamp(expire_ts,
                                                               timezone.utc).isoformat()

            rv.append(cookie_har)

        return rv

    def format_request_cookies(self, fields):
        return self.format_cookies(cookies.group_cookies(fields))

    def format_response_cookies(self, fields):
        return self.format_cookies((c[0], c[1][0], c[1][1]) for c in fields)

    def name_value(self, obj):
        """
            Convert (key, value) pairs to HAR format.
        """
        return [{"name": k, "value": v} for k, v in obj.items()]


addons = [
    HarDumpAddOn()
]
