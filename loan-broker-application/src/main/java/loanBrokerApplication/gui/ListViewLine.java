package loanBrokerApplication.gui;


import loanBrokerApplication.model.BankInterestRequest;
import loanBrokerApplication.model.LoanRequest;

/**
 * This class is an item/line for a ListView. It makes it possible to put both BankInterestRequest and LoanRequest object in one item in a ListView.
 */
class ListViewLine {

    private LoanRequest loanRequest;
    private BankInterestRequest bankInterestRequest;

    public ListViewLine(LoanRequest loanRequest) {
        setLoanRequest(loanRequest);
        setBankInterestRequest(null);
    }

    public LoanRequest getLoanRequest() {
        return loanRequest;
    }

    private void setLoanRequest(LoanRequest loanRequest) {
        this.loanRequest = loanRequest;
    }

    public BankInterestRequest getBankInterestRequest() {
        return bankInterestRequest;
    }

    public void setBankInterestRequest(BankInterestRequest bankInterestRequest) {
        this.bankInterestRequest = bankInterestRequest;
    }

    /**
     * This method defines how one line is shown in the ListView.
     * @return
     *  a) if BankInterestReply is null, then this item will be shown as "loanRequest.toString ---> waiting for loan reply..."
     *  b) if BankInterestReply is not null, then this item will be shown as "loanRequest.toString ---> bankInterestRequest.toString"
     */
    @Override
    public String toString() {
        return loanRequest.toString() + "  --->  " + ((bankInterestRequest !=null)? bankInterestRequest.toString():"waiting for loan reply...");
    }

}
