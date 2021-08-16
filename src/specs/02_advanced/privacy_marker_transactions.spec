# Privacy Marker Transaction

 Tags: privacy-precompile-enabled

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"
* Transaction Hash is returned for "contract17"

## Privacy Marker Transactions created

* "contract17_transactionHash" retrieved from "Node1" is a privacy marker transaction
* Execute "contract17"'s `set()` function with new value "5" in "Node1" and it's private for "Node4", store transaction hash as "contract17.set5"
* "contract17.set5" retrieved from "Node1" is a privacy marker transaction

## Privacy Marker Transaction receipts

* Store "contract17"'s PMT receipt retrieved by "Node1" as "contract17.pmt.receipt"
* Store "contract17"'s private tx receipt retrieved by "Node1" as "contract17.pvttx.receipt"
* "contract17.pmt.receipt" has contractAddress equal to null is "true"
* "contract17.pmt.receipt" has gasUsed equal to 0 is "false"
* "contract17.pmt.receipt" has privacyMarkerTransaction equal to "true"
* "contract17.pvttx.receipt" has contractAddress equal to null is "false"
* "contract17.pvttx.receipt" has gasUsed equal to 0 is "true"
* "contract17.pvttx.receipt" has privacyMarkerTransaction equal to "false"
* "contract17.pmt.receipt" and "contract17.pvttx.receipt" have same transactionHash
