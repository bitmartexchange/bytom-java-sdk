package io.bytom.api;

import com.google.gson.annotations.SerializedName;
import io.bytom.common.Utils;
import io.bytom.exception.BytomException;
import io.bytom.http.Client;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

public class Block {

    public String hash;

    public Integer size;

    public Integer version;

    public Integer height;

    @SerializedName("previous_block_hash")
    public String previousBlockHash;

    public Integer timestamp;

    public Integer nonce;

    public long bits;

    public String difficulty;

    @SerializedName("transaction_merkle_root")
    public String transactionsMerkleRoot;

    @SerializedName("transaction_status_hash")
    public String transactionStatusHash;

    public List<BlockTx> transactions;


    private static Logger logger = Logger.getLogger(Block.class);

    public String toJson() {
        return Utils.serializer.toJson(this);
    }

    /**
     * Call get-block-count api
     *
     * @param client
     * @return
     * @throws BytomException
     */
    public static Integer getBlockCount(Client client) throws BytomException {
        Integer blockCount =
                client.requestGet("get-block-count", null, "block_count", Integer.class);

        logger.info("get-block-count:"+blockCount);
        return blockCount;
    }

    /**
     * Call get-block-hash api
     *
     * @param client
     * @return
     * @throws BytomException
     */
    public static String getBlockHash(Client client) throws BytomException {
        String blockHash =
                client.requestGet("get-block-hash", null, "block_hash", String.class);

        logger.info("get-block-hash:"+blockHash);

        return blockHash;
    }

    public static class QueryBuilder {

        /**
         * block_height, height of block.
         */
        @SerializedName("block_height")
        public int blockHeight;
        /**
         * block_hash, hash of block.
         */
        @SerializedName("block_hash")
        public String blockHash;

        public QueryBuilder setBlockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        public QueryBuilder setBlockHash(String blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        /**
         * Call get-block api
         *
         * @param client
         * @return
         * @throws BytomException
         */
        public Block getBlock(Client client) throws BytomException {

            Block block = client.request("get-block", this, Block.class);

            logger.info("get-block:");
            logger.info(block.toJson());

            return block;
        }

        /**
         * Call get-block-header api
         *
         * @param client
         * @return
         * @throws BytomException
         */
        public BlockHeader getBlockHeader(Client client) throws BytomException {
            BlockHeader blockHeader =
                    client.request("get-block-header", this, BlockHeader.class);

            logger.info("get-block-header:");
            logger.info(blockHeader.toJson());

            return blockHeader;
        }

        /**
         * Call get-difficulty api
         *
         * @param client
         * @return
         * @throws BytomException
         */
        public BlockDifficulty getBlockDifficulty(Client client) throws BytomException {
            BlockDifficulty blockDifficulty =
                    client.request("get-difficulty", this, BlockDifficulty.class);

            logger.info("get-difficulty:");
            logger.info(blockDifficulty.toJson());

            return blockDifficulty;
        }

        /**
         * Call get-hash-rate api
         *
         * @param client
         * @return
         * @throws BytomException
         */
        public BlockHashRate getHashRate(Client client) throws BytomException {
            BlockHashRate blockHashRate =
                    client.request("get-hash-rate", this, BlockHashRate.class);

            logger.info("get-hash-rate:");
            logger.info(blockHashRate.toJson());

            return blockHashRate;
        }

    }

    public static class BlockTx {
        /**
         * Unique identifier, or transaction hash, of a transaction.
         */
        public String id;

        /**
         * version
         */
        public Integer version;

        /**
         * size
         */
        public Integer size;
        /**
         * time_range
         */
        @SerializedName("time_range")
        public Integer timeRange;

        /**
         * status
         */
        @SerializedName("status_fail")
        public boolean statusFail;

        /**
         * mux_id
         */
        @SerializedName("mux_id")
        public String muxId;

        /**
         * List of specified inputs for a transaction.
         */
        @SerializedName("inputs")
        public List<AnnotatedInput> inputs;

        /**
         * List of specified outputs for a transaction.
         */
        @SerializedName("outputs")
        public List<AnnotatedOutput> outputs;
    }

    public static class AnnotatedInput {

        /**
         * The number of units of the asset being issued or spent.
         */
        public Integer amount;

        /**
         * inputs param
         */
        public String arbitrary;

        /**
         * The definition of the asset being issued or spent (possibly null).
         */
        @SerializedName("asset_definition")
        public Map<String, Object> assetDefinition;

        /**
         * The id of the asset being issued or spent.
         */
        @SerializedName("asset_id")
        public String assetId;

        /**
         * The type of the input.<br>
         * Possible values are "issue" and "spend".
         */
        public String type;

        public String address;

        @SerializedName("spent_output_id")
        public String spentOutputId;

        @SerializedName("input_id")
        public String inputId;

        @SerializedName("control_program")
        public String controlProgram;

        @SerializedName("witness_arguments")
        public List<String> witnessArguments;
    }

    public static class AnnotatedOutput {

        /**
         * The number of units of the asset being controlled.
         */
        public long amount;

        /**
         * The definition of the asset being controlled (possibly null).
         */
        @SerializedName("asset_definition")
        public Map<String, Object> assetDefinition;

        /**
         * The id of the asset being controlled.
         */
        @SerializedName("asset_id")
        public String assetId;

        /**
         * The control program which must be satisfied to transfer this output.
         */
        @SerializedName("control_program")
        public String controlProgram;

        /**
         * The id of the output.
         */
        @SerializedName("id")
        public String id;

        /**
         * The output's position in a transaction's list of outputs.
         */
        public Integer position;

        /**
         * The type the output.<br>
         * Possible values are "control" and "retire".
         */
        public String type;

        public String address;
    }

    public static class BlockHeader {

        @SerializedName("block_header")
        public String blockHeader;

        @SerializedName("reward")
        public Integer reward;

        public String toJson() {
            return Utils.serializer.toJson(this);
        }

    }

    public static class BlockDifficulty {
        public String hash;
        public Integer height;
        public Integer bits;
        public String difficulty;

        public String toJson() {
            return Utils.serializer.toJson(this);
        }


    }

    public static class BlockHashRate {
        public String hash;
        public Integer height;
        public Integer hash_rate;

        public String toJson() {
            return Utils.serializer.toJson(this);
        }

    }
}
