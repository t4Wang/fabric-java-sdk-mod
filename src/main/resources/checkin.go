package main

import (
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
)

var logger = shim.NewLogger("chicken")

type SmartContract struct {
}

const (
	OK    = 200
	ERROR = 500
)

// Init is called when the smart contract is instantiated
func (s *SmartContract) Init(APIstub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

func (s *SmartContract) Invoke(APIstub shim.ChaincodeStubInterface) sc.Response {
	function, args := APIstub.GetFunctionAndParameters()

	if function == "checkin" {
		return s.checkin(APIstub, args)
	}
	if function == "get" {
		return s.get(APIstub, args)
	}

	return shim.Error("Invalid Smart Contract function name.")
}

func (s *SmartContract) checkin(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 4 {
		return shim.Error("Incorrect number of arguments, expecting 4")
	}

	// Extract the args
	rfid := args[0]
	readerid := args[1]
	checkin := args[2]
	cinfo := args[3]

	txid := APIstub.GetTxID()
	compositeIndexName := "rfid~readerid~checkin~txID"

	compositeKey, compositeErr := APIstub.CreateCompositeKey(compositeIndexName, []string{rfid, readerid, checkin, txid})
	if compositeErr != nil {
		return shim.Error(fmt.Sprintf("Could not create a composite key for %s: %s", rfid, compositeErr.Error()))
	}

	compositePutErr := APIstub.PutState(compositeKey, []byte(cinfo))
	if compositePutErr != nil {
		return shim.Error(fmt.Sprintf("Could not put operation for %s in the ledger: %s", rfid, compositePutErr.Error()))
	}

	return shim.Success([]byte(fmt.Sprintf("Successfully checkin %s %s at %s", readerid, rfid, checkin)))
}

func (s *SmartContract) get(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	// Check we have a valid number of args
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments, expecting 1")
	}

	rfid := args[0]
	// Get all deltas for the variable
	deltaResultsIterator, deltaErr := APIstub.GetStateByPartialCompositeKey("rfid~readerid~checkin~txID", []string{rfid})
	if deltaErr != nil {
		return shim.Error(fmt.Sprintf("Could not retrieve value for %s: %s", rfid, deltaErr.Error()))
	}
	defer deltaResultsIterator.Close()

	// Check the variable existed
	if !deltaResultsIterator.HasNext() {
		return shim.Error(fmt.Sprintf("No variable by the name %s exists", rfid))
	}

	// Iterate through result set and compute final value
	var i int
	for i = 0; deltaResultsIterator.HasNext(); i++ {
		// Get the next row
		responseRange, nextErr := deltaResultsIterator.Next()
		if nextErr != nil {
			return shim.Error(nextErr.Error())
		}

		// Split the composite key into its component parts
		_, keyParts, splitKeyErr := APIstub.SplitCompositeKey(responseRange.Key)
		if splitKeyErr != nil {
			return shim.Error(splitKeyErr.Error())
		}

		// Retrieve the delta value and operation
		readerid := keyParts[1]
		checkin := keyParts[2]
		txID := keyParts[3]
		chicken, getErr := APIstub.GetState(responseRange.Key)
		if getErr != nil {
			return shim.Error(fmt.Sprintf("Failed to get state: %s", getErr.Error()))
		}
		logger.Infof("鸡 rfid:%s readerid:%s checkin:%s txID:%s chicken:%s\n", rfid, readerid, checkin, txID, chicken)
	}

	return shim.Success([]byte("ok"))
}

// The main function is only relevant in unit test mode. Only included here for completeness.
func main() {

	// Create a new Smart Contract
	err := shim.Start(new(SmartContract))
	if err != nil {
		fmt.Printf("Error creating new Smart Contract: %s", err)
	}
}
 