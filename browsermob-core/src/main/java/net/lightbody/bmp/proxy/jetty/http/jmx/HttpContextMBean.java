// ========================================================================
// $Id: HttpContextMBean.java,v 1.17 2005/08/13 00:01:26 gregwilkins Exp $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.lightbody.bmp.proxy.jetty.http.jmx;

import net.lightbody.bmp.proxy.jetty.http.HttpContext;
import net.lightbody.bmp.proxy.jetty.log.LogFactory;
import net.lightbody.bmp.proxy.jetty.util.LifeCycleEvent;
import net.lightbody.bmp.proxy.jetty.util.LifeCycleListener;
import net.lightbody.bmp.proxy.jetty.util.LogSupport;
import net.lightbody.bmp.proxy.jetty.util.jmx.LifeCycleMBean;
import net.lightbody.bmp.proxy.jetty.util.jmx.ModelMBeanImpl;
import org.apache.commons.logging.Log;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.HashMap;


/* ------------------------------------------------------------ */
/**
 *
 * @version $Revision: 1.17 $
 * @author Greg Wilkins (gregw)
 */
public class HttpContextMBean extends LifeCycleMBean
{
    private static Log log = LogFactory.getLog(HttpContextMBean.class);

    private HttpContext _httpContext;
    private HashMap _rlMap=new HashMap(3);
    private HashMap _handlerMap = new HashMap();

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @exception MBeanException
     */
    public HttpContextMBean()
        throws MBeanException
    {}

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("virtualHosts");
        defineAttribute("hosts");
        defineAttribute("contextPath");

        defineAttribute("handlers", ModelMBeanImpl.READ_ONLY, ModelMBeanImpl.ON_MBEAN);
        defineAttribute("requestLog", ModelMBeanImpl.READ_ONLY, ModelMBeanImpl.ON_MBEAN);
        
        defineAttribute("classPath");

        defineAttribute("realm");
        defineAttribute("realmName");

        defineAttribute("redirectNullPath");
        defineAttribute("resourceBase");
        defineAttribute("maxCachedFileSize");
        defineAttribute("maxCacheSize");
        defineOperation("flushCache",
                        ModelMBeanImpl.IMPACT_ACTION);
        defineOperation("getResource",
                        new String[] {ModelMBeanImpl.STRING},
                        ModelMBeanImpl.IMPACT_ACTION);

        defineAttribute("welcomeFiles");
        defineOperation("addWelcomeFile",
                        new String[] {ModelMBeanImpl.STRING},
                        ModelMBeanImpl.IMPACT_INFO);
        defineOperation("removeWelcomeFile",
                        new String[] {ModelMBeanImpl.STRING},
                        ModelMBeanImpl.IMPACT_INFO);

        defineAttribute("mimeMap");
        defineOperation("setMimeMapping",new String[] {ModelMBeanImpl.STRING, ModelMBeanImpl.STRING}, ModelMBeanImpl.IMPACT_ACTION);

        
        defineAttribute("statsOn");
        defineAttribute("statsOnMs");
        defineOperation("statsReset", ModelMBeanImpl.IMPACT_ACTION);
        defineAttribute("requests");
        defineAttribute("requestsActive");
        defineAttribute("requestsActiveMax");
        defineAttribute("responses1xx");
        defineAttribute("responses2xx");
        defineAttribute("responses3xx");
        defineAttribute("responses4xx");
        defineAttribute("responses5xx");

        defineOperation("stop",new String[] {"java.lang.Boolean.TYPE"}, ModelMBeanImpl.IMPACT_ACTION);

        defineOperation("destroy",
                        ModelMBeanImpl.IMPACT_ACTION);

        defineOperation("setInitParameter",
                        new String[] {ModelMBeanImpl.STRING, ModelMBeanImpl.STRING},
                        ModelMBeanImpl.IMPACT_ACTION);
        defineOperation("getInitParameter",
                        new String[] {ModelMBeanImpl.STRING},
                        ModelMBeanImpl.IMPACT_INFO);
        defineOperation("getInitParameterNames",
                        ModelMBeanImpl.NO_PARAMS,
                        ModelMBeanImpl.IMPACT_INFO);

        defineOperation("setAttribute",new String[] {ModelMBeanImpl.STRING, ModelMBeanImpl.OBJECT}, ModelMBeanImpl.IMPACT_ACTION);
        defineOperation("getAttribute",new String[] {ModelMBeanImpl.STRING}, ModelMBeanImpl.IMPACT_INFO);
        defineOperation("getAttributeNames", ModelMBeanImpl.NO_PARAMS, ModelMBeanImpl.IMPACT_INFO);
        defineOperation("removeAttribute",new String[] {ModelMBeanImpl.STRING}, ModelMBeanImpl.IMPACT_ACTION);

        defineOperation("addHandler",new String[] {"HttpHandler"}, ModelMBeanImpl.IMPACT_ACTION);
        defineOperation("addHandler",new String[] {ModelMBeanImpl.INT,"HttpHandler"}, ModelMBeanImpl.IMPACT_ACTION);
        defineOperation("removeHandler",new String[] {ModelMBeanImpl.INT}, ModelMBeanImpl.IMPACT_ACTION);


        _httpContext=(HttpContext)getManagedResource();
        
        _httpContext.addEventListener(new LifeCycleListener()
                {

                    public void lifeCycleStarting (LifeCycleEvent event)
                    {}

                    public void lifeCycleStarted (LifeCycleEvent event)
                    {
                        getHandlers();                     
                    }

                    public void lifeCycleFailure (LifeCycleEvent event)
                    {}

                    public void lifeCycleStopping (LifeCycleEvent event)
                    {}

                    public void lifeCycleStopped (LifeCycleEvent event)
                    {
                        destroyHandlers();
                    }
            
                });
    }


    /* ------------------------------------------------------------ */
    protected ObjectName newObjectName(MBeanServer server)
    {
        ObjectName oName=super.newObjectName(server);
        String context=_httpContext.getContextPath();
        if (context.length()==0)
            context="/";
        try{oName=new ObjectName(oName+",context="+context);}
        catch(Exception e){log.warn(LogSupport.EXCEPTION,e);}
        return oName;
    }

    /* ------------------------------------------------------------ */
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);
        if (ok.booleanValue())
            getHandlers();
    }

    /* ------------------------------------------------------------ */
    public void postDeregister()
    {
        _httpContext=null;
        destroyComponentMBeans(_handlerMap);
        super.postDeregister();
    }

    /* ------------------------------------------------------------ */
    public ObjectName[] getHandlers()
    {
        return getComponentMBeans(_httpContext.getHandlers(),_handlerMap);
    }
    
  
    public void destroyHandlers()
    {
        destroyComponentMBeans(_handlerMap);
    }

    /* ------------------------------------------------------------ */
    public ObjectName getRequestLog()
    {
        Object o = _httpContext.getRequestLog();
        if (o==null)
            return null;
        
        ObjectName[] on=getComponentMBeans(new Object[]{o},_rlMap);
        if (on.length>0)
            return on[0];
        return null;
    }

}


