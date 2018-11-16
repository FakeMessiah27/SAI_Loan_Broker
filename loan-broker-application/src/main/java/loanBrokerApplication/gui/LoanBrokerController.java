package loanBrokerApplication.gui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import loanBrokerApplication.model.BankInterestRequest;
import loanBrokerApplication.model.LoanRequest;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

public class LoanBrokerController implements Initializable, MessageListener {

    private Gson gson;

    // UI variables
    public ListView lvMessages;

    // Connection variables
    private static int ackMode;
    private static String clientListenQueueName;
    private static String bankListenQueueName;
    private static String messageBrokerUrl;
    private static int correlationId;

    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private MessageProducer bankProducer;
    private List<Destination> replyToDestinations;
    private HashMap<String, LoanRequest> hashMapRequests;
    private HashMap<Integer, Message> hashMapMessageIDs;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        clientListenQueueName = "loanRequestsQueue";
        bankListenQueueName = "brokerQueue";

        ackMode = Session.AUTO_ACKNOWLEDGE;
        correlationId = 0;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gson = new Gson();
        replyToDestinations = new ArrayList<>();
        hashMapRequests = new HashMap<>();

        this.setupMessageQueueConsumer();
    }

    private void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);
        Connection connection;

        try {
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ackMode);
            Destination clientQueue = this.session.createQueue(clientListenQueueName);
            Destination bankQueue = this.session.createQueue(bankListenQueueName);

            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

            Destination abnAmroDestination = session.createQueue("ABNQueue");
            this.bankProducer = this.session.createProducer(abnAmroDestination);
            this.bankProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MessageConsumer clientConsumer = this.session.createConsumer(clientQueue);
            MessageConsumer bankConsumer = this.session.createConsumer(bankQueue);

            clientConsumer.setMessageListener(this);
            bankConsumer.setMessageListener(this);
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                Destination replyToDestination = message.getJMSReplyTo();
                if (!replyToDestinations.contains(replyToDestination)) {
                    replyToDestinations.add(replyToDestination);
                }

                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                String messageCorrelationID = message.getJMSCorrelationID();

                LoanRequest loanRequest = gson.fromJson(messageText, LoanRequest.class);
                String messageIdentifier = Integer.toString(replyToDestinations.indexOf(replyToDestination) + 1)
                        + "-" + messageCorrelationID;
                hashMapRequests.put(messageIdentifier, loanRequest);
                this.sendBankRequest(loanRequest);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        //create the ListView line with the request and add it to lvMessages
                        ListViewLine listViewLine = new ListViewLine(loanRequest);
                        lvMessages.getItems().add(listViewLine);
                    }
                });
            }
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }

    private void sendBankRequest(LoanRequest loanRequest) {
        try {
            BankInterestRequest bankInterestRequest = new BankInterestRequest(loanRequest.getAmount(),
                    loanRequest.getTime());

            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText(gson.toJson(bankInterestRequest));

            txtMessage.setJMSCorrelationID(Integer.toString(++correlationId));
            this.bankProducer.send(txtMessage);

            hashMapMessageIDs.put(correlationId, txtMessage);
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }
}
