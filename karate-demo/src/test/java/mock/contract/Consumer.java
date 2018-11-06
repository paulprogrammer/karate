package mock.contract;

import com.intuit.karate.JsonUtils;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class Consumer implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private final String paymentServiceUrl;
    private final String proxyHost;
    private final Integer proxyPort;
    private final List<Shipment> shipments = new ArrayList();
    private final QueueConsumer queueConsumer;

    public Consumer(String paymentServiceUrl, String queueName) {
        this(paymentServiceUrl, null, null, queueName);
    }

    public Consumer(String paymentServiceUrl, String proxyHost, Integer proxyPort, String queueName) {
        this.paymentServiceUrl = paymentServiceUrl;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        queueConsumer = new QueueConsumer(queueName);
        queueConsumer.setMessageListener(this);
    }

    private HttpURLConnection getConnection(String path) throws Exception {
        URL url = new URL(paymentServiceUrl + path);
        if (proxyHost != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            return (HttpURLConnection) url.openConnection(proxy);
        } else {
            return (HttpURLConnection) url.openConnection();
        }
    }

    public Payment create(Payment payment) {
        try {
            HttpURLConnection con = getConnection("/payments");
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            String json = JsonUtils.toJson(payment);
            IOUtils.write(json, con.getOutputStream(), "utf-8");
            int status = con.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("status code was " + status);
            }
            String content = IOUtils.toString(con.getInputStream(), "utf-8");
            return JsonUtils.fromJson(content, Payment.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Shipment> getShipments() {
        return shipments;
    }        

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage tm = (TextMessage) message;
            String json = tm.getText();
            logger.info("*** received message: {}", json);
            Shipment shipment = JsonUtils.fromJson(json, Shipment.class);
            shipments.add(shipment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void waitUntilFirstMessage() {
        QueueUtils.waitUntilCondition(200, () -> !shipments.isEmpty());
    }
    
    public void stopQueueConsumer() {
        queueConsumer.stop();        
    }

}
