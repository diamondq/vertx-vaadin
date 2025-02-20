/*
 * The MIT License
 * Copyright © 2016-2019 Marco Collovati (mcollovati@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mcollovati.vertx.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;

import com.github.mcollovati.vertx.vaadin.VertxVaadin;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.BootstrapListener;
import com.vaadin.flow.server.BootstrapPageResponse;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class MockServiceSessionSetup {

    @Mock
    private FileSystem fileSystem;
    @Mock
    private Vertx vertx;
    @Mock
    private VaadinRequest request;
    @Mock
    private VaadinSession session;
    @Mock
    private WebBrowser browser;
    @Mock
    private Context context;
    //@Mock
    //private WrappedHttpSession wrappedSession;
    //@Mock
    //private HttpSession httpSession;
    //@Mock
    //private ServletConfig servletConfig;
    private TestVertxVaadin vertxVaadin;
    private TestVertxVaadinService service;
    private MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();

    public MockServiceSessionSetup() throws Exception {
        this(true);
    }

    public MockServiceSessionSetup(boolean sessionAvailable)
        throws Exception {
        MockitoAnnotations.initMocks(this);
        SharedData sharedData = Mockito.mock(SharedData.class);
        Mockito.when(vertx.sharedData()).thenReturn(sharedData);
        Mockito.when(sharedData.getLocalMap(Matchers.anyString())).thenReturn(Mockito.mock(LocalMap.class));
        Mockito.when(vertx.eventBus()).thenReturn(Mockito.mock(EventBus.class));
        Mockito.when(vertx.fileSystem()).thenReturn(fileSystem);
        Mockito.when(vertx.getOrCreateContext()).thenReturn(context);

        vertxVaadin = new TestVertxVaadin(vertx);

        deploymentConfiguration.setXsrfProtectionEnabled(false);
        //Mockito.when(servletConfig.getServletContext())
        //        .thenReturn(servletContext);


        if (sessionAvailable) {
            Mockito.when(session.getConfiguration())
                .thenReturn(deploymentConfiguration);

            Mockito.when(session.getBrowser()).thenReturn(browser);
            Mockito.when(session.getPushId()).thenReturn("fake push id");
            Mockito.when(session.getLocale()).thenReturn(Locale.ENGLISH);

            //Mockito.when(wrappedSession.getHttpSession())
            //        .thenReturn(httpSession);

            Mockito.when(session.getService()).thenAnswer(i -> service);
            Mockito.when(session.hasLock()).thenReturn(true);
            Mockito.when(session.getPendingAccessQueue())
                .thenReturn(new LinkedBlockingDeque<>());
            //Mockito.when(request.getWrappedSession())
            //        .thenReturn(wrappedSession);
        } else {
            session = null;
        }
        //servlet.init(servletConfig);

        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinService.class, service);
        if (sessionAvailable) {
            CurrentInstance.set(VaadinSession.class, session);
        }

        //Mockito.when(request.getServletPath()).thenReturn("");
        Mockito.when(browser.isEs6Supported()).thenReturn(true);

    }

    public TestVertxVaadinService getService() {
        return service;
    }

    public TestVertxVaadin getVertxVaadin() {
        return vertxVaadin;
    }

    public VaadinSession getSession() {
        return session;
    }

    public MockDeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    public WebBrowser getBrowser() {
        return browser;
    }

    /*
    public ServletContext getServletContext() {
        return servletContext;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public WrappedHttpSession getWrappedSession() {
        return wrappedSession;
    }
    */

    public void cleanup() {
        CurrentInstance.clearAll();
    }

    /*
    public ServletConfig getServletConfig() {
        return servletConfig;
    }
    */

    public void setProductionMode(boolean productionMode) {
        deploymentConfiguration.setProductionMode(productionMode);
    }

    public void setBrowserEs6(boolean browserEs6) {
        Mockito.when(browser.isEs6Supported()).thenReturn(browserEs6);
    }

    public static class TestRouteRegistry extends ApplicationRouteRegistry {

    }

    public class TestVertxVaadinService extends VertxVaadinService {

        private List<DependencyFilter> dependencyFilterOverride;
        private TestRouteRegistry routeRegistry = new TestRouteRegistry();
        private Router router;
        private List<BootstrapListener> bootstrapListeners = new ArrayList<>();

        public TestVertxVaadinService(VertxVaadin vertxVaadin, DeploymentConfiguration deploymentConfiguration) {
            super(vertxVaadin, deploymentConfiguration);
        }

        @Override
        public Iterable<DependencyFilter> getDependencyFilters() {
            if (dependencyFilterOverride != null) {
                return dependencyFilterOverride;
            }
            return super.getDependencyFilters();
        }

        public void setDependencyFilters(
            List<DependencyFilter> dependencyFilters) {
            dependencyFilterOverride = dependencyFilters;
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            if (routeRegistry != null) {
                return routeRegistry;
            }
            return super.getRouteRegistry();
        }

        public void setRouteRegistry(TestRouteRegistry routeRegistry) {
            this.routeRegistry = routeRegistry;
        }

        @Override
        public Router getRouter() {
            if (router != null) {
                return router;
            }
            return super.getRouter();
        }

        public void setRouter(Router router) {
            this.router = router;
        }

        public void addBootstrapListener(BootstrapListener listener) {
            bootstrapListeners.add(listener);
        }

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            bootstrapListeners.forEach(
                listener -> listener.modifyBootstrapPage(response));

            super.modifyBootstrapPage(response);
        }

    }

    public class TestVertxVaadin extends VertxVaadin {

        private Function<String, String> resolveOverride;
        private Function<String, Boolean> resourceFoundOverride;
        private Function<String, InputStream> resourceAsStreamOverride;

        protected TestVertxVaadin(Vertx vertx) {
            super(vertx, new JsonObject());
        }

        @Override
        protected VertxVaadinService createVaadinService() {
            service = new TestVertxVaadinService(this, deploymentConfiguration);
            return service;
        }

        /*

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }
    */
        public void setResolveOverride(
            Function<String, String> resolveOverride) {
            this.resolveOverride = resolveOverride;
        }

        public void setResourceFoundOverride(
            Function<String, Boolean> resourceFoundOverride) {
            this.resourceFoundOverride = resourceFoundOverride;
        }

        public void setResourceAsStreamOverride(
            Function<String, InputStream> resourceAsStreamOverride) {
            this.resourceAsStreamOverride = resourceAsStreamOverride;
        }
/*
        @Override
        public InputStream getResourceAsStream(String path) {
            if (resourceAsStreamOverride != null) {
                return resourceAsStreamOverride.apply(path);
            }

            return super.getResourceAsStream(path);
        }

        @Override
        public String resolveResource(String url) {
            if (resolveOverride != null) {
                return resolveOverride.apply(url);
            }
            return super.resolveResource(url);
        }

        @Override
        public boolean isResourceFound(String resolvedUrl) {
            if (resourceFoundOverride != null) {
                return resourceFoundOverride.apply(resolvedUrl);
            }
            return super.isResourceFound(resolvedUrl);
        }

        @Override
        boolean isInServletContext(String resolvedUrl) {
            if (resourceFoundOverride != null) {
                return resourceFoundOverride.apply(resolvedUrl);
            }
            return super.isInServletContext(resolvedUrl);
        }
        */

        public void addResource(String path) {
            addResource(path, "This is " + path);
        }

        public void addResource(String path, String contents) {
            String relativePath = path;
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            Mockito.when(fileSystem.existsBlocking(relativePath)).thenReturn(true);
            //Mockito.when(fileSystem.r)        .thenReturn(url);
            Mockito.when(fileSystem.readFileBlocking(relativePath))
                .thenAnswer(i -> Buffer.buffer(contents.getBytes(StandardCharsets.UTF_8)));
        }

        public void addWebJarResource(String webjarPath) {
            // Webjars map /frontend/bower_components/foo/bar.html to
            // /META-INF/resources/webjars/webjars/foo/bar.html
            addResource("META-INF/resources/webjars/" + webjarPath);
        }

        /*
        public void verifyServletContextResourceLoadedOnce(String resource) {
            Mockito.verify(servlet.getServletContext())
                    .getResourceAsStream(resource);

        }
*/
    }
}
