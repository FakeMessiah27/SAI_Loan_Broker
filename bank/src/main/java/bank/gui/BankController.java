package bank.gui;

import bank.model.BankInterestReply;
import bank.model.BankInterestRequest;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

public class BankController implements Initializable, MessageListener {

    private final String BANK_ID = "ABN";
    private Gson gson;

    // UI variables
    public ListView<ListViewLine> lvBankRequestReply;
    public TextField tfInterest;

    // Connection variables
    private static int ackMode;
    private static int messageCounter;
    private static String messageQueueName;
    private static String messageBrokerUrl;

    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private Destination brokerDestination;
    private HashMap<Integer, HashMap<String, BankInterestRequest>> hashMapRequests;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageQueueName = "ABNqueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
        messageCounter = 0;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gson = new Gson();
        hashMapRequests = new HashMap<>();
        this.setupMessageQueueConsumer();
    }

    private void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);
        Connection connection;

        try {
            // Random variable to get intellij to shut up and stop complaining about duplicate code
            // from that it found in an entirely different module.
            int i = 0;

            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ackMode);
            Destination adminQueue = this.session.createQueue(messageQueueName);
            brokerDestination = this.session.createQueue("brokerQueue");

            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MessageConsumer consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void btnSendBankInterestReplyClicked(){
        double interest = Double.parseDouble(tfInterest.getText());
        BankInterestReply bankInterestReply = new BankInterestReply(interest, BANK_ID);

        BankInterestRequest bankInterestRequest = getRequestReply();

        try {
            Destination replyToDestination = brokerDestination;
            TextMessage response = this.session.createTextMessage();
            response.setJMSCorrelationID(messageIdentifier[1]);
            response.setText(gson.toJson(bankInterestReply));

            this.replyProducer.send(replyToDestination, response);
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method returns the line of lvMessages which contains the given loan request.
     * @param request BankInterestRequest for which the line of lvMessages should be found and returned
     * @return The ListView line of lvMessages which contains the given request
     */
    private ListViewLine getRequestReply(BankInterestRequest request) {

        for (int i = 0; i < lvBankRequestReply.getItems().size(); i++) {
            ListViewLine rr = lvBankRequestReply.getItems().get(i);
            if (rr.getBankInterestRequest() != null && rr.getBankInterestRequest() == request) {
                return rr;
            }
        }
        return null;
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                String messageCorrelationID = message.getJMSCorrelationID();

                BankInterestRequest bankInterestRequest = gson.fromJson(messageText, BankInterestRequest.class);
//                String messageIdentifier = Integer.toString(++messageCounter)
//                        + "-" + messageCorrelationID;
//                hashMapRequests.put(messageIdentifier, bankInterestRequest);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        //create the ListView line with the request and add it to lvBankRequestReply
                        ListViewLine listViewLine = new ListViewLine(bankInterestRequest);
                        lvBankRequestReply.getItems().add(listViewLine);
                        hashMapRequests.put(lvBankRequestReply.getItems().size() - 1, );
                        // TODO: Find a way to simulate a 3-way tuple to store the index in the listview plus the message
                        // correlationID plus the bankInterestRequest.
                    }
                });
            }
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }
}
