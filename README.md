# jtminer
Java block miner for Thought Network

This is an experimental Cuckoo Cycle-based solo miner for the Thought Network.
This is a work in progress, not production-ready code, and is being provided as-is for those who wish to participate in the testing of the Thought Network blockchain prior to the release of the cuckoo cycle POW on the main chain and the subsequent beginning of public mining on the blockchain.

### Building ###
Building jtminer requires Java 8 (or higher) Development Kit and Maven.

jtminer depends on the [thought4j RPC library] (https://github.com/thoughtnetwork/jtminer).  
Clone and install thought4j before building jtminer.

`git clone https://github.com/thoughtnetwork/jtminer.git`
`cd thought4j`
`mvn install`

Once thought4j is installed, clone and build jtminer.

`git clone https://github.com/thoughtnetwork/jtminer`
`cd jtminer`
`mvn install`

The build will produce a shaded jar file in the target directory of the repository.  

### Running ###
Mining with jtminer requires a running Thought wallet or Thought daemon on testnet with the RPC server enabled.  Binary distributions of a testnet-only daemon and wallet can be found at PUT_LINK_HERE.




