import falcon
import _thread

from mitmproxy import ctx

from wsgiref.simple_server import make_server

initialized = False


def load(l):
    ctx.log.info('Loading addons manager add-on...')
    l.add_option(
        "addons_management_port", int, 8088, "REST api management port.",
    )


def running():
    global initialized
    if not initialized and is_script_loader_initialized():
        ctx.log.info('Scanning for custom add-ons resources...')
        resources = get_resources()
        ctx.log.info('Found resources:')
        for r in resources:
            ctx.log.info('  - ' + str(r.__class__))
        ctx.log.info('Starting falcon REST service...')
        _thread.start_new_thread(start_falcon, tuple([resources]))
        initialized = True


def is_script_loader_initialized():
    script_loader = ctx.master.addons.get("scriptloader")

    for custom_addon in script_loader.addons:
        if len(custom_addon.addons) == 0:
            return False

    return True


def get_resources():
    script_loader = ctx.master.addons.get("scriptloader")
    resources = []
    get_resource_fun_name = "get_resource"

    for custom_addon in script_loader.addons:
        custom_loaded_addon = custom_addon.addons[0]
        if hasattr(custom_loaded_addon, "addons"):
            for ca in custom_loaded_addon.addons:
                if hasattr(ca, get_resource_fun_name):
                    resources.append(getattr(ca, get_resource_fun_name)())
        if hasattr(custom_loaded_addon, get_resource_fun_name):
            resources.append(getattr(custom_loaded_addon, get_resource_fun_name)())

    return resources


def start_falcon(resources):
    app = falcon.API()
    for resource in resources:
        app.add_route("/" + resource.addon_path() + "/{method_name}", resource)

    with make_server('', ctx.options.addons_management_port, app) as httpd:
        print('Starting REST API management on port: {}'.format(ctx.options.addons_management_port))
        httpd.serve_forever()

