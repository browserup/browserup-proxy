import time
import json

from mitmproxy import ctx
from mitmproxy.exceptions import TcpTimeout

RESOLUTION_FAILED_ERROR_MESSAGE = "Unable to resolve host: "
CONNECTION_FAILED_ERROR_MESSAGE = "Unable to connect to host"
RESPONSE_TIMED_OUT_ERROR_MESSAGE = "Response timed out"
DEFAULT_PAGE_REF = "Default"
DEFAULT_PAGE_TITLE = "Default"

class HttpConnectCaptureResource:

    def addon_path(self):
        return "http_connect_capture"

    def __init__(self, har_connect_addon):
        self.num = 0
        self.har_connect_addon = har_connect_addon
        for a in ctx.master.addons.get("scriptloader").addons:
            if 'har_dump.py' in a.fullpath:
                self.har_connect_addon.har_dump_addon = a.addons[0].addons[0]

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)


class HttpConnectCaptureAddOn:

    def __init__(self):
        self.num = 0
        self.har_dump_addon = None

        self.dns_resolution_started_nanos = 0
        self.dns_resolution_finished_nanos = 0
        self.connection_started_nanos = 0
        self.connection_succeeded_time_nanos = 0
        self.send_started_nanos = 0
        self.send_finished_nanos = 0
        self.response_receive_started_nanos = 0
        self.ssl_handshake_started_nanos = 0
        self.http_connect_timing = None

    def generate_http_connect_timing(self):
        return {
            "blockedTimeNanos": -1,
            "dnsTimeNanos": -1,
            "connectTimeNanos": -1,
            "sslHandshakeTimeNanos": -1,
        }

    # TCP Callbacks

    def tcp_resolving_server_address_finished(self, sever_conn):
        self.populate_dns_timings()
        self.dns_resolution_finished_nanos = self.now_time_nanos()

        if self.dns_resolution_started_nanos > 0:
            self.get_http_connect_timing()['dnsTimeNanos'] = self.dns_resolution_finished_nanos - self.dns_resolution_started_nanos
        else:
            self.get_http_connect_timing()['dnsTimeNanos'] = 0

    def tcp_resolving_server_address_started(self, sever_conn):
        self.dns_resolution_started_nanos = int(round(self.now_time_nanos()))
        self.connection_started_nanos = int(round(self.now_time_nanos()))
        self.proxy_to_server_resolution_started()


    # SSL Callbacks
    def ssl_handshake_started(self, flow):
        self.ssl_handshake_started_nanos = int(round(self.now_time_nanos()))

    # HTTP Callbacks

    def http_connect(self, flow):
        self.http_connect_timing = self.get_http_connect_timing()
        self.har_dump_addon.http_connect_timings[flow.client_conn] = self.http_connect_timing
        ctx.log.info('xxx http connect!')

    def http_proxy_to_server_request_started(self, flow):
        self.send_started_nanos = self.now_time_nanos()

    def http_proxy_to_server_request_finished(self, flow):
        self.send_finished_nanos = self.now_time_nanos()
        if self.send_started_nanos > 0:
            self.get_har_entry()['timings'][
                'send'] = self.send_finished_nanos - self.send_started_nanos
        else:
            self.get_har_entry()['timings']['send'] = 0

    def http_server_to_proxy_response_receiving(self, flow):
        self.response_receive_started_nanos = self.now_time_nanos()

    def http_server_to_proxy_response_received(self, flow):
        """"""

    # PROXY Callbacks
    def proxy_to_server_resolution_started(self):
        self.get_http_connect_timing()['blockedTimeNanos'] = 0

    def proxy_to_server_connection_succeeded(self, f):
        self.connection_succeeded_time_nanos = self.now_time_nanos()

        if self.connection_started_nanos > 0:
            self.get_http_connect_timing()['connectTimeNanos'] = self.connection_succeeded_time_nanos - self.connection_started_nanos
        else:
            self.get_http_connect_timing()['connectTimeNanos'] = 0

        if self.ssl_handshake_started_nanos > 0:
            self.get_http_connect_timing()['sslHandshakeTimeNanos'] = self.connection_succeeded_time_nanos - self.ssl_handshake_started_nanos
        else:
            self.get_http_connect_timing()['sslHandshakeTimeNanos'] = 0

    def error(self, flow):
        req_host_port = flow.request.host
        if flow.request.port != 80:
            req_host_port = req_host_port + ':' + str(flow.request.port)
        original_error = HttpConnectCaptureAddOn.get_original_exception(
            flow.error)

        if 'Name or service not known' in str(original_error):
            self.proxy_to_server_resolution_failed(flow, req_host_port,
                                                   original_error)
        elif isinstance(original_error, TcpTimeout):
            self.server_to_proxy_response_timed_out(flow, req_host_port,
                                                    original_error)
        else:
            self.proxy_to_server_connection_failed(flow, original_error)

    # Populate data

    def populate_dns_timings(self):
        if self.dns_resolution_started_nanos > 0 and self.get_har_entry():
            time_now = self.now_time_nanos()
            dns_nanos = time_now - self.dns_resolution_started_nanos
            self.get_har_entry()['timings']['dns'] = dns_nanos

    def populate_timings_for_failed_connect(self):
        if self.connection_started_nanos > 0:
            connect_nanos = self.now_time_nanos() - self.connection_started_nanos
            self.get_har_entry()['timings']['connect'] = connect_nanos
        self.populate_dns_timings()

    def populate_server_ip_address(self, flow, original_error):
        if isinstance(original_error, (ConnectionRefusedError, TcpTimeout)):
            if flow.server_conn and flow.server_conn.ip_address:
                self.get_har_entry()['serverIPAddress'] = str(
                    flow.server_conn.ip_address[0])

    def get_resource(self):
        return HttpConnectCaptureResource(self)

    def proxy_to_server_resolution_failed(self, flow, req_host_port,
                                          original_error):
        msg = RESOLUTION_FAILED_ERROR_MESSAGE + req_host_port
        self.create_har_entry_for_failed_connect(flow.request, msg)
        self.populate_dns_timings()
        self.populate_server_ip_address(flow, original_error)

        self.get_har_entry()['time'] = self.calculate_total_elapsed_time()

    def proxy_to_server_connection_failed(self, flow, original_error):
        msg = CONNECTION_FAILED_ERROR_MESSAGE
        self.create_har_entry_for_failed_connect(flow.request, msg)
        self.populate_timings_for_failed_connect()
        self.populate_server_ip_address(flow, original_error)

        self.get_har_entry()['time'] = self.calculate_total_elapsed_time()

    def server_to_proxy_response_timed_out(self, flow, req_host_port,
                                           original_error):
        msg = RESPONSE_TIMED_OUT_ERROR_MESSAGE
        self.create_har_entry_for_failed_connect(flow.request, msg)
        self.populate_timings_for_failed_connect()
        self.populate_server_ip_address(flow, original_error)

        current_time_nanos = self.now_time_nanos()

        if self.send_started_nanos > 0 and self.send_finished_nanos == 0:
            self.get_har_entry()['timings'][
                'send'] = current_time_nanos - self.send_started_nanos

        elif self.send_finished_nanos > 0 and self.response_receive_started_nanos == 0:
            self.get_har_entry()['timings'][
                'wait'] = current_time_nanos - self.send_finished_nanos

        elif self.response_receive_started_nanos > 0:
            self.get_har_entry()['timings'][
                'receive'] = current_time_nanos - self.response_receive_started_nanos

        self.get_har_entry()['time'] = self.calculate_total_elapsed_time()

    def create_har_entry_for_failed_connect(self, request, msg):
        if not self.get_har_entry():
            self.har_dump_addon.create_har_entry_with_default_response(request)

        self.get_har_entry()['response']['_errorMessage'] = msg

    def calculate_total_elapsed_time(self):
        timings = self.get_har_entry()['timings']
        result = (0 if timings['blocked'] == -1 else timings['blocked']) + \
                 (0 if timings['dns'] == -1 else timings['dns']) + \
                 (0 if timings['connect'] == -1 else timings['connect']) + \
                 (0 if timings['send'] == -1 else timings['send']) + \
                 (0 if timings['wait'] == -1 else timings['wait']) + \
                 (0 if timings['receive'] == -1 else timings['receive'])
        return self.nano_to_ms(result)

    def get_har_entry(self):
        return self.har_dump_addon.har_entry

    def get_http_connect_timing(self):
        if self.http_connect_timing is None:
            self.http_connect_timing = self.generate_http_connect_timing()
        return self.http_connect_timing

    @staticmethod
    def get_original_exception(flow_error):
        result = flow_error.cause
        while True:
            if hasattr(result, '__cause__') and result.__cause__:
                result = result.__cause__
            else:
                break
        return result

    @staticmethod
    def now_time_nanos():
        return int(round(time.time() * 1000000000))

    @staticmethod
    def nano_to_ms(time_nano):
        return int(time_nano / 1000000)


addons = [
    HttpConnectCaptureAddOn()
]
