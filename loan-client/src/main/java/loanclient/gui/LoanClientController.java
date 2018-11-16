package loanclient.gui;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import loanclient.model.LoanRequest;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

public class LoanClientController implements Initializable, MessageListener {

    private Gson gson;

    // UI variables
    public TextField tfSsn;
    public TextField tfAmount;
    public TextField tfTime;
    public ListView<ListViewLine> lvLoanRequestReply;

    // Connection variables
    private static int ackMode;
    private static String clientQueueName;
    private static int correlationId;

    private boolean transacted = false;
    private MessageProducer producer;
    private ActiveMQConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Destination tempDest;
    private HashMap<Integer, Message> hashMapMessageIDs;

    static {
        clientQueueName = "loanRequestsQueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
        correlationId = 0;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gson = new Gson();
        tfSsn.setText("123456");
        tfAmount.setText("80000");
        tfTime.setText("30");

        connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        hashMapMessageIDs = new HashMap<>();

        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(transacted, ackMode);
            Destination adminQueue = session.createQueue(clientQueueName);

            this.producer = session.createProducer(adminQueue);
            this.producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            tempDest = session.createTemporaryQueue();
            MessageConsumer responseConsumer = session.createConsumer(tempDest);

            responseConsumer.setMessageListener(this);
        }
        catch (JMSException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void btnSendLoanRequestClicked(){
        // create the BankInterestRequest
        int ssn = Integer.parseInt(tfSsn.getText());
        int amount = Integer.parseInt(tfAmount.getText());
        int time = Integer.parseInt(tfTime.getText());
        LoanRequest loanRequest = new LoanRequest(ssn,amount,time);

        try {
            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText(gson.toJson(loanRequest));

            txtMessage.setJMSReplyTo(tempDest);
            txtMessage.setJMSCorrelationID(Integer.toString(++correlationId));
            this.producer.send(txtMessage);

            hashMapMessageIDs.put(correlationId, txtMessage);

            //create the ListView line with the request and add it to lvLoanRequestReply
            ListViewLine listViewLine = new ListViewLine(loanRequest);
            lvLoanRequestReply.getItems().add(listViewLine);
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
    private ListViewLine getRequestReply(LoanRequest request) {

        for (int i = 0; i < lvLoanRequestReply.getItems().size(); i++) {
            ListViewLine rr =  lvLoanRequestReply.getItems().get(i);
            if (rr.getLoanRequest() != null && rr.getLoanRequest() == request) {
                return rr;
            }
        }

        return null;
    }

    @Override
    public void onMessage(Message message) {

    }
}
