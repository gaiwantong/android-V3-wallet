package info.blockchain.wallet.send;

import com.google.bitcoin.core.Transaction;

import info.blockchain.wallet.OpCallback;

public class Spendable    {

    private Transaction tx = null;
    private OpCallback opc = null;
    private String note = null;
    private boolean isHD = false;
    private boolean sentChange = false;
    private int accountIdx = -1;

    public Spendable(Transaction tx, OpCallback opc, String note, boolean isHD, boolean sentChange, int accountIdx)  {
        this.tx = tx;
        this.opc = opc;
        this.note = note;
        this.isHD = isHD;
        this.sentChange = sentChange;
        this.accountIdx = accountIdx;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    public OpCallback getOpCallback() {
        return opc;
    }

    public void setOpCallback(OpCallback opc) {
        this.opc = opc;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isHD() {
        return isHD;
    }

    public void setHD(boolean isHD) {
        this.isHD = isHD;
    }

    public boolean sentChange() {
        return sentChange;
    }

    public void setSentChange(boolean sentChange) {
        this.sentChange = sentChange;
    }

    public int getAccountIdx() {
        return accountIdx;
    }

    public void setAccountIdx(int accountIdx) {
        this.accountIdx = accountIdx;
    }
}
