package vaadin.bugs.upload;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.OutputStream;

public class VaadinUploadTest
{
    public static void main(String[] args) throws Exception
    {
        boolean assertionsEnabled = areAssertionsEnabled();
        if (!assertionsEnabled) {
            throw new IllegalStateException("Start this test with assertions enabled: add '-ea' to the JVM arguments");
        }

        TestServlet servlet = new TestServlet();
        ServletHolder sh = new ServletHolder(servlet);
        sh.setAsyncSupported(true);
        sh.setInitParameter("productionMode", "false");
        sh.setInitParameter("org.atmosphere.websocket.suppressJSR356", "true");

        Server server = new Server(9090);
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        handler.addServlet(sh, "/*");
        server.setHandler(handler);

        server.start();

        server.join();
    }

    private static boolean areAssertionsEnabled()
    {
        try {
            assert false : "testing assertions";
            return false;
        } catch (AssertionError e) {
            return true;
        }
    }

    @Push(value = PushMode.MANUAL, transport = Transport.LONG_POLLING)
    public static class TestUI extends UI
    {

        @Override
        protected void init(VaadinRequest request)
        {
            getPage().setTitle("Upload Testing");

            ProgressBar progressBar = new ProgressBar(0.0f);
            progressBar.setVisible(false);
            Upload uploadButton = new Upload(null, (filename, mimeType) -> new NullOutputStream());
            uploadButton.addStartedListener(started -> {
                progressBar.setValue(0.0f);
                progressBar.setVisible(true);
                if (getPushConfiguration().getPushMode() == PushMode.MANUAL) {
                    push();
                }
            });
            uploadButton.addFinishedListener(finished -> {
                progressBar.setVisible(false);
                if (getPushConfiguration().getPushMode() == PushMode.MANUAL) {
                    push();
                }
            });
            uploadButton.addProgressListener((Upload.ProgressListener) (readBytes, contentLength) -> {
                progressBar.setValue(value(readBytes, contentLength));
                if (getPushConfiguration().getPushMode() == PushMode.MANUAL) {
                    push();
                }
            });
            Button cancelButton = new Button("Cancel", event -> uploadButton.interruptUpload());
            VerticalLayout components = new VerticalLayout();
            components.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
            components.addComponents(progressBar, uploadButton, cancelButton);
            components.setWidth(100, Unit.PERCENTAGE);

            setContent(components);
        }

        private float value(float readBytes, float contentLength)
        {
            return readBytes / contentLength;
        }
    }

    @VaadinServletConfiguration(productionMode = false, ui = TestUI.class)
    public static class TestServlet extends VaadinServlet {
    }

    private static class NullOutputStream extends OutputStream
    {
        @Override
        public void write(int b)
        {
            //don't save it, we just want to test cancelling
        }
    }
}


