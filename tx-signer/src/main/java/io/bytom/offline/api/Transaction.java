package io.bytom.offline.api;

import io.bytom.offline.common.Utils;
import io.bytom.offline.exception.MapTransactionException;
import io.bytom.offline.exception.SerializeTransactionException;
import io.bytom.offline.exception.SignTransactionException;
import io.bytom.offline.types.*;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liqiang on 2018/10/24.
 */

public class Transaction {

    private String txID;
    /**
     * version
     */
    private Integer version;
    /**
     * size
     */
    private Integer size;
    /**
     * time_range
     */
    private Integer timeRange;

    /**
     * List of specified inputs for a transaction.
     */
    private List<BaseInput> inputs;

    /**
     * List of specified outputs for a transaction.
     */
    private List<Output> outputs;

    /**
     * fee
     */
    private Integer fee;

    /**
     * changeControlProgram
     */
    private String changeControlProgram;

    String btmAssetID = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    public Transaction(Builder builder, String changeControlProgram) {
        this.inputs = builder.inputs;
        this.outputs = builder.outputs;
        this.version = builder.version;
        this.size = builder.size;
        this.timeRange = builder.timeRange;
        this.changeControlProgram = changeControlProgram;

        this.validate();
        this.estimateFee();
        this.mapTx();
        this.sign();
    }

    public static class Builder {

        private Integer version = 1;

        private Integer size = 0;

        private Integer timeRange;

        private List<BaseInput> inputs;
        private List<Output> outputs;


        public Builder() {
            this.inputs = new ArrayList<>();
            this.outputs = new ArrayList<>();
        }

        public Builder addInput(BaseInput input) {
            this.inputs.add(input);
            return this;
        }

        public Builder addOutput(Output output) {
            this.outputs.add(output);
            return this;
        }

        public Builder setTimeRange(int timeRange) {
            this.timeRange = timeRange;
            return this;
        }

        public Transaction build(String changeControlProgram) {
            return new Transaction(this, changeControlProgram);
        }
    }

    private void sign() {
        for (BaseInput input : inputs) {
            try {
                input.buildWitness(txID);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SignTransactionException(e);
            }
        }
    }

    public String rawTransaction() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(7);

            Utils.writeVarint(version, stream);

            Utils.writeVarint(timeRange, stream);

            Utils.writeVarint(inputs.size(), stream);
            for (BaseInput input : inputs) {
                stream.write(input.serializeInput());
            }

            Utils.writeVarint(outputs.size(), stream);
            for (Output output : outputs) {
                stream.write(output.serializeOutput());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new SerializeTransactionException(e);
        }
        return Hex.toHexString(stream.toByteArray());
    }

    private void validate() {
        if (version == null) {
            throw new IllegalArgumentException("the version of transaction must be specified.");
        }
        if (timeRange == null) {
            throw new IllegalArgumentException("the time range of transaction must be specified.");
        }
        if (size == null) {
            throw new IllegalArgumentException("the size range of transaction must be specified.");
        }

        for (BaseInput input : inputs) {
            input.validate();
        }
    }

    private void mapTx() {
        Map<Hash, Entry> entryMap = new HashMap<>();
        ValueSource[] muxSources = new ValueSource[inputs.size()];
        List<InputEntry> inputEntries = new ArrayList<>();

        try {
            for (int i = 0; i < inputs.size(); i++) {
                BaseInput input = inputs.get(i);
                InputEntry inputEntry = input.toInputEntry(entryMap, i);
                Hash spendID = addEntry(entryMap, inputEntry);
                input.setInputID(spendID.toString());

                muxSources[i] = new ValueSource(spendID, input.getAssetAmount(), 0);
                inputEntries.add(inputEntry);
            }

            Mux mux = new Mux(muxSources, new Program(1, new byte[]{0x51}));
            Hash muxID = addEntry(entryMap, mux);
            for (InputEntry inputEntry : inputEntries) {
                inputEntry.setDestination(muxID, inputEntry.getOrdinal(), entryMap);
            }

            List<Hash> resultIDList = new ArrayList<>();
            for (int i = 0; i < outputs.size(); i++) {
                Output output = outputs.get(i);

                AssetAmount amount = new AssetAmount(new AssetID(output.getAssetId()), output.getAmount());
                ValueSource src = new ValueSource(muxID, amount, i);

                Hash resultID;
                if (output.getControlProgram().startsWith("6a")) {
                    Retirement retirement = new Retirement(src, i);
                    resultID = addEntry(entryMap, retirement);
                } else {
                    Program program = new Program(1, Hex.decode(output.getControlProgram()));
                    OutputEntry oup = new OutputEntry(src, program, i);
                    resultID = addEntry(entryMap, oup);
                }

                resultIDList.add(resultID);
                output.setId(resultID.toString());

                ValueDestination destination = new ValueDestination(resultID, src.getValue(), 0);
                mux.getWitnessDestinations().add(destination);
            }

            TxHeader txHeader = new TxHeader(version, size, timeRange, resultIDList.toArray(new Hash[]{}));
            Hash txID = addEntry(entryMap, txHeader);
            this.txID = txID.toString();

        } catch (Exception e) {
            e.printStackTrace();
            throw new MapTransactionException(e);
        }
    }

    private Hash addEntry(Map<Hash, Entry> entryMap, Entry entry) {
        Hash id = entry.entryID();
        entryMap.put(id, entry);
        return id;
    }

    private void estimateFee() {
        Long inputAmount = 0L;
        for (BaseInput input : inputs) {
            if (input.getAssetId().equals(btmAssetID)) {
                inputAmount += input.getAmount();
            }
        }

        Long outputAmount = 0L;
        for (Output output : outputs) {
            if (output.getAssetId().equals(btmAssetID)) {
                outputAmount += output.getAmount();
            }
        }

        Long changeAmount = inputAmount - outputAmount;
        Output changeOutput = new Output(btmAssetID, changeAmount, this.changeControlProgram);
        this.outputs.add(changeOutput);

        String rawTransaction = this.rawTransaction();

        BigDecimal defaultBaseRate = new BigDecimal(100000);
        Long flexibleGas = 1800L;
        Long vmGasRate = 200L;
        Long storageGasRate = 1L;

        // baseTxSize
        int baseTxSize = Hex.decode(rawTransaction).length;

        // estimateSignSize
        Long signSize = 0L;
        Long baseWitnessSize = 300L;

        for (BaseInput input : this.inputs) {
            //signSize += int64(t.Quorum) * baseWitnessSize;
            //t.Quorum = 1
            signSize += baseWitnessSize;
        }

        Long totalTxSizeGas = storageGasRate * (baseTxSize + signSize);

        Long totalP2WPKHGas = 0L;
        Long totalP2WSHGas = 0L;
        Long baseP2WPKHGas = 1419L;

        for (BaseInput input : this.inputs) {
            //if segwit.IsP2WPKHScript(resOut.ControlProgram.Code) {
            //   totalP2WPKHGas += baseP2WPKHGas
            //}
            if (input.getProgram().length() == 44) {
                totalP2WPKHGas += baseP2WPKHGas;
            }
            //TODO
            //else if (input.getProgram().length() == 66) {
            //totalP2WSHGas += estimateP2WSHGas(sigInst)
            //}
        }

        // total estimate gas
        Long totalGas = totalTxSizeGas + totalP2WPKHGas + totalP2WSHGas + flexibleGas;

        // rounding totalNeu with base rate 100000
        //totalNeu := float64(totalGas*consensus.VMGasRate) / defaultBaseRate
        //roundingNeu := math.Ceil(totalNeu)
        //estimateNeu := int64(roundingNeu) * int64(defaultBaseRate)
        BigDecimal totalGasDecimal = new BigDecimal(totalGas * vmGasRate);
        totalGasDecimal = totalGasDecimal.divide(defaultBaseRate);
        totalGasDecimal = totalGasDecimal.setScale(0, BigDecimal.ROUND_UP);
        totalGasDecimal = totalGasDecimal.multiply(defaultBaseRate);

        this.fee = totalGasDecimal.intValue();
        if (changeAmount - this.fee > 10000L) {
            changeOutput.setAmount(changeAmount - this.fee);
        } else {
            this.outputs.remove(this.outputs.size()-1);
        }
    }

    public String getTxID() {
        return txID;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getTimeRange() {
        return timeRange;
    }

    public List<BaseInput> getInputs() {
        return inputs;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    public Integer getFee() {
        return fee;
    }
}
